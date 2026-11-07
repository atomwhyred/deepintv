package com.deepintv.launcher

import android.os.Bundle
import android.view.KeyEvent
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

    /** Channel up/down open the TV's Live TV app on some firmwares even with Home in front;
     * there is nothing to change channel on here, so the keys are swallowed. */
    private fun isChannelKey(code: Int) = code == KeyEvent.KEYCODE_CHANNEL_UP || code == KeyEvent.KEYCODE_CHANNEL_DOWN

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        isChannelKey(keyCode) || super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        isChannelKey(keyCode) || super.onKeyUp(keyCode, event)

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        trimAppIconCache(level)
    }
}
