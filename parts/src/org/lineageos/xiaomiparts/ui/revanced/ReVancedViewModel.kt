package org.lineageos.xiaomiparts.ui.revanced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lineageos.xiaomiparts.R
import org.lineageos.xiaomiparts.data.ReVancedManager

data class ReVancedUiState(
    val isEnabled: Boolean = false
)

class ReVancedViewModel(
    private val reVancedManager: ReVancedManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReVancedUiState())
    val uiState: StateFlow<ReVancedUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<Pair<Int, Boolean>?>(null)
    val toastMessage: StateFlow<Pair<Int, Boolean>?> = _toastMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnabled = reVancedManager.isEnabled()) }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val success = reVancedManager.setEnabled(enabled)
            val actualEnabled = reVancedManager.isEnabled()
            _uiState.update { it.copy(isEnabled = actualEnabled) }
            
            _toastMessage.update {
                if (success)
                    Pair(R.string.revanced_restart_required, true)
                else
                    Pair(R.string.revanced_toggle_failed, false)
            }
        }
    }

    fun toastMessageShown() {
        _toastMessage.update { null }
    }
}

class ReVancedViewModelFactory(
    private val reVancedManager: ReVancedManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReVancedViewModel::class.java)) {
            return ReVancedViewModel(reVancedManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}