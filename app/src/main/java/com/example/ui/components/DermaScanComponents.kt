package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.BackendStatus
import com.example.domain.ClassProbability
import com.example.domain.ClinicalAttentionLevel
import com.example.domain.Ham10000Catalog
import com.example.domain.InferenceEngine
import com.example.domain.PredictionRecord
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.VerifiedEmerald
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MedicalDisclaimerBanner(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("medical_disclaimer_banner"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MedicalServices,
                contentDescription = "Educational medical disclaimer",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Educational & Research Notice",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(id = R.string.medical_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
fun LesionAttentionBadge(
    level: ClinicalAttentionLevel,
    modifier: Modifier = Modifier,
) {
    val (bgColor, fgColor) = when (level) {
        ClinicalAttentionLevel.LOW_CONCERN -> VerifiedEmerald.copy(alpha = 0.14f) to VerifiedEmerald
        ClinicalAttentionLevel.MONITOR -> CautionAmber.copy(alpha = 0.16f) to CautionAmber
        ClinicalAttentionLevel.HIGH_ATTENTION -> AlertCoral.copy(alpha = 0.14f) to AlertCoral
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
    ) {
        Text(
            text = level.label,
            style = MaterialTheme.typography.labelSmall,
            color = fgColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun BackendStatusChip(
    status: BackendStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (label, dotColor, icon) = when (status) {
        is BackendStatus.Checking -> Triple("Checking API", CautionAmber, Icons.Filled.Refresh)
        is BackendStatus.Ready -> Triple("AI Ready", VerifiedEmerald, Icons.Filled.CheckCircle)
        is BackendStatus.ModelNotTrained -> Triple("Needs Training", CautionAmber, Icons.Filled.Warning)
        is BackendStatus.Offline -> Triple("API Offline", MaterialTheme.colorScheme.error, Icons.Filled.CloudOff)
    }

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .testTag("backend_status_chip"),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = dotColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ServerConfigDialog(
    currentEngine: InferenceEngine,
    currentUrl: String,
    backendStatus: BackendStatus,
    onDismiss: () -> Unit,
    onSaveConfig: (InferenceEngine, String) -> Unit,
    onRefreshHealth: () -> Unit,
) {
    var selectedEngine by remember(currentEngine) { mutableStateOf(currentEngine) }
    var draftUrl by remember(currentUrl) { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Inference Engine & API", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Choose built-in Cloud AI (works immediately with no external server) or connect to your custom FastAPI EfficientNetB0 server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                InferenceEngine.entries.forEach { engine ->
                    val isSelected = selectedEngine == engine
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { selectedEngine = engine }
                            .testTag("engine_option_${engine.name.lowercase()}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedEngine = engine },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = engine.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (engine == InferenceEngine.CLOUD_AI) {
                                        "Zero-setup Firebase AI Logic multimodal vision"
                                    } else {
                                        "Python TensorFlow/Keras HAM10000 endpoint"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (selectedEngine == InferenceEngine.FASTAPI_EFFICIENTNET) {
                    OutlinedTextField(
                        value = draftUrl,
                        onValueChange = { draftUrl = it },
                        label = { Text("FastAPI Base URL") },
                        placeholder = { Text("http://10.0.2.2:8000/") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_url_input"),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalButton(
                            onClick = { draftUrl = "http://10.0.2.2:8000/" },
                            modifier = Modifier.testTag("preset_emulator_url_button"),
                        ) {
                            Text("Emulator Loopback", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = onRefreshHealth,
                            modifier = Modifier.testTag("refresh_health_dialog_button"),
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Ping server", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ping", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                val statusMessage = when (backendStatus) {
                    is BackendStatus.Checking -> "Checking /api/health..."
                    is BackendStatus.Ready -> "Active: ${backendStatus.architecture}"
                    is BackendStatus.ModelNotTrained -> "Connected, model untrained: ${backendStatus.message}"
                    is BackendStatus.Offline -> "FastAPI unreachable: ${backendStatus.reason}"
                }
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveConfig(selectedEngine, draftUrl)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_server_url_button"),
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun ConfidenceRingGauge(
    confidence: Float,
    modifier: Modifier = Modifier,
) {
    val clamped = confidence.coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = clamped * 360f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "confidence_sweep",
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = 10.dp.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.US, "%.1f%%", clamped * 100f),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Confidence",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ProbabilityBarChart(
    probabilities: List<ClassProbability>,
    topCode: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        probabilities.forEachIndexed { index, item ->
            val isTop = item.code.equals(topCode, ignoreCase = true)
            val animatedFraction by animateFloatAsState(
                targetValue = item.probability.coerceIn(0f, 1f),
                animationSpec = tween(
                    durationMillis = 550 + (index * 60),
                    easing = FastOutSlowInEasing,
                ),
                label = "prob_bar_$index",
            )
            val barColor = if (isTop) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isTop) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = item.code,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isTop) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isTop) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.US, "%.2f%%", item.probability * 100f),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isTop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedFraction.coerceAtLeast(0.015f))
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(barColor),
                    )
                }
            }
        }
    }
}

@Composable
fun PredictionResultDashboard(
    record: PredictionRecord,
    onExportReport: () -> Unit,
    modifier: Modifier = Modifier,
    onAnalyzeAnother: (() -> Unit)? = null,
) {
    val categoryInfo = remember(record.predictedCode) {
        Ham10000Catalog.findByCodeOrKey(record.predictedCode)
    }
    val formattedDate = remember(record.timestampMillis) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            .format(Date(record.timestampMillis))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("prediction_results_dashboard"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // AI Prediction Banner Card
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
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
                            imageVector = Icons.Filled.Psychology,
                            contentDescription = "AI classification output",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "AI Classification Output",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = "${record.inferenceTimeMs} ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = File(record.thumbnailPath),
                        contentDescription = "Analyzed skin lesion thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = record.predictedCode,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        Text(
                            text = record.predictedName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        LesionAttentionBadge(level = categoryInfo.attentionLevel)
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ConfidenceRingGauge(confidence = record.confidence)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Model Confidence Score",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Softmax probability assigned by ${record.modelArchitecture}. High confidence does not replace clinical dermatology review.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // 7-Class Probability Distribution Bar Chart
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "HAM10000 Class Probability Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ProbabilityBarChart(
                    probabilities = record.probabilities,
                    topCode = record.predictedCode,
                )
            }
        }

        // Educational Category Context
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Educational overview",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "About ${categoryInfo.displayName} (${categoryInfo.code})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = categoryInfo.educationalSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Typical Dermoscopic Reference Patterns",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = categoryInfo.dermoscopicFeatures,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        MedicalDisclaimerBanner()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onExportReport,
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("export_report_button"),
            ) {
                Icon(Icons.Filled.Download, contentDescription = "Export report", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export Report")
            }
            if (onAnalyzeAnother != null) {
                OutlinedButton(
                    onClick = onAnalyzeAnother,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("analyze_another_button"),
                ) {
                    Text("New Scan")
                }
            }
        }
    }
}
