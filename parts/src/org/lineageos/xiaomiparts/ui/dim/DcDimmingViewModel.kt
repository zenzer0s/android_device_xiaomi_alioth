package org.lineageos.xiaomiparts.ui.dim

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lineageos.xiaomiparts.data.DcDimmingUtils

data class DcDimmingUiState(
    val isDcDimmingEnabled: Boolean = false,
    val isDcDimmingSupported: Boolean = true
)

class DcDimmingViewModel(
    app: Application,
    private val dcDimmingUtils: DcDimmingUtils
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(DcDimmingUiState())
    val uiState: StateFlow<DcDimmingUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val isSupported = dcDimmingUtils.isDcDimmingSupported()
            val isEnabled = if (isSupported) dcDimmingUtils.isDcDimmingEnabled() else false
            _uiState.update {
                it.copy(
                    isDcDimmingSupported = isSupported,
                    isDcDimmingEnabled = isEnabled
                )
            }
        }
    }

    fun setDcDimmingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val isSupported = dcDimmingUtils.isDcDimmingSupported()
            if (!isSupported) {
                _uiState.update { it.copy(isDcDimmingSupported = false, isDcDimmingEnabled = false) }
                return@launch
            }

            dcDimmingUtils.setDcDimmingEnabled(enabled)
            val actualEnabled = dcDimmingUtils.isDcDimmingEnabled()
            _uiState.update { it.copy(isDcDimmingEnabled = actualEnabled) }
        }
    }
}

class DcDimmingViewModelFactory(
    private val app: Application,
    private val dcDimmingUtils: DcDimmingUtils
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DcDimmingViewModel::class.java)) {
            return DcDimmingViewModel(app, dcDimmingUtils) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}