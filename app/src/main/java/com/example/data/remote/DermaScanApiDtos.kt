package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthResponseDto(
    @Json(name = "status") val status: String = "unknown",
    @Json(name = "model_loaded") val modelLoaded: Boolean = false,
    @Json(name = "model_architecture") val modelArchitecture: String = "EfficientNetB0",
    @Json(name = "message") val message: String = "",
)

@JsonClass(generateAdapter = true)
data class CategoryProbabilityDto(
    @Json(name = "code") val code: String,
    @Json(name = "key") val key: String,
    @Json(name = "name") val name: String,
    @Json(name = "probability") val probability: Float,
)

@JsonClass(generateAdapter = true)
data class PredictionResponseDto(
    @Json(name = "predicted_key") val predictedKey: String,
    @Json(name = "predicted_code") val predictedCode: String,
    @Json(name = "predicted_name") val predictedName: String,
    @Json(name = "confidence") val confidence: Float,
    @Json(name = "probabilities") val probabilities: List<CategoryProbabilityDto>,
    @Json(name = "inference_time_ms") val inferenceTimeMs: Int,
    @Json(name = "model_architecture") val modelArchitecture: String,
    @Json(name = "disclaimer") val disclaimer: String,
)

@JsonClass(generateAdapter = true)
data class EvaluationMetricsDto(
    @Json(name = "available") val available: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "model_architecture") val modelArchitecture: String? = null,
    @Json(name = "dataset") val dataset: String? = null,
    @Json(name = "evaluated_samples") val evaluatedSamples: Int? = null,
    @Json(name = "evaluated_at_utc") val evaluatedAtUtc: String? = null,
    @Json(name = "accuracy") val accuracy: Float? = null,
    @Json(name = "precision_macro") val precisionMacro: Float? = null,
    @Json(name = "recall_macro") val recallMacro: Float? = null,
    @Json(name = "f1_macro") val f1Macro: Float? = null,
    @Json(name = "precision_weighted") val precisionWeighted: Float? = null,
    @Json(name = "recall_weighted") val recallWeighted: Float? = null,
    @Json(name = "f1_weighted") val f1Weighted: Float? = null,
    @Json(name = "class_labels") val classLabels: List<String>? = null,
    @Json(name = "confusion_matrix") val confusionMatrix: List<List<Int>>? = null,
)

@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    @Json(name = "detail") val detail: String? = null,
)
