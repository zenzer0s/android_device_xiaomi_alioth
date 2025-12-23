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
import org.lineageos.xiaomiparts.data.DcDimmingUtils

class DcDimmingTileService : TileService() {
    
    private val dcDimmingUtils: DcDimmingUtils by lazy {
        DcDimmingUtils.getInstance(this)
    }

    private val tileScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun updateUI(enabled: Boolean) {
        qsTile?.let { tile ->
            tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        tileScope.cancel()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateUI(dcDimmingUtils.isDcDimmingEnabled())
    }

    override fun onClick() {
        super.onClick()

        val tile = qsTile ?: return
        val newState = tile.state == Tile.STATE_INACTIVE
        
        tileScope.launch {
            dcDimmingUtils.setDcDimmingEnabled(newState)
        }
        updateUI(newState)
    }

    companion object {
        @JvmStatic
        fun updateTile(context: Context) {
            requestListeningState(
                context,
                ComponentName(context, DcDimmingTileService::class.java)
            )
        }
    }
}