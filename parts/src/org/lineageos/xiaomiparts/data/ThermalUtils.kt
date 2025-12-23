package org.lineageos.xiaomiparts.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.UserHandle
import android.provider.MediaStore
import android.telecom.DefaultDialerManager.getDefaultDialerApplication
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.lineageos.xiaomiparts.R
import org.lineageos.xiaomiparts.utils.Logging
import org.lineageos.xiaomiparts.utils.writeLine
import org.lineageos.xiaomiparts.services.ThermalService
import com.android.settingslib.applications.AppUtils.isBrowserApp


class ThermalUtils
private constructor(
    private val context: Context,
) {
    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val serviceIntent = Intent(context, ThermalService::class.java)

    private val toggleScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val toggleMutex = Mutex()
    @Volatile private var toggleJob: Job? = null

    var enabled: Boolean = sharedPrefs.getBoolean(PREF_THERMAL_ENABLED, false)
        set(value) {
            if (field == value) return
            field = value
            sharedPrefs.edit().putBoolean(PREF_THERMAL_ENABLED, value).apply()

            val targetEnabled = value
            toggleJob?.cancel()
            toggleJob = toggleScope.launch {
                if (!isActive) return@launch
                toggleMutex.withLock {
                    if (!isActive) return@withLock
                    if (enabled != targetEnabled) return@withLock

                    if (targetEnabled) {
                        startService()
                    } else {
                        setDefaultThermalProfile()
                        stopService()
                    }
                }
            }
        }

    var value: String = readValue()
        set(value) {
            if (field == value) return
            field = value
            writeValue(value)
        }

    fun startService() {
        if (enabled) {
            Logging.d(TAG, "startService")
            context.startServiceAsUser(serviceIntent, UserHandle.CURRENT)
        }
    }

    suspend fun stopService() {
        Logging.d(TAG, "stopService")
        context.stopService(serviceIntent)
    }

    private fun writeValue(value: String) {
        Logging.d(TAG, "writing pref value: $value")
        sharedPrefs.edit().putString(PREF_THERMAL_CONTROL, value).apply()
    }

    private fun readValue(): String = normalizeStoredValue(sharedPrefs.getString(PREF_THERMAL_CONTROL, null))

    fun writePackage(packageName: String, mode: Int) {
        Logging.d(TAG, "writePackage: $packageName -> $mode")
        val thermalStates = ThermalState.values()
        if (mode !in thermalStates.indices) {
            Logging.w(TAG, "writePackage: invalid mode index=$mode")
            return
        }

        val newValue = normalizeStoredValue(value).replace("$packageName,", "")
        val modes = newValue.split(":").toMutableList()
        if (modes.size != thermalStates.size) {
            Logging.w(TAG, "writePackage: malformed state string, resetting")
            value = DEFAULT_VALUE
            return
        }

        modes[mode] += "$packageName,"
        value = modes.joinToString(":")
    }

    fun getStateForPackage(packageName: String): ThermalState {
        val modes = normalizeStoredValue(value).split(":")
        return ThermalState.values().find { state -> modes.getOrNull(state.id)?.contains("$packageName,") == true }
            ?: getDefaultStateForPackage(packageName)
    }

    fun resetProfiles() {
        Logging.d(TAG, "resetProfiles")
        value = DEFAULT_VALUE
    }

    suspend fun setDefaultThermalProfile() {
        Logging.d(TAG, "setDefaultThermalProfile")
        val ok = writeLine(THERMAL_SCONFIG, THERMAL_STATE_OFF)
        if (!ok) {
            Logging.e(TAG, "Failed to write default thermal profile")
        }
    }

    suspend fun setThermalProfile(packageName: String) {
        if (packageName.isEmpty()) {
            Logging.d(TAG, "setThermalProfile: packageName is empty")
            return
        }
        val state = getStateForPackage(packageName)
        Logging.d(TAG, "setThermalProfile: $packageName -> $state")
        val ok = writeLine(THERMAL_SCONFIG, state.config)
        if (!ok) {
            Logging.e(TAG, "Failed to write thermal profile: $state")
        }
    }

    private fun getDefaultStateForPackage(packageName: String): ThermalState {
        runCatching { context.packageManager.getApplicationInfo(packageName, 0) }
            .onSuccess {
                when (it.category) {
                    ApplicationInfo.CATEGORY_GAME -> return ThermalState.GAMING
                    ApplicationInfo.CATEGORY_VIDEO -> return ThermalState.VIDEO
                    ApplicationInfo.CATEGORY_MAPS -> return ThermalState.NAVIGATION
                }
            }
            .onFailure {
                return ThermalState.DEFAULT
            }

        return when {
            NAVIGATION_PACKAGES.contains(packageName) -> ThermalState.NAVIGATION
            VIDEO_CALL_PACKAGES.contains(packageName) -> ThermalState.VIDEOCALL
            BENCHMARKING_APPS.contains(packageName) -> ThermalState.BENCHMARK
            getDefaultDialerApplication(context) == packageName -> ThermalState.DIALER
            isBrowserApp(context, packageName, UserHandle.myUserId()) -> ThermalState.BROWSER
            isCameraApp(packageName) -> ThermalState.CAMERA
            else -> ThermalState.DEFAULT
        }
    }

    private fun isCameraApp(packageName: String): Boolean {
        val cameraIntent =
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).setPackage(packageName)
        val activities =
            context.packageManager.queryIntentActivitiesAsUser(
                cameraIntent,
                PackageManager.MATCH_ALL,
                UserHandle.myUserId(),
            )
        return activities.any { it.activityInfo != null }
    }

    enum class ThermalState(
        val id: Int,
        val config: String,
        val prefix: String,
        @StringRes val label: Int,
    ) {
        BENCHMARK(
            0,
            "10", // thermal-nolimits.conf
            "thermal.benchmark=",
            R.string.thermal_benchmark,
        ),
        BROWSER(
            1,
            "11", // thermal-class0.conf
            "thermal.browser=",
            R.string.thermal_browser,
        ),
        CAMERA(
            2,
            "12", // thermal-camera.conf
            "thermal.camera=",
            R.string.thermal_camera,
        ),
        DIALER(
            3,
            "8", // thermal-phone.conf
            "thermal.dialer=",
            R.string.thermal_dialer,
        ),
        GAMING(
            4,
            "13", // thermal-tgame.conf
            "thermal.gaming=",
            R.string.thermal_gaming,
        ),
        NAVIGATION(
            5,
            "19", // thermal-navigation.conf
            "thermal.navigation=",
            R.string.thermal_navigation,
        ),
        VIDEOCALL(
            6,
            "4", // thermal-videochat.conf
            "thermal.streaming=",
            R.string.thermal_streaming,
        ),
        VIDEO(
            7,
            "21", // thermal-video.conf
            "thermal.video=",
            R.string.thermal_video,
        ),
        DEFAULT(
            8,
            "20", // thermal-mgame.conf [zenzer0rs: changed from normal.conf to mgame.conf, because mgame has lower frequency all cores are locked at 1.8GHz while normal.conf goes up to 2.4GHz]
            "thermal.default=",
            R.string.thermal_default,
        ),
    }

    companion object {
        private const val TAG = "ThermalUtils"
        private const val THERMAL_STATE_OFF = "20" // thermal-mgame.conf

        private val DEFAULT_VALUE = ThermalState.values().map { it.prefix }.joinToString(":")

        private val NAVIGATION_PACKAGES =
            arrayOf(
                "com.google.android.apps.maps",
                "com.google.android.apps.mapslite",
                "com.waze",
            )
        private val VIDEO_CALL_PACKAGES =
            arrayOf(
                "com.google.android.apps.tachyon",
                "us.zoom.videomeetings",
                "com.microsoft.teams",
                "com.skype.raider",
            )
        private val BENCHMARKING_APPS =
            arrayOf(
                "com.primatelabs.geekbench5",
                "com.primatelabs.geekbench6",
                "com.antutu.ABenchMark",
                "com.futuremark.dmandroid.application",
                "com.futuremark.pcmark.android.benchmark",
                "com.glbenchmark.glbenchmark27",
                "com.texts.throttlebench",
                "skynet.cputhrottlingtest",
            )

        private fun normalizeStoredValue(raw: String?): String {
            if (raw.isNullOrBlank()) return DEFAULT_VALUE

            val states = ThermalState.values()
            val segments = raw.split(":")
            val payloadByPrefix = HashMap<String, String>(states.size)

            for (segment in segments) {
                for (state in states) {
                    if (segment.startsWith(state.prefix)) {
                        payloadByPrefix[state.prefix] = segment.removePrefix(state.prefix)
                        break
                    }
                }
            }

            return states.joinToString(":") { state -> state.prefix + (payloadByPrefix[state.prefix] ?: "") }
        }

        @Volatile private var instance: ThermalUtils? = null

        fun getInstance(context: Context) =
            instance
                ?: synchronized(this) {
                    instance ?: ThermalUtils(context.applicationContext).also { instance = it }
                }
    }
}
