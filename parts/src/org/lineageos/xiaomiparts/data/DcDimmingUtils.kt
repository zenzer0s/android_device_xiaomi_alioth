package org.lineageos.xiaomiparts.data

import android.content.Context
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.lineageos.xiaomiparts.services.HBMModeTileService
import org.lineageos.xiaomiparts.utils.Logging
import org.lineageos.xiaomiparts.utils.fileExists
import org.lineageos.xiaomiparts.utils.writeLine
import java.io.File

class DcDimmingUtils private constructor(context: Context) {

    private val appContext = context.applicationContext

    private val hbmFile = File(HBM_SYSFS_PATH)

    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(appContext)
    
    private val mutex = Mutex()

    suspend fun isDcDimmingSupported(): Boolean = fileExists(DC_DIMMING_NODE)

    fun isDcDimmingEnabled(): Boolean {
        return sharedPrefs.getBoolean(PREF_DC_DIMMING_KEY, false)
    }

    suspend fun setDcDimmingEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        mutex.withLock {
            Logging.d(TAG, "Setting DC Dimming: $enabled")

            if (!fileExists(DC_DIMMING_NODE)) {
                Logging.w(TAG, "DC dimming not supported: node missing")
                return@withLock
            }

            val writeOk = writeLine(DC_DIMMING_NODE, if (enabled) "1" else "0")
            if (!writeOk) {
                Logging.e(TAG, "Failed to write DC dimming state")
                return@withLock
            }

            sharedPrefs.edit().putBoolean(PREF_DC_DIMMING_KEY, enabled).apply()

            if (enabled) {
                val hbmDisabled = disableHBM()
                if (!hbmDisabled) {
                    Logging.e(TAG, "HBM disable failed; rolling back DC dimming")

                    val rollbackOk = writeLine(DC_DIMMING_NODE, "0")
                    if (!rollbackOk) {
                        Logging.e(TAG, "Failed to roll back DC dimming state")
                    }

                    sharedPrefs.edit().putBoolean(PREF_DC_DIMMING_KEY, false).apply()
                    return@withLock
                }
            } else {
                val ok = hbmFile.setWritable(true)
                if (!ok) {
                    Logging.w(TAG, "Failed to make HBM node writable")
                }
            }
        }
    }

    private suspend fun disableHBM(): Boolean {
        Logging.d(TAG, "Disabling HBM due to DC dimming enable")

        val hbmManager = HBMManager.getInstance(appContext)

        val disabled = hbmManager.disableHBM(HBMManager.HBMOwner.MANUAL)
        if (!disabled) {
            Logging.w(TAG, "Failed to disable HBM while enabling DC dimming")
            return false
        }

        val ok = hbmFile.setReadOnly()
        if (!ok) {
            Logging.w(TAG, "Failed to make HBM node read-only")
        }

        HBMModeTileService.updateTile(appContext, false)

        return true
    }

    companion object {
        private const val TAG = "DcDimmingUtils"

        @Volatile private var instance: DcDimmingUtils? = null

        fun getInstance(context: Context) =
            instance
                ?: synchronized(this) {
                    instance ?: DcDimmingUtils(context.applicationContext).also { instance = it }
                }
    }
}