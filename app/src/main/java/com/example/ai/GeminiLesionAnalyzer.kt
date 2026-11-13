package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.domain.ClassProbability
import com.example.domain.Ham10000Catalog
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiLesionAnalyzer"
private const val PRIMARY_MODEL_NAME = "gemini-3.8-flash"
private const val FALLBACK_MODEL_NAME = "gemini-3.5-flash-lite"

data class GeminiAnalysisOutput(
    val predictedKey: String,
    val predictedCode: String,
    val predictedName: String,
    val confidence: Float,
    val probabilities: List<ClassProbability>,
    val inferenceTimeMs: Int,
    val modelArchitecture: String,
)

class GeminiLesionAnalyzer {

    private val hamCodes = listOf("NV", "MEL", "BKL", "BCC", "AKIEC", "VASC", "DF")

    private val responseSchema = Schema.obj(
        properties = mapOf(
            "predictedCode" to Schema.enumeration(
                values = hamCodes,
                description = "Most likely HAM10000 educational category code among NV, MEL, BKL, BCC, AKIEC, VASC, DF",
            ),
            "confidence" to Schema.double(
                description = "Probability score between 0.0 and 1.0 for the top predicted category",
            ),
            "probabilities" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "code" to Schema.enumeration(
                            values = hamCodes,
                            description = "HAM10000 category code",
                        ),
                        "probability" to Schema.double(
                            description = "Normalized probability score between 0.0 and 1.0",
                        ),
                    ),
                ),
                description = "Probability distribution across all 7 HAM10000 categories summing to 1.0",
            ),
            "visualObservations" to Schema.string(
                description = "Concise objective educational notes on visible asymmetry, border, color variation, and surface pattern",
            ),
        ),
    )

    private val sharedGenerationConfig by lazy {
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = this@GeminiLesionAnalyzer.responseSchema
            temperature = 0.2f
        }
    }

    private val primaryModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL_NAME,
            generationConfig = sharedGenerationConfig,
        )
    }

    private val fallbackModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL_NAME,
            generationConfig = sharedGenerationConfig,
        )
    }

    suspend fun analyze(bitmap: Bitmap): Result<GeminiAnalysisOutput> {
        val startMs = System.currentTimeMillis()
        val promptContent = content {
            image(bitmap)
            text(
                """
                You are an educational dermatology image-analysis assistant for the DermaScan AI academic project.
                Analyze the provided skin lesion image across the 7 HAM10000 categories:
                - NV: Melanocytic Nevi
                - MEL: Melanoma
                - BKL: Benign Keratosis-like Lesions
                - BCC: Basal Cell Carcinoma
                - AKIEC: Actinic Keratoses / Intraepithelial Carcinoma
                - VASC: Vascular Lesions
                - DF: Dermatofibroma

                Provide an educational probability estimate across ALL 7 categories summing to 1.0,
                select the highest-probability predictedCode and matching confidence, and summarize objective visual characteristics.
                Remember: this is strictly for academic and educational demonstration, not a clinical diagnosis.
                """.trimIndent()
            )
        }

        return try {
            val (response, usedModelLabel) = withTimeout(60_000L) {
                executeWithTransientFallback(promptContent)
            }

            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Gemini AI returned an empty response."))

            val json = JSONObject(jsonText)
            val rawTopCode = json.optString("predictedCode", "NV").uppercase()
            val rawProbsArray = json.optJSONArray("probabilities")

            val parsedProbMap = mutableMapOf<String, Float>()
            if (rawProbsArray != null) {
                for (i in 0 until rawProbsArray.length()) {
                    val item = rawProbsArray.optJSONObject(i) ?: continue
                    val code = item.optString("code", "").uppercase()
                    val prob = item.optDouble("probability", 0.0).toFloat().coerceIn(0f, 1f)
                    if (code in hamCodes) {
                        parsedProbMap[code] = prob
                    }
                }
            }

            val rawConfidence = json.optDouble("confidence", 0.0).toFloat().coerceIn(0f, 1f)
            if (parsedProbMap.isEmpty()) {
                parsedProbMap[rawTopCode] = if (rawConfidence > 0f) rawConfidence else 0.7f
            }
            val completeRaw = hamCodes.associateWith { code ->
                parsedProbMap[code] ?: 0.01f
            }
            val sum = completeRaw.values.sum().coerceAtLeast(1e-6f)

            val sortedProbs = Ham10000Catalog.categories
                .map { cat ->
                    ClassProbability(
                        key = cat.key,
                        code = cat.code,
                        name = cat.displayName,
                        probability = (completeRaw[cat.code] ?: 0f) / sum,
                    )
                }
                .sortedByDescending { it.probability }

            val topItem = sortedProbs.first()
            val elapsedMs = (System.currentTimeMillis() - startMs).toInt().coerceAtLeast(1)

            Result.success(
                GeminiAnalysisOutput(
                    predictedKey = topItem.key,
                    predictedCode = topItem.code,
                    predictedName = topItem.name,
                    confidence = topItem.probability,
                    probabilities = sortedProbs,
                    inferenceTimeMs = elapsedMs,
                    modelArchitecture = "Firebase AI Logic • $usedModelLabel",
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI analysis timed out after 60s", e)
            Result.failure(IllegalStateException("AI analysis timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI analysis failed", e)
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Firebase AI Logic request failed. Please try again."
                )
            )
        }
    }

    private suspend fun executeWithTransientFallback(
        promptContent: com.google.firebase.ai.type.Content,
    ): Pair<GenerateContentResponse, String> {
        val attempts = listOf(
            Triple(primaryModel, "Gemini 3.8 Flash", 0L),
            Triple(fallbackModel, "Gemini 3.5 Flash-Lite", 900L),
            Triple(fallbackModel, "Gemini 3.5 Flash-Lite", 2000L),
        )

        var lastException: Exception? = null
        for ((index, attempt) in attempts.withIndex()) {
            val (model, label, delayMs) = attempt
            if (delayMs > 0L) {
                delay(delayMs)
            }
            try {
                val response = model.generateContent(promptContent)
                return response to label
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                val isTransient = isTransientDemandError(e)
                Log.w(
                    TAG,
                    "Attempt ${index + 1} ($label) failed (transient=$isTransient): ${e.message}",
                )
                if (!isTransient && index == 0) {
                    // Still give fallback model a chance only if error looks model/capacity-related; otherwise fail fast
                    throw e
                }
            }
        }

        throw lastException ?: IllegalStateException("AI service is temporarily busy. Please try again shortly.")
    }

    private fun isTransientDemandError(error: Throwable): Boolean {
        val msg = generateSequence(error) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()
        return msg.contains("high demand") ||
            msg.contains("spikes in demand") ||
            msg.contains("temporarily") ||
            msg.contains("unavailable") ||
            msg.contains("503") ||
            msg.contains("429") ||
            msg.contains("overloaded") ||
            msg.contains("resource exhausted") ||
            msg.contains("try again later")
    }
}
