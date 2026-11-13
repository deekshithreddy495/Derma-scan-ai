package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DermaScanDatabase
import com.example.data.local.PredictionHistoryEntity
import com.example.domain.Ham10000Catalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: DermaScanDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DermaScanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `app name matches DermaScan AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DermaScan AI", appName)
    }

    @Test
    fun `ham10000 catalog defines all seven diagnostic categories`() {
        val codes = Ham10000Catalog.categories.map { it.code }.toSet()
        assertEquals(setOf("NV", "MEL", "BKL", "BCC", "AKIEC", "VASC", "DF"), codes)
    }

    @Test
    fun `room prediction history dao inserts observes and deletes records`() = runTest {
        val dao = database.predictionHistoryDao()
        val insertedId = dao.insert(
            PredictionHistoryEntity(
                thumbnailPath = "/tmp/sample.jpg",
                imageFileName = "lesion_01.jpg",
                predictedKey = "nv",
                predictedCode = "NV",
                predictedName = "Melanocytic Nevi",
                confidence = 0.874f,
                probabilitiesJson = "[]",
                inferenceTimeMs = 142,
                modelArchitecture = "EfficientNetB0 (ImageNet Transfer Learning)",
                timestampMillis = 1700000000000L,
            )
        )
        assertTrue(insertedId > 0L)

        val records = dao.observeAll().first()
        assertEquals(1, records.size)
        assertEquals("NV", records.first().predictedCode)
        assertEquals(0.874f, records.first().confidence, 0.0001f)

        dao.deleteById(insertedId)
        val afterDelete = dao.observeAll().first()
        assertTrue(afterDelete.isEmpty())
    }
}
