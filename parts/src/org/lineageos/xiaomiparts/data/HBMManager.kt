package org.lineageos.xiaomiparts.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.lineageos.xiaomiparts.utils.getFileValueAsBoolean
import org.lineageos.xiaomiparts.utils.writeLine
import org.lineageos.xiaomiparts.utils.Logging
import org.lineageos.xiaomiparts.services.AutoHBMService
import java.util.concurrent.CopyOnWriteArraySet

class HBMManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<HBMStateListener>()

    private val mutex = Mutex()

    @Volatile
    private var currentOwner = HBMOwner.NONE

    @Volatile
    private var savedBrightnessMode: Int? = null
    @Volatile
    private var savedBrightnessValue: Int? = null

    fun isHBMEnabled(): Boolean {
        return currentOwner != HBMOwner.NONE
    }

    suspend fun isHBMEnabledInHardware(): Boolean = getFileValueAsBoolean(HBM_SYSFS_PATH, false)

    private suspend fun enableHBMInternal(owner: HBMOwner): Boolean = withContext(Dispatchers.IO) {
        val success = mutex.withLock {
            Logging.i(TAG, "enableHBM: owner=$owner, currentOwner=$currentOwner")
            
            val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
            if (prefs.getBoolean(PREF_DC_DIMMING_KEY, false)) {
                Logging.w(TAG, "Cannot enable HBM: DC Dimming is active")
                return@withLock false
            }
            
            if (currentOwner == HBMOwner.MANUAL && owner == HBMOwner.AUTO_SERVICE) {
                Logging.i(TAG, "HBM already enabled manually, ignoring auto-enable request")
                return@withLock false
            }
            
            val hardwareEnabled = getFileValueAsBoolean(HBM_SYSFS_PATH, false)
            if (hardwareEnabled && currentOwner == owner) {
                Logging.i(TAG, "HBM already enabled by same owner, skipping")
                return@withLock true
            }

            if (currentOwner == HBMOwner.NONE) {
                val currentMode = Settings.System.getInt(
                    appContext.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                savedBrightnessMode = currentMode

                val currentBrightness = Settings.System.getInt(
                    appContext.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    128
                )
                savedBrightnessValue = currentBrightness

                Logging.i(TAG, "Saving brightness state: Mode=$currentMode, Value=$currentBrightness")
                prefs.edit()
                    .putInt(PREF_HBM_SAVED_BRIGHTNESS_MODE, currentMode)
                    .putInt(PREF_HBM_SAVED_BRIGHTNESS_VALUE, currentBrightness)
                    .apply()
            }

            if (!writeLine(HBM_SYSFS_PATH, "0x10000")) {
                Logging.e(TAG, "Failed to write to HBM sysfs")
                return@withLock false
            }
            
            Settings.System.putInt(
                appContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )

            if (!writeLine(BACKLIGHT_SYSFS_PATH, "2047")) {
                Logging.w(TAG, "Failed to write max brightness to backlight sysfs, proceeding anyway")
            }

            Settings.System.putInt(
                appContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                255
            )

            currentOwner = owner
            val editor = prefs.edit()
                .putString(PREF_HBM_OWNER_KEY, owner.name)
            if (owner == HBMOwner.MANUAL) {
                editor.putBoolean(PREF_HBM_KEY, true)
            }
            editor.apply()
            
            Logging.i(TAG, "HBM enabled successfully by $owner")
            return@withLock true
        }

        if (success) {
            notifyListeners(true, owner)
        }
        return@withContext success
    }

    private suspend fun disableHBMInternal(owner: HBMOwner): Boolean = withContext(Dispatchers.IO) {
        val success = mutex.withLock {
            Logging.i(TAG, "disableHBM: owner=$owner, currentOwner=$currentOwner")

            val previousOwner = currentOwner
            
            val hardwareEnabled = getFileValueAsBoolean(HBM_SYSFS_PATH, false)

            if (currentOwner == HBMOwner.NONE && !hardwareEnabled) {
                Logging.i(TAG, "HBM already disabled (Logic+Hardware), skipping")
                return@withLock true
            }
            
            if (owner == HBMOwner.AUTO_SERVICE && currentOwner == HBMOwner.MANUAL) {
                Logging.i(TAG, "Cannot disable manually-enabled HBM from auto service")
                return@withLock false
            }
            
            if (!writeLine(HBM_SYSFS_PATH, "0xF0000")) {
                Logging.e(TAG, "Failed to write to HBM sysfs")
                return@withLock false
            }

            val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
            val resolver = appContext.contentResolver

            val currentSystemMode = Settings.System.getInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            )
            
            if (currentSystemMode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                val modeToRestore = savedBrightnessMode 
                    ?: if (prefs.contains(PREF_HBM_SAVED_BRIGHTNESS_MODE)) 
                        prefs.getInt(PREF_HBM_SAVED_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) 
                       else Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

                Logging.i(TAG, "Restoring brightness mode: $modeToRestore")
                Settings.System.putInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    modeToRestore
                )
            }

            val restoredMode = Settings.System.getInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            )

            var valToRestore = savedBrightnessValue
            if (valToRestore == null && prefs.contains(PREF_HBM_SAVED_BRIGHTNESS_VALUE)) {
                valToRestore = prefs.getInt(PREF_HBM_SAVED_BRIGHTNESS_VALUE, 128)
                Logging.i(TAG, "Restored brightness value from disk backup: $valToRestore")
            }

            if (restoredMode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL && valToRestore != null) {
                Logging.i(TAG, "Restoring brightness value: $valToRestore")
                Settings.System.putInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    valToRestore
                )
            }

            currentOwner = HBMOwner.NONE
            savedBrightnessMode = null
            savedBrightnessValue = null

            val editor = prefs.edit()
                .remove(PREF_HBM_SAVED_BRIGHTNESS_MODE)
                .remove(PREF_HBM_SAVED_BRIGHTNESS_VALUE)
                .remove(PREF_HBM_OWNER_KEY)
            if (previousOwner == HBMOwner.MANUAL) {
                editor.putBoolean(PREF_HBM_KEY, false)
            }
            editor.apply()
            
            Logging.i(TAG, "HBM disabled successfully by $owner")
            return@withLock true
        }

        if (success) {
            notifyListeners(false, HBMOwner.NONE)
        }
        return@withContext success
    }

    suspend fun enableHBM(owner: HBMOwner): Boolean {
        require(owner != HBMOwner.NONE) { "Cannot enable HBM with NONE owner" }
        return enableHBMInternal(owner)
    }

    suspend fun disableHBM(owner: HBMOwner): Boolean {
        return disableHBMInternal(owner)
    }

    suspend fun initializeOnBoot() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        val shouldBeEnabled = prefs.getBoolean(PREF_HBM_KEY, false)

        val hardwareEnabled = getFileValueAsBoolean(HBM_SYSFS_PATH, false)

        Logging.i(TAG, "Init: Pref=$shouldBeEnabled, Hardware=$hardwareEnabled")

        if (shouldBeEnabled) {
            if (!hardwareEnabled && !prefs.getBoolean(PREF_DC_DIMMING_KEY, false)) {
                Logging.i(TAG, "Restoring manually-enabled HBM on boot")
                enableHBMInternal(HBMOwner.MANUAL)
            } else {
                if (hardwareEnabled) {
                    val savedOwnerName = prefs.getString(PREF_HBM_OWNER_KEY, null)
                    currentOwner = if (savedOwnerName != null) {
                        try {
                            HBMOwner.valueOf(savedOwnerName)
                        } catch (e: IllegalArgumentException) {
                            HBMOwner.MANUAL 
                        }
                    } else {
                         HBMOwner.MANUAL 
                    }
                    Logging.i(TAG, "Restored HBM Owner from disk: $currentOwner")
                }
            }
        } else {
            if (hardwareEnabled) {
                Logging.i(TAG, "Disabling HBM on boot (Hardware was ON, Pref is OFF)")
                disableHBMInternal(HBMOwner.MANUAL)
            } else {
                 prefs.edit()
                      .remove(PREF_HBM_SAVED_BRIGHTNESS_MODE)
                      .remove(PREF_HBM_SAVED_BRIGHTNESS_VALUE)
                 .remove(PREF_HBM_OWNER_KEY)
                 .apply()
            }
        }
    }

    fun addListener(listener: HBMStateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: HBMStateListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(enabled: Boolean, owner: HBMOwner) {
        mainHandler.post {
            listeners.forEach { listener ->
                try {
                    listener.onHBMStateChanged(enabled, owner)
                } catch (e: Exception) {
                    Logging.e(TAG, "Error notifying listener", e)
                }
            }
        }
    }

    fun isAutoHbmEnabled(): Boolean =
        PreferenceManager.getDefaultSharedPreferences(appContext)
            .getBoolean(PREF_AUTO_HBM_KEY, false)

    fun setAutoHbmEnabled(enabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(appContext).edit()
            .putBoolean(PREF_AUTO_HBM_KEY, enabled).apply()
        AutoHBMService.enableService(appContext, enabled)
    }
    
    fun getThresholdValue(): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        return try {
            prefs.getInt(PREF_AUTO_HBM_THRESHOLD_KEY, 20000)
        } catch (e: ClassCastException) {
             try {
                prefs.getString(PREF_AUTO_HBM_THRESHOLD_KEY, "20000")?.toInt() ?: 20000
            } catch (ne: NumberFormatException) { 20000 }
        }
    }
    
    fun getDisableTime(): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        return try {
            prefs.getInt(PREF_HBM_DISABLE_TIME_KEY, 1)
        } catch (e: ClassCastException) {
            try {
                val stringValue = prefs.getString(PREF_HBM_DISABLE_TIME_KEY, "1")
                val intValue = stringValue?.toIntOrNull() ?: 1
                prefs.edit().putInt(PREF_HBM_DISABLE_TIME_KEY, intValue).apply()
                intValue
            } catch (ne: NumberFormatException) { 
                prefs.edit().putInt(PREF_HBM_DISABLE_TIME_KEY, 1).apply()
                1
            }
        }
    }
    
    fun setDisableTime(value: Int) {
        PreferenceManager.getDefaultSharedPreferences(appContext).edit()
            .putInt(PREF_HBM_DISABLE_TIME_KEY, value).apply()
    }
    
    fun setThresholdValue(value: Int) {
        PreferenceManager.getDefaultSharedPreferences(appContext).edit()
            .putInt(PREF_AUTO_HBM_THRESHOLD_KEY, value).apply()
    }

    interface HBMStateListener {
        fun onHBMStateChanged(enabled: Boolean, owner: HBMOwner)
    }

    enum class HBMOwner {
        NONE,
        MANUAL,
        AUTO_SERVICE
    }

    companion object {
        private const val TAG = "HBMMgr"
        const val MIN_THRESHOLD = 2000
        const val MAX_THRESHOLD = 20000
        const val STEP_THRESHOLD = 2000

        const val MIN_TIME_DELAY = 1
        const val MAX_TIME_DELAY = 10
        const val STEP_TIME_DELAY = 1

        @Volatile private var instance: HBMManager? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: HBMManager(context.applicationContext).also { instance = it }
            }
    }
}