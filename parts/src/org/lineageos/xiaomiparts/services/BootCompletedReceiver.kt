package org.lineageos.xiaomiparts.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.SystemProperties
import androidx.preference.PreferenceManager
import org.lineageos.xiaomiparts.ui.revanced.ReVancedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.lineageos.xiaomiparts.data.ThermalUtils
import org.lineageos.xiaomiparts.utils.Logging
import org.lineageos.xiaomiparts.data.DcDimmingUtils
import org.lineageos.xiaomiparts.data.ChargeUtils
import org.lineageos.xiaomiparts.data.HBMManager
import org.lineageos.xiaomiparts.data.PREF_AUTO_HBM_KEY
import org.lineageos.xiaomiparts.data.PREF_BYPASS_CHARGE
import org.lineageos.xiaomiparts.data.PROPERTY_REVANCED_AVAILABLE

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Logging.d(TAG, "Received intent: ${intent.action}")
        if (intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        val isReVancedAvailable = SystemProperties.getBoolean(PROPERTY_REVANCED_AVAILABLE, false)
        val pm = context.packageManager
        val componentName = ComponentName(context, ReVancedActivity::class.java)
        
        pm.setComponentEnabledSetting(
            componentName,
            if (isReVancedAvailable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        Logging.i(TAG, "Boot completed, restoring settings...")
        
        val pendingResult = goAsync() 
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        scope.launch {
            try {

                val dcDimmingUtils = DcDimmingUtils.getInstance(context)
                val dcDimmingEnabled = dcDimmingUtils.isDcDimmingEnabled()
                dcDimmingUtils.setDcDimmingEnabled(dcDimmingEnabled)
                
                val chargeUtils = ChargeUtils.getInstance(context)
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val bypassEnabled = prefs.getBoolean(PREF_BYPASS_CHARGE, false)
                chargeUtils.enableBypassCharge(bypassEnabled)

                Logging.i(TAG, "Initializing HBM")
                val hbmManager = HBMManager.getInstance(context)
                hbmManager.initializeOnBoot()

                Logging.i(TAG, "Initializing Thermal")
                val thermalUtils = ThermalUtils.getInstance(context)
                thermalUtils.startService()
                if (!thermalUtils.enabled) {
                    thermalUtils.setDefaultThermalProfile()
                }

                val autoHBMEnabled = prefs.getBoolean(PREF_AUTO_HBM_KEY, false)

                if (autoHBMEnabled) {
                    Logging.i(TAG, "Auto HBM is enabled, starting service")
                    AutoHBMService.enableService(context, true)
                }

            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    companion object {
        private const val TAG = "Boot"
    }
}