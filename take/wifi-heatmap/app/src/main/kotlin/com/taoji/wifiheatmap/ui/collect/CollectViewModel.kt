package com.taoji.wifiheatmap.ui.collect

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taoji.wifiheatmap.data.repo.SignalRepository
import com.taoji.wifiheatmap.wifi.manager.WiFiManagerWrapper
import com.taoji.wifiheatmap.wifi.scanner.ScanResultsReceiver
import com.taoji.wifiheatmap.wifi.scanner.Transformer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CollectUiState(
    val currentPosition: String = "",
    val markedPositions: List<String> = emptyList(),
    val isScanning: Boolean = false,
    val totalReadings: Int = 0,
    val lastScanAdded: Int = 0,
    val elapsedSeconds: Int = 0,
    val scanThrottled: Boolean = false,
    val wifiEnabled: Boolean = true,
    val sessionId: String = UUID.randomUUID().toString(),
    val error: String? = null,
)

class CollectViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SignalRepository(application)
    private val wifiManager = application.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
    val wifiWrapper = WiFiManagerWrapper(wifiManager)
    private val transformer = Transformer()
    private val handler = Handler(Looper.getMainLooper())
    private val scanReceiver = ScanResultsReceiver { onScanResults() }

    private val _uiState = MutableStateFlow(CollectUiState())
    val uiState: StateFlow<CollectUiState> = _uiState.asStateFlow()

    private val scanRunnable = object : Runnable {
        override fun run() {
            if (_uiState.value.currentPosition.isEmpty()) return
            val ok = wifiWrapper.startScan()
            if (!ok) {
                val throttled = wifiWrapper.isScanThrottleEnabled()
                _uiState.update { it.copy(scanThrottled = throttled) }
            }
            handler.postDelayed(this, 3000L)
        }
    }

    private var timerJob: Job? = null

    fun checkWiFi(): Boolean {
        val enabled = wifiWrapper.wiFiEnabled()
        _uiState.update { it.copy(wifiEnabled = enabled) }
        return enabled
    }

    fun markPosition(name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            state.copy(
                currentPosition = name,
                markedPositions = if (state.markedPositions.contains(name))
                    state.markedPositions
                else
                    state.markedPositions + name,
            )
        }
    }

    fun setCurrentPosition(name: String) {
        _uiState.update { it.copy(currentPosition = name) }
    }

    fun startScanning() {
        if (_uiState.value.currentPosition.isEmpty()) return
        _uiState.update { it.copy(isScanning = true, scanThrottled = false, error = null) }
        scanReceiver.register(getApplication())
        handler.post(scanRunnable)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    fun pauseScanning() {
        _uiState.update { it.copy(isScanning = false) }
        handler.removeCallbacks(scanRunnable)
        scanReceiver.unregister(getApplication())
        timerJob?.cancel()
    }

    fun stopScanning() {
        pauseScanning()
        _uiState.update { it.copy(elapsedSeconds = 0) }
    }

    private fun onScanResults() {
        val state = _uiState.value
        if (!state.isScanning || state.currentPosition.isEmpty()) return
        val results = wifiWrapper.scanResults()
        if (results.isEmpty()) return
        val transformed = transformer.transformList(results)
        val added = repository.saveScanResults(transformed, state.currentPosition, state.sessionId)
        if (added > 0) {
            _uiState.update {
                it.copy(
                    totalReadings = it.totalReadings + added,
                    lastScanAdded = added,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacks(scanRunnable)
        scanReceiver.unregister(getApplication())
        timerJob?.cancel()
    }
}
