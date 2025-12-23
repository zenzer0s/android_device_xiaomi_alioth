package org.lineageos.xiaomiparts.data

import android.content.Context
import android.os.SystemProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lineageos.xiaomiparts.utils.Logging

class ReVancedManager private constructor(context: Context) {

    suspend fun isEnabled(): Boolean = withContext(Dispatchers.IO) {
        SystemProperties.getBoolean(PROPERTY_REVANCED_ENABLED, DEFAULT_ENABLED)
    }

    suspend fun setEnabled(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            SystemProperties.set(PROPERTY_REVANCED_ENABLED, if (enabled) "true" else "false")

            val readBack = SystemProperties.getBoolean(PROPERTY_REVANCED_ENABLED, !enabled)
            val success = readBack == enabled
            if (success) {
                Logging.i(TAG, "ReVanced ${if (enabled) "enabled" else "disabled"}")
            } else {
                Logging.e(TAG, "ReVanced property write did not stick (expected=$enabled, actual=$readBack)")
            }

            success
        } catch (e: Exception) {
            Logging.e(TAG, "Failed to set ReVanced state", e)
            false
        }
    }

    companion object {
        private const val TAG = "ReVancedManager"
        private const val DEFAULT_ENABLED = true

        @Volatile private var instance: ReVancedManager? = null

        fun getInstance(context: Context) =
            instance
                ?: synchronized(this) {
                    instance ?: ReVancedManager(context.applicationContext).also { instance = it }
                }
    }
}