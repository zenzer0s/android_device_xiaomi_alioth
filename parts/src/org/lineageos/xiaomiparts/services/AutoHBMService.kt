package org.lineageos.xiaomiparts.services

import android.app.KeyguardManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.UserHandle
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.lineageos.xiaomiparts.data.PREF_DC_DIMMING_KEY
import org.lineageos.xiaomiparts.data.HBMManager
import org.lineageos.xiaomiparts.utils.Logging

class AutoHBMService : Service() {

    private lateinit var sensorManager: SensorManager
    private lateinit var powerManager: PowerManager
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var prefs: SharedPreferences
    
    private var lightSensor: Sensor? = null
    private var currentLux = 0f
    
    private val handler = Handler(Looper.getMainLooper())
    private var disableHBMRunnable: Runnable? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var autoHBMActive = false
    
    private lateinit var hbmManager: HBMManager
    
    private val lightSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            currentLux = event.values[0]
            
            val luxThreshold = hbmManager.getThresholdValue().toFloat()
            
            val disableDelaySeconds = hbmManager.getDisableTime().toLong()
            
            val isLocked = keyguardManager.isKeyguardLocked
            val dcDimmingEnabled = prefs.getBoolean(PREF_DC_DIMMING_KEY, false)
            
            if (currentLux > luxThreshold) {
                disableHBMRunnable?.let {
                    handler.removeCallbacks(it)
                    disableHBMRunnable = null
                }

                if (!isLocked && !dcDimmingEnabled && !autoHBMActive) {
                    Logging.i(TAG, "Lux $currentLux > $luxThreshold, enabling Auto HBM")
                    
                    serviceScope.launch {
                        val enabled = hbmManager.enableHBM(HBMManager.HBMOwner.AUTO_SERVICE)
                        handler.post {
                            autoHBMActive = enabled || hbmManager.isHBMEnabled()
                        }

                        if (enabled) {
                            Logging.i(TAG, "Auto HBM enabled successfully")
                        } else {
                            Logging.i(TAG, "Auto HBM enable skipped/blocked")
                        }
                    }
                }
            }
            else if (currentLux < luxThreshold && autoHBMActive) {
                disableHBMRunnable?.let {
                    handler.removeCallbacks(it)
                }
                
                disableHBMRunnable = Runnable {
                    if (currentLux < luxThreshold && autoHBMActive) {
                        Logging.i(TAG, "Lux $currentLux < $luxThreshold after delay, disabling Auto HBM")
                        
                        serviceScope.launch {
                            val disabled = hbmManager.disableHBM(HBMManager.HBMOwner.AUTO_SERVICE)
                            handler.post {

                                autoHBMActive = false
                            }

                            if (disabled) {
                                Logging.i(TAG, "Auto HBM disabled successfully")
                            } else {
                                Logging.i(TAG, "Auto HBM disable skipped/blocked")
                            }
                        }
                    } else {
                        Logging.i(TAG, "Lux increased during delay, keeping Auto HBM enabled")
                    }
                    disableHBMRunnable = null
                }
                
                handler.postDelayed(disableHBMRunnable!!, disableDelaySeconds * 1000)
                Logging.i(TAG, "Scheduled Auto HBM disable in ${disableDelaySeconds}s")
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        }
    }
    
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Logging.i(TAG, "Screen ON - activating light sensor")
                    activateLightSensor()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    Logging.i(TAG, "Screen OFF - deactivating light sensor")
                    deactivateLightSensor()
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Logging.i(TAG, "AutoHBMService created")
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        hbmManager = HBMManager.getInstance(this)
        
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (lightSensor == null) {
            Logging.e(TAG, "No light sensor found! Auto HBM will not work")
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)
        
        if (powerManager.isInteractive) {
            Logging.i(TAG, "Screen is on at service start, activating sensor")
            activateLightSensor()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logging.i(TAG, "AutoHBMService started")
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Logging.i(TAG, "AutoHBMService destroyed")
        
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            Logging.e(TAG, "Error unregistering receiver", e)
        }
        
        deactivateLightSensor()
        
        if (autoHBMActive) {
            Logging.i(TAG, "Service stopping - disabling Auto HBM")
            
            serviceScope.launch {
                val disabled = hbmManager.disableHBM(HBMManager.HBMOwner.AUTO_SERVICE)
                handler.post {
                    autoHBMActive = false
                }
                if (!disabled) {
                    Logging.i(TAG, "Auto HBM disable skipped/blocked")
                }
            }
        }
        
        disableHBMRunnable?.let {
            handler.removeCallbacks(it)
            disableHBMRunnable = null
        }
        serviceScope.cancel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun activateLightSensor() {
        lightSensor?.let { sensor ->
            val registered = sensorManager.registerListener(
                lightSensorListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            if (registered) {
                Logging.i(TAG, "Light sensor activated")
            } else {
                Logging.e(TAG, "Failed to register light sensor listener")
            }
        }
    }
    
    private fun deactivateLightSensor() {
        sensorManager.unregisterListener(lightSensorListener)
        Logging.i(TAG, "Light sensor deactivated")
        
        disableHBMRunnable?.let {
            handler.removeCallbacks(it)
            disableHBMRunnable = null
        }
        
        if (autoHBMActive) {
            Logging.i(TAG, "Screen off - disabling Auto HBM")
            
            serviceScope.launch {
                val disabled = hbmManager.disableHBM(HBMManager.HBMOwner.AUTO_SERVICE)
                handler.post {
                    autoHBMActive = false
                }
                if (!disabled) {
                    Logging.i(TAG, "Auto HBM disable skipped/blocked")
                }
            }
        }
    }
    
    companion object {
        private const val TAG = "AutoHBM"
        
        fun enableService(context: Context, enable: Boolean) {
            val intent = Intent(context, AutoHBMService::class.java)
            if (enable) {
                context.startServiceAsUser(intent, UserHandle.CURRENT)
            } else {
                context.stopServiceAsUser(intent, UserHandle.CURRENT)
            }
        }
    }
}
