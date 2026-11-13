package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.Ham10000Catalog
import com.example.domain.PredictionRecord
import com.example.ui.components.LesionAttentionBadge
import com.example.ui.components.PredictionResultDashboard
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    records: List<PredictionRecord>,
    activeFilterCode: String?,
    selectedRecord: PredictionRecord?,
    onSelectFilter: (String?) -> Unit,
    onOpenRecord: (PredictionRecord) -> Unit,
    onCloseRecord: () -> Unit,
    onDeleteRecord: (PredictionRecord) -> Unit,
    onClearAllHistory: () -> Unit,
    onNavigateToClassify: () -> Unit,
    onExportReport: (PredictionRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (selectedRecord != null) {
        BackHandler { onCloseRecord() }
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onCloseRecord)
                                .padding(end = 12.dp)
                                .testTag("history_detail_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to prediction history",
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Back to History",
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        IconButton(
                            onClick = { onDeleteRecord(selectedRecord) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("history_detail_delete_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete this prediction record",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                item {
                    PredictionResultDashboard(
                        record = selectedRecord,
                        onExportReport = { onExportReport(selectedRecord) },
                    )
                }
            }
        }
        return
    }

    val filteredRecords = remember(records, activeFilterCode) {
        if (activeFilterCode == null) records
        else records.filter { it.predictedCode.equals(activeFilterCode, ignoreCase = true) }
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Prediction History",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${records.size} saved ${if (records.size == 1) "analysis" else "analyses"} stored locally in Room DB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (records.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_all_history_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Clear all history",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Category Filter Chips
            if (records.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = activeFilterCode == null,
                            onClick = { onSelectFilter(null) },
                            label = { Text("All (${records.size})") },
                            modifier = Modifier.testTag("history_filter_all"),
                        )
                        Ham10000Catalog.categories.forEach { cat ->
                            FilterChip(
                                selected = activeFilterCode.equals(cat.code, ignoreCase = true),
                                onClick = { onSelectFilter(cat.code) },
                                label = { Text(cat.code) },
                                modifier = Modifier.testTag("history_filter_${cat.code.lowercase()}"),
                            )
                        }
                    }
                }
            }

            if (filteredRecords.isEmpty()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = "Empty prediction history",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .size(34.dp),
                                )
                            }
                            Text(
                                text = if (records.isEmpty()) "No Prediction History Yet" else "No Matches for $activeFilterCode",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (records.isEmpty()) {
                                    "Analyzed skin lesion images, predicted HAM10000 categories, and confidence scores are saved locally here for quick reference."
                                } else {
                                    "No saved analyses match the selected category filter."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = {
                                    if (records.isEmpty()) onNavigateToClassify() else onSelectFilter(null)
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("empty_history_action_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DocumentScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (records.isEmpty()) "Analyze Skin Image" else "Show All Records")
                            }
                        }
                    }
                }
            } else {
                items(filteredRecords, key = { it.id }) { record ->
                    HistoryRecordCard(
                        record = record,
                        onOpen = { onOpenRecord(record) },
                        onDelete = { onDeleteRecord(record) },
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Prediction History?") },
            text = { Text("This removes all locally saved analysis records and thumbnails from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllHistory()
                        showClearDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_history_button"),
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun HistoryRecordCard(
    record: PredictionRecord,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val categoryInfo = remember(record.predictedCode) {
        Ham10000Catalog.findByCodeOrKey(record.predictedCode)
    }
    val dateLabel = remember(record.timestampMillis) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            .format(Date(record.timestampMillis))
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .testTag("history_record_card_${record.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = File(record.thumbnailPath),
                contentDescription = "Thumbnail for ${record.predictedName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = record.predictedCode,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f%%", record.confidence * 100f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                Text(
                    text = record.predictedName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                LesionAttentionBadge(level = categoryInfo.attentionLevel)

                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_history_item_${record.id}"),
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete prediction record",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
