package org.lineageos.xiaomiparts.ui.thermal

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import org.lineageos.xiaomiparts.data.ThermalUtils
import org.lineageos.xiaomiparts.ui.thermal.ThermalScreen
import org.lineageos.xiaomiparts.theme.XiaomiPartsTheme
import org.lineageos.xiaomiparts.utils.Logging


class ThermalComposeActivity : ComponentActivity() {

    private lateinit var thermalUtils: ThermalUtils
    private lateinit var launcherApps: LauncherApps

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        Logging.d(TAG, "onCreate")
        
        thermalUtils = ThermalUtils.getInstance(this)
        launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        
        setContent {
            XiaomiPartsTheme() {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    val viewModel: ThermalViewModel = viewModel(
                        factory = ThermalViewModelFactory(
                            thermalUtils = thermalUtils,
                            launcherApps = launcherApps
                        )
                    )
                    
                    ThermalScreen(
                        viewModel = viewModel,
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logging.d(TAG, "onDestroy")
    }

    companion object {
        private const val TAG = "ThermalComposeActivity"
    }
}