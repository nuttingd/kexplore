package dev.nutting.kexplore.ui.screen.connection

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class QrScanState(
    val hasPermission: Boolean = false,
    val decodedPayload: String? = null,
    val error: String? = null,
    val isProcessing: Boolean = false,
)

class QrScanViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(QrScanState())
    val state: StateFlow<QrScanState> = _state.asStateFlow()

    init {
        checkPermission()
    }

    fun checkPermission() {
        val granted = ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        _state.update { it.copy(hasPermission = granted) }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasPermission = granted) }
    }

    fun onQrDecoded(rawValue: String) {
        if (_state.value.isProcessing) return
        _state.update { it.copy(isProcessing = true, decodedPayload = rawValue) }
    }

    fun onError(message: String) {
        _state.update { it.copy(error = message, isProcessing = false) }
    }

    fun reset() {
        _state.update { QrScanState(hasPermission = it.hasPermission) }
    }
}
