package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.InferenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dermaScanDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dermascan_preferences"
)

class UserPreferencesRepository(private val context: Context) {

    companion object {
        const val DEFAULT_SERVER_URL = "http://10.0.2.2:8000/"
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
        private val KEY_SERVER_URL = stringPreferencesKey("backend_server_url")
        private val KEY_INFERENCE_ENGINE = stringPreferencesKey("inference_engine")
    }

    val isDarkMode: Flow<Boolean> = context.dermaScanDataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE] ?: false
    }

    val serverUrl: Flow<String> = context.dermaScanDataStore.data.map { prefs ->
        normalizeServerUrl(prefs[KEY_SERVER_URL] ?: DEFAULT_SERVER_URL)
    }

    val inferenceEngine: Flow<InferenceEngine> = context.dermaScanDataStore.data.map { prefs ->
        val raw = prefs[KEY_INFERENCE_ENGINE]
        runCatching { if (raw != null) InferenceEngine.valueOf(raw) else InferenceEngine.CLOUD_AI }
            .getOrDefault(InferenceEngine.CLOUD_AI)
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dermaScanDataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = enabled
        }
    }

    suspend fun setServerUrl(rawUrl: String) {
        context.dermaScanDataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = normalizeServerUrl(rawUrl)
        }
    }

    suspend fun setInferenceEngine(engine: InferenceEngine) {
        context.dermaScanDataStore.edit { prefs ->
            prefs[KEY_INFERENCE_ENGINE] = engine.name
        }
    }

    fun normalizeServerUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim().ifBlank { DEFAULT_SERVER_URL }
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}
