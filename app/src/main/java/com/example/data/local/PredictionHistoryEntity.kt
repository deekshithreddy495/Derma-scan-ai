package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prediction_history")
data class PredictionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val thumbnailPath: String,
    val imageFileName: String,
    val predictedKey: String,
    val predictedCode: String,
    val predictedName: String,
    val confidence: Float,
    val probabilitiesJson: String,
    val inferenceTimeMs: Int,
    val modelArchitecture: String,
    val timestampMillis: Long = System.currentTimeMillis(),
)
