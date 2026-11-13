package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DermaScanDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DermaScanRepository
import com.example.data.repository.PredictionOutcome
import com.example.domain.BackendStatus
import com.example.domain.EvaluationMetricsData
import com.example.domain.InferenceEngine
import com.example.domain.PredictionRecord
import com.example.domain.SelectedImageState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object ValidatingImage : AnalysisUiState
    data object Analyzing : AnalysisUiState
    data class Result(val record: PredictionRecord) : AnalysisUiState
    data class ModelNotTrained(val message: String) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

sealed interface MetricsUiState {
    data object Loading : MetricsUiState
    data class Loaded(val metrics: EvaluationMetricsData) : MetricsUiState
    data class Unavailable(val message: String) : MetricsUiState
}

class DermaScanViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = UserPreferencesRepository(application)
    private val database = DermaScanDatabase.getInstance(application)
    private val repository = DermaScanRepository(application, database.predictionHistoryDao())

    val isDarkMode: StateFlow<Boolean> = preferences.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = false,
    )

    val serverUrl: StateFlow<String> = preferences.serverUrl.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UserPreferencesRepository.DEFAULT_SERVER_URL,
    )

    val inferenceEngine: StateFlow<InferenceEngine> = preferences.inferenceEngine.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = InferenceEngine.CLOUD_AI,
    )

    val predictionHistory: StateFlow<List<PredictionRecord>> = repository.predictionHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
    )

    private val _selectedImage = MutableStateFlow<SelectedImageState?>(null)
    val selectedImage: StateFlow<SelectedImageState?> = _selectedImage.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _backendStatus = MutableStateFlow<BackendStatus>(
        BackendStatus.Ready("Firebase AI Logic • Gemini 3.8 Flash")
    )
    val backendStatus: StateFlow<BackendStatus> = _backendStatus.asStateFlow()

    private val _metricsState = MutableStateFlow<MetricsUiState>(
        MetricsUiState.Unavailable(
            "Evaluation metrics (Accuracy, Precision, Recall, F1, Confusion Matrix) are computed when running `evaluate.py` on your trained EfficientNetB0 model and connecting the FastAPI server."
        )
    )
    val metricsState: StateFlow<MetricsUiState> = _metricsState.asStateFlow()

    private val _historyFilterCode = MutableStateFlow<String?>(null)
    val historyFilterCode: StateFlow<String?> = _historyFilterCode.asStateFlow()

    private val _selectedHistoryRecord = MutableStateFlow<PredictionRecord?>(null)
    val selectedHistoryRecord: StateFlow<PredictionRecord?> = _selectedHistoryRecord.asStateFlow()

    init {
        refreshBackendStatus()
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            preferences.setDarkMode(!isDarkMode.value)
        }
    }

    fun updateInferenceConfig(engine: InferenceEngine, newUrl: String) {
        viewModelScope.launch {
            preferences.setInferenceEngine(engine)
            preferences.setServerUrl(newUrl)
            refreshBackendStatus()
        }
    }

    fun refreshBackendStatus() {
        viewModelScope.launch {
            val engine = preferences.inferenceEngine.first()
            if (engine == InferenceEngine.CLOUD_AI) {
                _backendStatus.value = BackendStatus.Ready("Firebase AI Logic • Gemini 3.8 Flash")
                _metricsState.value = MetricsUiState.Unavailable(
                    "Cloud AI (Gemini 3.8 Flash) is active. Switch to Custom FastAPI (EfficientNetB0) in Engine Settings to inspect HAM10000 test metrics from `evaluate.py`."
                )
            } else {
                _backendStatus.value = BackendStatus.Checking
                val url = preferences.serverUrl.first()
                _backendStatus.value = repository.checkBackendHealth(url)
                loadEvaluationMetrics(url)
            }
        }
    }

    private suspend fun loadEvaluationMetrics(url: String) {
        _metricsState.value = MetricsUiState.Loading
        val result = repository.fetchEvaluationMetrics(url)
        _metricsState.value = result.fold(
            onSuccess = { data ->
                if (data.available) {
                    MetricsUiState.Loaded(data)
                } else {
                    MetricsUiState.Unavailable(
                        data.message.ifBlank {
                            "Model has not been evaluated yet. Run `evaluate.py` after training on HAM10000."
                        }
                    )
                }
            },
            onFailure = { error ->
                MetricsUiState.Unavailable(
                    "Connect to the FastAPI backend to load live HAM10000 evaluation metrics. " +
                        "(${error.localizedMessage ?: "Server offline"})"
                )
            },
        )
    }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.ValidatingImage
            val validation = repository.validateSelectedImage(uri)
            validation.fold(
                onSuccess = { state ->
                    _selectedImage.value = state
                    _analysisState.value = AnalysisUiState.Idle
                },
                onFailure = { err ->
                    _selectedImage.value = null
                    _analysisState.value = AnalysisUiState.Error(
                        err.localizedMessage ?: "Selected file failed validation."
                    )
                },
            )
        }
    }

    fun clearSelectedImage() {
        _selectedImage.value = null
        _analysisState.value = AnalysisUiState.Idle
    }

    fun analyzeCurrentImage() {
        val currentImage = _selectedImage.value ?: return
        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.Analyzing
            val engine = preferences.inferenceEngine.first()

            if (engine == InferenceEngine.CLOUD_AI) {
                when (val outcome = repository.analyzeWithCloudAi(currentImage)) {
                    is PredictionOutcome.Success -> {
                        _analysisState.value = AnalysisUiState.Result(outcome.record)
                        _backendStatus.value = BackendStatus.Ready(outcome.record.modelArchitecture)
                    }
                    is PredictionOutcome.ModelNotTrained -> {
                        _analysisState.value = AnalysisUiState.ModelNotTrained(outcome.message)
                    }
                    is PredictionOutcome.Failure -> {
                        _analysisState.value = AnalysisUiState.Error(outcome.message)
                    }
                }
                return@launch
            }

            val url = preferences.serverUrl.first()
            when (val outcome = repository.analyzeSkinImage(url, currentImage)) {
                is PredictionOutcome.Success -> {
                    _analysisState.value = AnalysisUiState.Result(outcome.record)
                    _backendStatus.value = BackendStatus.Ready(outcome.record.modelArchitecture)
                }
                is PredictionOutcome.ModelNotTrained -> {
                    _analysisState.value = AnalysisUiState.ModelNotTrained(outcome.message)
                    _backendStatus.value = BackendStatus.ModelNotTrained(outcome.message)
                }
                is PredictionOutcome.Failure -> {
                    _analysisState.value = AnalysisUiState.Error(outcome.message)
                    _backendStatus.value = BackendStatus.Offline(outcome.message)
                }
            }
        }
    }

    fun setHistoryFilter(code: String?) {
        _historyFilterCode.value = if (_historyFilterCode.value == code) null else code
    }

    fun openHistoryRecord(record: PredictionRecord) {
        _selectedHistoryRecord.value = record
    }

    fun closeHistoryRecord() {
        _selectedHistoryRecord.value = null
    }

    fun deleteHistoryRecord(record: PredictionRecord) {
        viewModelScope.launch {
            if (_selectedHistoryRecord.value?.id == record.id) {
                _selectedHistoryRecord.value = null
            }
            repository.deleteHistoryRecord(record)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            _selectedHistoryRecord.value = null
            repository.clearAllHistory()
        }
    }

    suspend fun createReportShareUri(record: PredictionRecord): Result<Uri> {
        return repository.createShareableReportUri(record)
    }

    fun buildPlainReportText(record: PredictionRecord): String {
        return repository.buildReportText(record)
    }
}
