package org.lineageos.xiaomiparts.ui.hbm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.lineageos.xiaomiparts.data.DcDimmingUtils
import org.lineageos.xiaomiparts.data.HBMManager
import org.lineageos.xiaomiparts.data.HBMManager.HBMOwner

data class HbmUiState(
    val isHbmEnabled: Boolean = false,
    val isAutoHbmEnabled: Boolean = false,
    val thresholdValue: Float = 20000f,
    val isAutoHbmSliderEnabled: Boolean = false,
    val isDcDimmingEnabled: Boolean = false,
    val hbmDisableTime: Int = 1
)

class HbmViewModel(
    app: Application,
    private val hbmManager: HBMManager,
    private val dcDimmingUtils: DcDimmingUtils
) : AndroidViewModel(app), HBMManager.HBMStateListener {

    private val _hbmEnabled = MutableStateFlow(hbmManager.isHBMEnabled())
    private val _autoHbmEnabled = MutableStateFlow(hbmManager.isAutoHbmEnabled())
    private val _thresholdValue = MutableStateFlow(hbmManager.getThresholdValue().toFloat())
    private val _hbmDisableTime = MutableStateFlow(hbmManager.getDisableTime())
    private val _isDcDimmingEnabled = MutableStateFlow(dcDimmingUtils.isDcDimmingEnabled())

    init {
        hbmManager.addListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        hbmManager.removeListener(this)
    }

    override fun onHBMStateChanged(enabled: Boolean, owner: HBMOwner) {
        _hbmEnabled.value = enabled
    }

    val uiState: StateFlow<HbmUiState> = combine(
        _hbmEnabled,
        _autoHbmEnabled,
        _thresholdValue,
        _hbmDisableTime,
        _isDcDimmingEnabled
    ) { hbm, autoHbm, threshold, disableTime, dcDimming ->
        
        HbmUiState(
            isHbmEnabled = hbm,
            isAutoHbmEnabled = autoHbm,
            thresholdValue = threshold,
            hbmDisableTime = disableTime,
            isAutoHbmSliderEnabled = autoHbm,
            isDcDimmingEnabled = dcDimming
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        HbmUiState(
            thresholdValue = hbmManager.getThresholdValue().toFloat(),
            hbmDisableTime = hbmManager.getDisableTime()
        )
    )

    fun setHbm(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                if (dcDimmingUtils.isDcDimmingEnabled()) {
                    dcDimmingUtils.setDcDimmingEnabled(false)
                    _isDcDimmingEnabled.value = false
                }
                
                hbmManager.enableHBM(HBMOwner.MANUAL)
            } else {
                hbmManager.disableHBM(HBMOwner.MANUAL)
            }
        }
    }

    fun setAutoHbm(enabled: Boolean) {
        hbmManager.setAutoHbmEnabled(enabled)
        _autoHbmEnabled.value = enabled
    }

    fun setThreshold(value: Float) {
        hbmManager.setThresholdValue(value.toInt())
        _thresholdValue.value = value
    }

    fun setDisableTime(value: Float) {
        val intValue = value.toInt()
        hbmManager.setDisableTime(intValue)
        _hbmDisableTime.value = intValue
    }

}

class HbmViewModelFactory(
    private val app: Application,
    private val hbmManager: HBMManager,
    private val dcDimmingUtils: DcDimmingUtils
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HbmViewModel::class.java)) {
            return HbmViewModel(app, hbmManager, dcDimmingUtils) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}