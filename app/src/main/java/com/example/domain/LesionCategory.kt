package com.example.domain

import androidx.compose.runtime.Immutable

enum class ClinicalAttentionLevel(val label: String) {
    LOW_CONCERN("Benign Pattern (Educational)"),
    MONITOR("Precancerous / Monitor"),
    HIGH_ATTENTION("High Clinical Attention"),
}

@Immutable
data class LesionCategoryInfo(
    val key: String,
    val code: String,
    val displayName: String,
    val shortName: String,
    val attentionLevel: ClinicalAttentionLevel,
    val educationalSummary: String,
    val dermoscopicFeatures: String,
)

object Ham10000Catalog {
    val categories: List<LesionCategoryInfo> = listOf(
        LesionCategoryInfo(
            key = "nv",
            code = "NV",
            displayName = "Melanocytic Nevi",
            shortName = "Melanocytic Nevi",
            attentionLevel = ClinicalAttentionLevel.LOW_CONCERN,
            educationalSummary = "Common benign proliferations of pigment-producing melanocytes (moles). They represent the majority of samples in the HAM10000 dataset and usually appear uniform in structure and color.",
            dermoscopicFeatures = "Regular reticular pigment network, symmetric globules, or homogeneous tan-brown pigmentation.",
        ),
        LesionCategoryInfo(
            key = "mel",
            code = "MEL",
            displayName = "Melanoma",
            shortName = "Melanoma",
            attentionLevel = ClinicalAttentionLevel.HIGH_ATTENTION,
            educationalSummary = "A malignant tumor originating from melanocytes. Early dermatological detection is vital. In clinical education, melanoma is evaluated using asymmetry, border irregularity, color variation, diameter, and evolution (ABCDE).",
            dermoscopicFeatures = "Atypical pigment network, blue-white veil, irregular streaks, radial streaming, or polymorphous vascular structures.",
        ),
        LesionCategoryInfo(
            key = "bkl",
            code = "BKL",
            displayName = "Benign Keratosis-like Lesions",
            shortName = "Benign Keratosis",
            attentionLevel = ClinicalAttentionLevel.LOW_CONCERN,
            educationalSummary = "Non-cancerous epidermal skin growths including seborrheic keratoses, solar lentigines, and lichen-planus-like keratoses. Their appearance can sometimes resemble pigmented lesions.",
            dermoscopicFeatures = "Milia-like cysts, comedo-like openings, cerebriform ridges, and sharply demarcated moth-eaten borders.",
        ),
        LesionCategoryInfo(
            key = "bcc",
            code = "BCC",
            displayName = "Basal Cell Carcinoma",
            shortName = "Basal Cell Carcinoma",
            attentionLevel = ClinicalAttentionLevel.HIGH_ATTENTION,
            educationalSummary = "A common, slow-growing form of epithelial skin cancer originating in basal keratinocytes. Rarely metastasizes, yet prompt professional assessment prevents local tissue damage.",
            dermoscopicFeatures = "Arborizing telangiectatic vessels, maple leaf-like areas, spoke-wheel structures, and blue-gray ovoid nests.",
        ),
        LesionCategoryInfo(
            key = "akiec",
            code = "AKIEC",
            displayName = "Actinic Keratoses / Intraepithelial Carcinoma",
            shortName = "Actinic Keratoses",
            attentionLevel = ClinicalAttentionLevel.MONITOR,
            educationalSummary = "Sun-induced precancerous lesions and intraepidermal squamous neoplasia (Bowen's disease). Often observed on chronically sun-exposed skin and monitored closely by dermatologists.",
            dermoscopicFeatures = "Strawberry pattern erythema, follicular keratin plugs, rosettes, and fine surface scaling.",
        ),
        LesionCategoryInfo(
            key = "vasc",
            code = "VASC",
            displayName = "Vascular Lesions",
            shortName = "Vascular Lesions",
            attentionLevel = ClinicalAttentionLevel.LOW_CONCERN,
            educationalSummary = "Benign proliferations or malformations of cutaneous blood vessels, including cherry angiomas, angiokeratomas, and pyogenic granulomas.",
            dermoscopicFeatures = "Well-demarcated red, maroon, or violaceous clods and lacunae separated by fibrous septa.",
        ),
        LesionCategoryInfo(
            key = "df",
            code = "DF",
            displayName = "Dermatofibroma",
            shortName = "Dermatofibroma",
            attentionLevel = ClinicalAttentionLevel.LOW_CONCERN,
            educationalSummary = "A benign fibrous skin nodule frequently occurring on the extremities after minor trauma, firm to palpation with a classic dimple sign.",
            dermoscopicFeatures = "Central white scar-like patch surrounded by a delicate, fading peripheral brown pigment network.",
        ),
    )

    private val byCode = categories.associateBy { it.code.uppercase() }
    private val byKey = categories.associateBy { it.key.lowercase() }

    fun findByCodeOrKey(codeOrKey: String): LesionCategoryInfo {
        val normalized = codeOrKey.trim()
        return byCode[normalized.uppercase()]
            ?: byKey[normalized.lowercase()]
            ?: categories.first()
    }
}

@Immutable
data class ClassProbability(
    val key: String,
    val code: String,
    val name: String,
    val probability: Float,
)

@Immutable
data class PredictionRecord(
    val id: Long = 0L,
    val thumbnailPath: String,
    val imageFileName: String,
    val predictedKey: String,
    val predictedCode: String,
    val predictedName: String,
    val confidence: Float,
    val probabilities: List<ClassProbability>,
    val inferenceTimeMs: Int,
    val modelArchitecture: String,
    val timestampMillis: Long,
)

@Immutable
data class SelectedImageState(
    val uriString: String,
    val fileName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
)

enum class InferenceEngine(val displayName: String) {
    CLOUD_AI("Cloud AI (Gemini 3.8 Flash)"),
    FASTAPI_EFFICIENTNET("Custom FastAPI (EfficientNetB0)"),
}

sealed interface BackendStatus {
    data object Checking : BackendStatus
    data class Ready(val architecture: String) : BackendStatus
    data class ModelNotTrained(val message: String) : BackendStatus
    data class Offline(val reason: String) : BackendStatus
}

@Immutable
data class EvaluationMetricsData(
    val available: Boolean,
    val message: String = "",
    val modelArchitecture: String = "EfficientNetB0",
    val dataset: String = "HAM10000",
    val evaluatedSamples: Int = 0,
    val evaluatedAtUtc: String = "",
    val accuracy: Float = 0f,
    val precisionMacro: Float = 0f,
    val recallMacro: Float = 0f,
    val f1Macro: Float = 0f,
    val precisionWeighted: Float = 0f,
    val recallWeighted: Float = 0f,
    val f1Weighted: Float = 0f,
    val classLabels: List<String> = emptyList(),
    val confusionMatrix: List<List<Int>> = emptyList(),
)

const val MEDICAL_SAFETY_DISCLAIMER =
    "This application is developed for educational and research purposes only. " +
        "AI predictions are not medical diagnoses. Please consult a qualified dermatologist " +
        "for professional evaluation."
