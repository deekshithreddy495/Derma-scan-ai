package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.EvaluationMetricsData
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.viewmodel.MetricsUiState
import java.util.Locale

@Composable
fun ModelInfoScreen(
    metricsState: MetricsUiState,
    onRefreshMetrics: () -> Unit,
    onOpenServerDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            // Transfer Learning Architecture Card
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column {
                        Image(
                            painter = painterResource(id = R.drawable.img_transfer_learning),
                            contentDescription = "EfficientNetB0 Transfer Learning architecture illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp),
                        )
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Memory,
                                    contentDescription = "Transfer learning architecture",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "EfficientNetB0 Transfer Learning",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Text(
                                text = "The backend classification pipeline loads EfficientNetB0 pretrained on ImageNet, freezes lower convolutional feature extractors, and attaches a custom dermatoscopic classification head.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            ArchitectureLayerRow("1. Input Layer", "224 × 224 × 3 RGB normalized tensor")
                            ArchitectureLayerRow("2. Backbone", "EfficientNetB0 (ImageNet weights, frozen initially)")
                            ArchitectureLayerRow("3. Pooling", "GlobalAveragePooling2D + BatchNormalization")
                            ArchitectureLayerRow("4. Dense Head", "Dropout(0.3) → Dense(256, ReLU) → Dropout(0.4)")
                            ArchitectureLayerRow("5. Output", "Dense(7, Softmax) over HAM10000 classes")
                            ArchitectureLayerRow("6. Fine-Tuning", "Top 40 layers unfrozen at LR 1e-5 with class weights")
                        }
                    }
                }
            }

            // Live Model Evaluation Metrics (Never Fabricated)
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Analytics,
                                    contentDescription = "Evaluation metrics",
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    text = "HAM10000 Evaluation Metrics",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            OutlinedButton(
                                onClick = onRefreshMetrics,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("refresh_metrics_button"),
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh metrics", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Refresh")
                            }
                        }

                        when (metricsState) {
                            is MetricsUiState.Loading -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = "Fetching real evaluation report from backend...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            is MetricsUiState.Unavailable -> {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = "No Verified Evaluation Report Found",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = metricsState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        OutlinedButton(
                                            onClick = onOpenServerDialog,
                                            modifier = Modifier.minimumInteractiveComponentSize(),
                                        ) {
                                            Text("Configure Backend URL")
                                        }
                                    }
                                }
                            }
                            is MetricsUiState.Loaded -> {
                                MetricsLoadedContent(metrics = metricsState.metrics)
                            }
                        }
                    }
                }
            }

            // About & Dataset Overview
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = "About the project",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "About DermaScan AI & HAM10000",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = "HAM10000 (Human Against Machine with 10000 training images) is a benchmark multi-source dermatoscopic dataset spanning seven diagnostic categories of pigmented skin lesions. To prevent data leakage across multiple images of the same lesion, training, validation, and test splits are grouped strictly by lesion_id.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Academic Contact & Project Info
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Project contact",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                text = "Project & Contact Information",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = "DermaScan AI is an academic healthcare AI/ML project demonstrating mobile client-server deep learning inference with TensorFlow/Keras EfficientNetB0, FastAPI, OpenCV/PIL, and Jetpack Compose.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = "Academic contact email",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Research & Academic Inquiries: dermascan-research@example.edu",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            item {
                MedicalDisclaimerBanner()
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ArchitectureLayerRow(
    stage: String,
    detail: String,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stage,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricsLoadedContent(metrics: EvaluationMetricsData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Evaluated on ${metrics.evaluatedSamples} held-out HAM10000 test images",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricPill("Accuracy", metrics.accuracy, Modifier.weight(1f))
            MetricPill("Macro F1", metrics.f1Macro, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricPill("Precision (Macro)", metrics.precisionMacro, Modifier.weight(1f))
            MetricPill("Recall (Macro)", metrics.recallMacro, Modifier.weight(1f))
        }

        if (metrics.confusionMatrix.isNotEmpty() && metrics.classLabels.isNotEmpty()) {
            Text(
                text = "Confusion Matrix (Rows: True • Cols: Predicted)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.width(56.dp))
                    metrics.classLabels.forEach { colLabel ->
                        Box(
                            modifier = Modifier.width(44.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = colLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                metrics.confusionMatrix.forEachIndexed { rowIdx, rowValues ->
                    val rowLabel = metrics.classLabels.getOrElse(rowIdx) { "?" }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.width(56.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = rowLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        rowValues.forEachIndexed { colIdx, cellValue ->
                            val isDiagonal = rowIdx == colIdx
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isDiagonal) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = cellValue.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDiagonal) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = String.format(Locale.US, "%.2f%%", value * 100f),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
