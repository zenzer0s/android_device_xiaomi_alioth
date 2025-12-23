package org.lineageos.xiaomiparts.services

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.lineageos.xiaomiparts.data.HBMManager
import org.lineageos.xiaomiparts.data.PREF_HBM_KEY
import org.lineageos.xiaomiparts.utils.Logging

class HBMModeTileService : TileService(), HBMManager.HBMStateListener {

    private val TAG = "HBMTile"
    private lateinit var hbmManager: HBMManager
    
    @Volatile
    private var isOperationInProgress = false
    
    private val tileScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Logging.i(TAG, "Tile created")
        hbmManager = HBMManager.getInstance(this)
        hbmManager.addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Logging.i(TAG, "Tile destroyed")
        tileScope.cancel()
        hbmManager.removeListener(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        Logging.i(TAG, "Tile listening")
        
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val enabled = prefs.getBoolean(PREF_HBM_KEY, false)
        updateTileUI(enabled)
    }

    override fun onStopListening() {
        super.onStopListening()
        Logging.i(TAG, "Tile stopped listening")
    }

    override fun onClick() {
        super.onClick()
        
        if (isOperationInProgress) {
            Logging.w(TAG, "Operation already in progress, ignoring rapid click")
            return
        }
        
        val tile = qsTile ?: return
        val newEnabled = tile.state != Tile.STATE_ACTIVE
        
        Logging.i(TAG, "Tile clicked: toggling HBM to $newEnabled")
        
        isOperationInProgress = true
        
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putBoolean(PREF_HBM_KEY, newEnabled).apply()
        
        updateTileUI(newEnabled)
        
        tileScope.launch {
            if (newEnabled) {
                hbmManager.enableHBM(HBMManager.HBMOwner.MANUAL)
            } else {
                hbmManager.disableHBM(HBMManager.HBMOwner.MANUAL)
            }
            isOperationInProgress = false
        }
    }

    override fun onHBMStateChanged(enabled: Boolean, owner: HBMManager.HBMOwner) {
        Logging.i(TAG, "HBM state changed: enabled=$enabled, owner=$owner")
        updateTileUI(enabled)
    }

    private fun updateTileUI(enabled: Boolean) {
        qsTile?.let { tile ->
            tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    companion object {
        @JvmStatic
        fun updateTile(context: Context, enabled: Boolean) {
            try {
                requestListeningState(
                    context,
                    ComponentName(context, HBMModeTileService::class.java)
                )
            } catch (e: Exception) {
                Logging.e("HBMTile", "Failed to request tile update", e)
            }
        }
    }
}
