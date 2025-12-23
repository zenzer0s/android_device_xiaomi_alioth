package org.lineageos.xiaomiparts.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.lineageos.xiaomiparts.utils.readOneLine
import org.lineageos.xiaomiparts.utils.writeLine
import org.lineageos.xiaomiparts.utils.Logging
import org.lineageos.xiaomiparts.utils.fileExists
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChargeUtils private constructor(context: Context) {

    private val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val mutex = Mutex()

    suspend fun isBypassChargeEnabled(): Boolean {
        if (!fileExists(BYPASS_CHARGE_NODE)) {
            return sharedPrefs.getBoolean(PREF_BYPASS_CHARGE, false)
        }

        val nodeValue = readOneLine(BYPASS_CHARGE_NODE)?.trim()
        return when (nodeValue) {
            "1" -> true
            "0" -> false
            else -> sharedPrefs.getBoolean(PREF_BYPASS_CHARGE, false)
        }
    }

    suspend fun enableBypassCharge(enable: Boolean) {
        mutex.withLock {
            if (!fileExists(BYPASS_CHARGE_NODE)) {
                Logging.w(TAG, "Bypass charge not supported: node missing")
                return
            }

            val writeOk = writeLine(BYPASS_CHARGE_NODE, if (enable) "1" else "0")
            if (!writeOk) {
                Logging.e(TAG, "Failed to write bypass charge state")
                return
            }

            sharedPrefs.edit().putBoolean(PREF_BYPASS_CHARGE, enable).apply()
        }
    }

    suspend fun isBypassChargeSupported(): Boolean = fileExists(BYPASS_CHARGE_NODE)

    companion object {
        private const val TAG = "Charge"

        const val BYPASS_DISABLED = 0
        const val BYPASS_ENABLED = 1

        @Volatile private var instance: ChargeUtils? = null

        fun getInstance(context: Context) =
            instance
                ?: synchronized(this) {
                    instance ?: ChargeUtils(context.applicationContext).also { instance = it }
                }
    }
}