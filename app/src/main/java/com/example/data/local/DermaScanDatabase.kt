package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PredictionHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class DermaScanDatabase : RoomDatabase() {
    abstract fun predictionHistoryDao(): PredictionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: DermaScanDatabase? = null

        fun getInstance(context: Context): DermaScanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DermaScanDatabase::class.java,
                    "dermascan_ai.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
