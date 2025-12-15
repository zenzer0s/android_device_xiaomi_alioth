package org.lineageos.xiaomiparts.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import org.lineageos.xiaomiparts.services.BypassChargeTileService
import org.lineageos.xiaomiparts.services.DcDimmingTileService
import org.lineageos.xiaomiparts.services.HBMModeTileService
import org.lineageos.xiaomiparts.ui.charge.ChargeActivity
import org.lineageos.xiaomiparts.ui.dim.DcDimmingActivity
import org.lineageos.xiaomiparts.ui.hbm.HBMActivity

class TilePreferencesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val componentName = intent.getParcelableExtra<ComponentName>(Intent.EXTRA_COMPONENT_NAME)
        val className = componentName?.className

        val targetClass = when (className) {
            HBMModeTileService::class.java.name -> HBMActivity::class.java
            DcDimmingTileService::class.java.name -> DcDimmingActivity::class.java
            BypassChargeTileService::class.java.name -> ChargeActivity::class.java
            else -> null
        }

        if (targetClass != null) {
            val intent = Intent(this, targetClass)
            startActivity(intent)
        }
        
        finish()
    }
}
