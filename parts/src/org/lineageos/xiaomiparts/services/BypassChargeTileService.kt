package org.lineageos.xiaomiparts.services

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.lineageos.xiaomiparts.data.ChargeUtils
import org.lineageos.xiaomiparts.utils.Logging

class BypassChargeTileService : TileService() {

    private val TAG = "BypassChargeTile"
    private lateinit var chargeUtils: ChargeUtils
    
    private val tileScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        chargeUtils = ChargeUtils.getInstance(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        tileScope.cancel()
    }

    override fun onStartListening() {
        super.onStartListening()
        tileScope.launch {
            val enabled = chargeUtils.isBypassChargeEnabled()
            updateTileUI(enabled)
        }
    }

    override fun onClick() {
        super.onClick()
        
        val currentState = qsTile.state
        val newEnabled = currentState != Tile.STATE_ACTIVE
        
        updateTileUI(newEnabled)
        
        tileScope.launch {
            chargeUtils.enableBypassCharge(newEnabled)
            val actualEnabled = chargeUtils.isBypassChargeEnabled()
            updateTileUI(actualEnabled)
        }
    }

    private fun updateTileUI(enabled: Boolean) {
        qsTile?.let { tile ->
            tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    companion object {
        @JvmStatic
        fun updateTile(context: Context) {
            try {
                requestListeningState(
                    context,
                    ComponentName(context, BypassChargeTileService::class.java)
                )
            } catch (e: Exception) {
                Logging.e("BypassChargeTile", "Failed to request tile update", e)
            }
        }
    }
}
