package org.lineageos.xiaomiparts.services

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.graphics.drawable.Icon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.lineageos.xiaomiparts.R
import org.lineageos.xiaomiparts.data.ThermalUtils

class ThermalTileService : TileService() {
    private lateinit var thermalUtils: ThermalUtils
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        thermalUtils = ThermalUtils.getInstance(this)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val currentMode = thermalUtils.qsMode
        val newMode = (currentMode + 1) % 3
        
        scope.launch(Dispatchers.IO) {
            thermalUtils.qsMode = newMode
            val qsModeValue = thermalUtils.qsMode
            val appliedConfig = ThermalUtils.QsMode.values()[qsModeValue].config
            // We update the tile in Main thread
            launch(Dispatchers.Main) {
                updateTile()
            }
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val currentMode = thermalUtils.qsMode

        tile.state = Tile.STATE_ACTIVE
        tile.label = getString(R.string.thermal_modes_title)
        
        when (currentMode) {
            ThermalUtils.QsMode.BATTERY.value -> {
                tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_thermal_battery)
                tile.subtitle = getString(R.string.thermal_qs_battery)
            }
            ThermalUtils.QsMode.DEFAULT.value -> {
                tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_thermal_default)
                tile.subtitle = getString(R.string.thermal_qs_default)
            }
            ThermalUtils.QsMode.PERFORMANCE.value -> {
                tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_thermal_performance)
                tile.subtitle = getString(R.string.thermal_qs_performance)
            }
        }
        tile.updateTile()
    }
}
