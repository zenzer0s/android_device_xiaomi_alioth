package org.lineageos.xiaomiparts.ui.charge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lineageos.xiaomiparts.data.ChargeUtils

data class ChargeUiState(
    val bypassChargeEnabled: Boolean = false,
    val isBypassSupported: Boolean = true,
    val showBypassWarningDialog: Boolean = false
)

class ChargeViewModel(
    private val chargeUtils: ChargeUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChargeUiState())
    val uiState: StateFlow<ChargeUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val isSupported = chargeUtils.isBypassChargeSupported()
            val isEnabled = if (isSupported) chargeUtils.isBypassChargeEnabled() else false
            _uiState.update {
                it.copy(
                    isBypassSupported = isSupported,
                    bypassChargeEnabled = isEnabled
                )
            }
        }
    }

    fun setBypassCharge(enabled: Boolean) {
        viewModelScope.launch {
            val isSupported = chargeUtils.isBypassChargeSupported()
            if (!isSupported) {
                _uiState.update {
                    it.copy(
                        isBypassSupported = false,
                        bypassChargeEnabled = false,
                        showBypassWarningDialog = false
                    )
                }
                return@launch
            }

            if (enabled) {
                _uiState.update { it.copy(showBypassWarningDialog = true) }
            } else {
                chargeUtils.enableBypassCharge(false)
                val actualEnabled = chargeUtils.isBypassChargeEnabled()
                _uiState.update { it.copy(bypassChargeEnabled = actualEnabled) }
            }
        }
    }

    fun confirmBypassCharge() {
        viewModelScope.launch {
            val isSupported = chargeUtils.isBypassChargeSupported()
            if (!isSupported) {
                _uiState.update {
                    it.copy(
                        isBypassSupported = false,
                        bypassChargeEnabled = false,
                        showBypassWarningDialog = false
                    )
                }
                return@launch
            }

            chargeUtils.enableBypassCharge(true)
            val actualEnabled = chargeUtils.isBypassChargeEnabled()
            _uiState.update {
                it.copy(
                    bypassChargeEnabled = actualEnabled,
                    showBypassWarningDialog = false
                )
            }
        }
    }

    fun dismissBypassDialog() {
        _uiState.update { it.copy(showBypassWarningDialog = false) }
    }
}

class ChargeViewModelFactory(
    private val chargeUtils: ChargeUtils
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChargeViewModel::class.java)) {
            return ChargeViewModel(chargeUtils) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}