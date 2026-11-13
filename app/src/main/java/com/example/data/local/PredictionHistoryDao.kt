package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionHistoryDao {
    @Query("SELECT * FROM prediction_history ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<PredictionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PredictionHistoryEntity): Long

    @Query("DELETE FROM prediction_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM prediction_history")
    suspend fun clearAll()
}
