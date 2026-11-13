package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.BackendStatus
import com.example.domain.InferenceEngine
import com.example.domain.PredictionRecord
import com.example.domain.SelectedImageState
import com.example.ui.components.BackendStatusChip
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.PredictionResultDashboard
import com.example.ui.theme.CautionAmber
import com.example.ui.viewmodel.AnalysisUiState
import java.util.Locale

@Composable
fun ClassifyScreen(
    selectedImage: SelectedImageState?,
    analysisState: AnalysisUiState,
    backendStatus: BackendStatus,
    inferenceEngine: InferenceEngine,
    serverUrl: String,
    onImagePicked: (Uri) -> Unit,
    onClearImage: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onOpenServerDialog: () -> Unit,
    onExportReport: (PredictionRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onImagePicked(uri)
        }
    }

    val launchPicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Top Backend Connection Strip
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Inference Engine",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (inferenceEngine == InferenceEngine.CLOUD_AI) {
                                    "Firebase AI Logic • Gemini 3.8 Flash"
                                } else {
                                    serverUrl
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BackendStatusChip(
                                status = backendStatus,
                                onClick = onOpenServerDialog,
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onOpenServerDialog)
                                    .testTag("configure_server_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Configure backend server URL",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Image Upload Dropzone or Preview
            item {
                if (selectedImage == null) {
                    UploadDropZoneCard(
                        isValidating = analysisState is AnalysisUiState.ValidatingImage,
                        onPickImage = launchPicker,
                    )
                } else {
                    SelectedImagePreviewCard(
                        image = selectedImage,
                        isAnalyzing = analysisState is AnalysisUiState.Analyzing,
                        onRemove = onClearImage,
                        onPickAnother = launchPicker,
                        onAnalyze = onAnalyzeClick,
                    )
                }
            }

            // Animated Analyzing Loader
            if (analysisState is AnalysisUiState.Analyzing) {
                item {
                    AnalyzingProgressCard(inferenceEngine = inferenceEngine)
                }
            }

            // Model Not Trained Notice (No Fake Predictions)
            if (analysisState is AnalysisUiState.ModelNotTrained) {
                item {
                    ModelTrainingRequiredCard(
                        message = analysisState.message,
                        onConfigureServer = onOpenServerDialog,
                    )
                }
            }

            // Network / Validation Error Notice
            if (analysisState is AnalysisUiState.Error) {
                item {
                    AnalysisErrorCard(
                        message = analysisState.message,
                        canRetry = selectedImage != null,
                        onRetry = onAnalyzeClick,
                        onConfigureServer = onOpenServerDialog,
                    )
                }
            }

            // Real Results Dashboard
            if (analysisState is AnalysisUiState.Result) {
                item {
                    PredictionResultDashboard(
                        record = analysisState.record,
                        onExportReport = { onExportReport(analysisState.record) },
                        onAnalyzeAnother = onClearImage,
                    )
                }
            } else {
                item {
                    MedicalDisclaimerBanner(compact = true)
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun UploadDropZoneCard(
    isValidating: Boolean,
    onPickImage: () -> Unit,
) {
    val dashColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(MaterialTheme.shapes.large)
                .clickable(enabled = !isValidating, onClick = onPickImage)
                .testTag("image_upload_dropzone"),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f),
                    ),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(16.dp)
                                .size(32.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Select skin lesion photo",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(36.dp),
                        )
                    }
                }
                Text(
                    text = if (isValidating) "Validating Lesion Image..." else "Upload Skin Lesion Image",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Select a clear JPG, JPEG, or PNG dermatoscopic or close-up skin lesion image (up to 15 MB).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onPickImage,
                    enabled = !isValidating,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("select_image_button"),
                ) {
                    Text("Choose Photo")
                }
            }
        }
    }
}

@Composable
private fun SelectedImagePreviewCard(
    image: SelectedImageState,
    isAnalyzing: Boolean,
    onRemove: () -> Unit,
    onPickAnother: () -> Unit,
    onAnalyze: () -> Unit,
) {
    val sizeKb = String.format(Locale.US, "%.1f KB", image.fileSizeBytes / 1024f)

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model = Uri.parse(image.uriString),
                    contentDescription = "Selected skin lesion preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(enabled = !isAnalyzing, onClick = onRemove)
                        .testTag("remove_image_icon_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove selected image",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = image.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = "${image.width} × ${image.height} px • $sizeKb • Validated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onPickAnother,
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("upload_another_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Replace image",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Replace")
                }
                Button(
                    onClick = onAnalyze,
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .weight(1.35f)
                        .minimumInteractiveComponentSize()
                        .testTag("analyze_image_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DocumentScanner,
                        contentDescription = "Analyze skin image",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isAnalyzing) "Analyzing..." else "Analyze Skin Image")
                }
            }
        }
    }
}

@Composable
private fun AnalyzingProgressCard(inferenceEngine: InferenceEngine) {
    val pulseTransition = rememberInfiniteTransition(label = "scan_pulse")
    val alpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan_alpha",
    )

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f * alpha)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (inferenceEngine == InferenceEngine.CLOUD_AI) {
                    "Analyzing Lesion with Cloud AI..."
                } else {
                    "Running EfficientNetB0 Inference..."
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (inferenceEngine == InferenceEngine.CLOUD_AI) {
                    "Evaluating dermatoscopic features via Firebase AI Logic (Gemini 3.8 Flash) across all 7 HAM10000 categories."
                } else {
                    "Preprocessing image (224×224 RGB), extracting convolutional embeddings, and computing 7-class Softmax probabilities."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp)),
            )
        }
    }
}

@Composable
private fun ModelTrainingRequiredCard(
    message: String,
    onConfigureServer: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = CautionAmber.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, CautionAmber.copy(alpha = 0.45f)),
        modifier = Modifier.testTag("model_not_trained_card"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Model training required",
                    tint = CautionAmber,
                )
                Text(
                    text = "Trained Model Required Before Predictions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = "Training command",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "python train.py --data-dir /path/to/HAM10000\nuvicorn main:app --host 0.0.0.0 --port 8000",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            OutlinedButton(
                onClick = onConfigureServer,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text("Check / Change Backend URL")
            }
        }
    }
}

@Composable
private fun AnalysisErrorCard(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onConfigureServer: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
        modifier = Modifier.testTag("analysis_error_card"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = "Analysis error",
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "Prediction Unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canRetry) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("retry_analysis_button"),
                    ) {
                        Text("Retry Analysis")
                    }
                }
                OutlinedButton(
                    onClick = onConfigureServer,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Text("Engine Settings")
                }
            }
        }
    }
}
