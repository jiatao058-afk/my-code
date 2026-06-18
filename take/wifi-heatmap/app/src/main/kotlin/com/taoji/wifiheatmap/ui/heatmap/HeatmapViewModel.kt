package com.taoji.wifiheatmap.ui.heatmap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taoji.wifiheatmap.data.repo.PositionSummary
import com.taoji.wifiheatmap.data.repo.SignalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HeatmapUiState(
    val sessions: List<String> = emptyList(),
    val selectedSessionId: String? = null,
    val positionSummaries: List<PositionSummary> = emptyList(),
    val isLoading: Boolean = false,
)

class HeatmapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SignalRepository(application)

    private val _uiState = MutableStateFlow(HeatmapUiState())
    val uiState: StateFlow<HeatmapUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            val sessions = repository.getSessions()
            _uiState.value = _uiState.value.copy(sessions = sessions)
        }
    }

    fun selectSession(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedSessionId = sessionId)
            val summaries = repository.getPositionSummaries(sessionId)
            _uiState.value = _uiState.value.copy(positionSummaries = summaries, isLoading = false)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            loadSessions()
        }
    }
}
