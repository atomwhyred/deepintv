package com.deepintv.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.deepintv.launcher.data.AppRepository
import com.deepintv.launcher.ui.HomeScreen
import com.deepintv.launcher.ui.components.trimAppIconCache
import com.deepintv.launcher.ui.theme.DeepinTVTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeepinTVTheme {
                HomeScreen(appRepository = AppRepository(packageManager))
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        trimAppIconCache(level)
    }
}
