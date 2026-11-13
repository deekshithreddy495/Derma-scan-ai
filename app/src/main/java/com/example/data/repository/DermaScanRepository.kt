package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.ai.GeminiLesionAnalyzer
import com.example.data.local.PredictionHistoryDao
import com.example.data.local.PredictionHistoryEntity
import com.example.data.remote.ApiErrorDto
import com.example.data.remote.CategoryProbabilityDto
import com.example.data.remote.DermaScanNetworkFactory
import com.example.domain.BackendStatus
import com.example.domain.ClassProbability
import com.example.domain.EvaluationMetricsData
import com.example.domain.Ham10000Catalog
import com.example.domain.MEDICAL_SAFETY_DISCLAIMER
import com.example.domain.PredictionRecord
import com.example.domain.SelectedImageState
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface PredictionOutcome {
    data class Success(val record: PredictionRecord) : PredictionOutcome
    data class ModelNotTrained(val message: String) : PredictionOutcome
    data class Failure(val message: String) : PredictionOutcome
}

class DermaScanRepository(
    private val context: Context,
    private val historyDao: PredictionHistoryDao,
) {
    private val geminiAnalyzer = GeminiLesionAnalyzer()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val probListType = Types.newParameterizedType(
        List::class.java,
        CategoryProbabilityDto::class.java,
    )
    private val probAdapter = moshi.adapter<List<CategoryProbabilityDto>>(probListType)
    private val errorAdapter = moshi.adapter(ApiErrorDto::class.java)

    val predictionHistory: Flow<List<PredictionRecord>> = historyDao.observeAll().map { entities ->
        entities.map { entity -> entity.toDomain() }
    }

    suspend fun validateSelectedImage(uri: Uri): Result<SelectedImageState> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri)?.lowercase(Locale.ROOT) ?: "image/jpeg"
            if (mimeType !in setOf("image/jpeg", "image/jpg", "image/png")) {
                return@withContext Result.failure(
                    IllegalArgumentException("Unsupported format ($mimeType). Please select a JPG, JPEG, or PNG image.")
                )
            }

            var fileName = "skin_lesion.jpg"
            var fileSize = 0L
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
                        if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
                    }
                }

            val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IllegalStateException("Unable to read selected image."))

            if (rawBytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Selected image file is empty."))
            }
            if (rawBytes.size > 15 * 1024 * 1024) {
                return@withContext Result.failure(IllegalArgumentException("Image exceeds 15 MB maximum upload size."))
            }

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return@withContext Result.failure(IllegalArgumentException("File is not a valid readable image."))
            }

            Result.success(
                SelectedImageState(
                    uriString = uri.toString(),
                    fileName = fileName,
                    mimeType = mimeType,
                    width = options.outWidth,
                    height = options.outHeight,
                    fileSizeBytes = if (fileSize > 0L) fileSize else rawBytes.size.toLong(),
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkBackendHealth(baseUrl: String): BackendStatus = withContext(Dispatchers.IO) {
        try {
            val api = DermaScanNetworkFactory.create(baseUrl)
            val response = api.getHealth()
            if (!response.isSuccessful) {
                return@withContext BackendStatus.Offline("Server responded with HTTP ${response.code()}")
            }
            val body = response.body()
                ?: return@withContext BackendStatus.Offline("Empty health response from backend.")
            if (body.modelLoaded) {
                BackendStatus.Ready(body.modelArchitecture)
            } else {
                BackendStatus.ModelNotTrained(body.message)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BackendStatus.Offline(
                e.localizedMessage ?: "Unable to connect to FastAPI server at $baseUrl"
            )
        }
    }

    suspend fun fetchEvaluationMetrics(baseUrl: String): Result<EvaluationMetricsData> = withContext(Dispatchers.IO) {
        try {
            val api = DermaScanNetworkFactory.create(baseUrl)
            val response = api.getMetrics()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IllegalStateException("Backend returned HTTP ${response.code()} while fetching metrics.")
                )
            }
            val dto = response.body()
                ?: return@withContext Result.failure(IllegalStateException("Empty metrics response."))
            Result.success(
                EvaluationMetricsData(
                    available = dto.available,
                    message = dto.message.orEmpty(),
                    modelArchitecture = dto.modelArchitecture ?: "EfficientNetB0 (ImageNet Transfer Learning)",
                    dataset = dto.dataset ?: "HAM10000",
                    evaluatedSamples = dto.evaluatedSamples ?: 0,
                    evaluatedAtUtc = dto.evaluatedAtUtc.orEmpty(),
                    accuracy = dto.accuracy ?: 0f,
                    precisionMacro = dto.precisionMacro ?: 0f,
                    recallMacro = dto.recallMacro ?: 0f,
                    f1Macro = dto.f1Macro ?: 0f,
                    precisionWeighted = dto.precisionWeighted ?: 0f,
                    recallWeighted = dto.recallWeighted ?: 0f,
                    f1Weighted = dto.f1Weighted ?: 0f,
                    classLabels = dto.classLabels.orEmpty(),
                    confusionMatrix = dto.confusionMatrix.orEmpty(),
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeWithCloudAi(
        selectedImage: SelectedImageState,
    ): PredictionOutcome = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(selectedImage.uriString)
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext PredictionOutcome.Failure("Could not open selected image.")

            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                ?: return@withContext PredictionOutcome.Failure("Could not decode image bitmap.")

            val maxDim = 1024
            val scale = minOf(1f, maxDim.toFloat() / maxOf(decoded.width, decoded.height).toFloat())
            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                decoded
            }

            val aiResult = geminiAnalyzer.analyze(scaledBitmap)
            if (scaledBitmap !== decoded) scaledBitmap.recycle()
            decoded.recycle()

            val output = aiResult.getOrElse { err ->
                return@withContext PredictionOutcome.Failure(
                    err.localizedMessage ?: "Cloud AI analysis failed."
                )
            }

            val thumbnailPath = saveThumbnailToInternalStorage(rawBytes)
            val probDtos = output.probabilities.map {
                CategoryProbabilityDto(
                    code = it.code,
                    key = it.key,
                    name = it.name,
                    probability = it.probability,
                )
            }
            val now = System.currentTimeMillis()
            val entity = PredictionHistoryEntity(
                thumbnailPath = thumbnailPath,
                imageFileName = selectedImage.fileName,
                predictedKey = output.predictedKey,
                predictedCode = output.predictedCode,
                predictedName = output.predictedName,
                confidence = output.confidence,
                probabilitiesJson = probAdapter.toJson(probDtos),
                inferenceTimeMs = output.inferenceTimeMs,
                modelArchitecture = output.modelArchitecture,
                timestampMillis = now,
            )
            val insertedId = historyDao.insert(entity)
            PredictionOutcome.Success(entity.copy(id = insertedId).toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PredictionOutcome.Failure(
                e.localizedMessage ?: "Cloud AI multimodal analysis failed."
            )
        }
    }

    suspend fun analyzeSkinImage(
        baseUrl: String,
        selectedImage: SelectedImageState,
    ): PredictionOutcome = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(selectedImage.uriString)
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext PredictionOutcome.Failure("Could not open selected image.")

            val mediaType = selectedImage.mimeType.toMediaTypeOrNull() ?: "image/jpeg".toMediaTypeOrNull()
            val requestBody = rawBytes.toRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("file", selectedImage.fileName, requestBody)

            val api = DermaScanNetworkFactory.create(baseUrl)
            val response = api.predictLesion(part)

            if (response.code() == 503) {
                val detail = parseErrorDetail(response.errorBody()?.string())
                    ?: "EfficientNetB0 model is not trained yet on the backend."
                return@withContext PredictionOutcome.ModelNotTrained(detail)
            }

            if (!response.isSuccessful) {
                val detail = parseErrorDetail(response.errorBody()?.string())
                    ?: "Backend returned HTTP ${response.code()}"
                return@withContext PredictionOutcome.Failure(detail)
            }

            val dto = response.body()
                ?: return@withContext PredictionOutcome.Failure("Backend returned an empty prediction payload.")

            val thumbnailPath = saveThumbnailToInternalStorage(rawBytes)
            val serializedProbs = probAdapter.toJson(dto.probabilities)
            val now = System.currentTimeMillis()

            val entity = PredictionHistoryEntity(
                thumbnailPath = thumbnailPath,
                imageFileName = selectedImage.fileName,
                predictedKey = dto.predictedKey,
                predictedCode = dto.predictedCode,
                predictedName = dto.predictedName,
                confidence = dto.confidence,
                probabilitiesJson = serializedProbs,
                inferenceTimeMs = dto.inferenceTimeMs,
                modelArchitecture = dto.modelArchitecture,
                timestampMillis = now,
            )
            val insertedId = historyDao.insert(entity)
            PredictionOutcome.Success(entity.copy(id = insertedId).toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PredictionOutcome.Failure(
                "Could not reach the FastAPI EfficientNetB0 backend at $baseUrl. " +
                    "Verify the server is running (`uvicorn main:app --host 0.0.0.0 --port 8000`). " +
                    "Details: ${e.localizedMessage ?: "Network error"}"
            )
        }
    }

    suspend fun deleteHistoryRecord(record: PredictionRecord) = withContext(Dispatchers.IO) {
        historyDao.deleteById(record.id)
        runCatching {
            val file = File(record.thumbnailPath)
            if (file.exists()) file.delete()
        }
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAll()
        runCatching {
            val dir = File(context.filesDir, "thumbnails")
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
        }
    }

    suspend fun createShareableReportUri(record: PredictionRecord): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
            val reportFile = File(reportsDir, "dermascan_report_${record.id}_${record.predictedCode}.txt")
            reportFile.writeText(buildReportText(record), Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                reportFile,
            )
            Result.success(uri)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildReportText(record: PredictionRecord): String {
        val dateText = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
            .format(Date(record.timestampMillis))
        val categoryInfo = Ham10000Catalog.findByCodeOrKey(record.predictedCode)
        val probLines = record.probabilities.joinToString("\n") { item ->
            val pct = String.format(Locale.US, "%.2f%%", item.probability * 100f)
            "  - [${item.code}] ${item.name}: $pct"
        }

        return buildString {
            appendLine("====================================================")
            appendLine("DermaScan AI - Educational Lesion Analysis Report")
            appendLine("====================================================")
            appendLine("Report ID        : #${record.id}")
            appendLine("Analyzed At      : $dateText")
            appendLine("Source Image     : ${record.imageFileName}")
            appendLine("Model            : ${record.modelArchitecture}")
            appendLine("Inference Time   : ${record.inferenceTimeMs} ms")
            appendLine()
            appendLine("TOP PREDICTED CATEGORY")
            appendLine("  Category       : ${record.predictedName} (${record.predictedCode})")
            appendLine("  Confidence     : ${String.format(Locale.US, "%.2f%%", record.confidence * 100f)}")
            appendLine("  Attention Tier : ${categoryInfo.attentionLevel.label}")
            appendLine()
            appendLine("ALL 7 HAM10000 CLASS PROBABILITIES")
            appendLine(probLines)
            appendLine()
            appendLine("EDUCATIONAL SUMMARY")
            appendLine("  ${categoryInfo.educationalSummary}")
            appendLine()
            appendLine("DERMOSCOPIC REFERENCE FEATURES")
            appendLine("  ${categoryInfo.dermoscopicFeatures}")
            appendLine()
            appendLine("IMPORTANT MEDICAL DISCLAIMER")
            appendLine("  $MEDICAL_SAFETY_DISCLAIMER")
            appendLine("====================================================")
        }
    }

    private fun saveThumbnailToInternalStorage(rawBytes: ByteArray): String {
        val thumbDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
        val outFile = File(thumbDir, "scan_${UUID.randomUUID()}.jpg")

        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
        if (decoded != null) {
            val maxDim = 512
            val scale = minOf(1f, maxDim.toFloat() / maxOf(decoded.width, decoded.height).toFloat())
            val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
            val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
            } else {
                decoded
            }
            val bos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, bos)
            FileOutputStream(outFile).use { it.write(bos.toByteArray()) }
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
        } else {
            FileOutputStream(outFile).use { it.write(rawBytes) }
        }
        return outFile.absolutePath
    }

    private fun parseErrorDetail(errorJson: String?): String? {
        if (errorJson.isNullOrBlank()) return null
        return runCatching { errorAdapter.fromJson(errorJson)?.detail }.getOrNull()
    }

    private fun PredictionHistoryEntity.toDomain(): PredictionRecord {
        val parsedProbs = runCatching { probAdapter.fromJson(probabilitiesJson) }.getOrNull().orEmpty()
        return PredictionRecord(
            id = id,
            thumbnailPath = thumbnailPath,
            imageFileName = imageFileName,
            predictedKey = predictedKey,
            predictedCode = predictedCode,
            predictedName = predictedName,
            confidence = confidence,
            probabilities = parsedProbs.map {
                ClassProbability(
                    key = it.key,
                    code = it.code,
                    name = it.name,
                    probability = it.probability,
                )
            },
            inferenceTimeMs = inferenceTimeMs,
            modelArchitecture = modelArchitecture,
            timestampMillis = timestampMillis,
        )
    }
}
