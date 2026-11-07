package com.deepintv.launcher.ui.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepintv.launcher.ui.theme.DeepinTextPrimary

/** In-launcher toast drawn as a glass pill, top right. Falls back to the system toast whenever
 * the launcher screen that hosts it is not on screen (see [GlassToastHost]). */
object GlassToastState {
    var message by mutableStateOf<String?>(null)
        internal set
    internal var hosted = false
    private val handler = Handler(Looper.getMainLooper())
    private val clear = Runnable { message = null }

    fun show(text: String, long: Boolean) {
        message = text
        handler.removeCallbacks(clear)
        handler.postDelayed(clear, if (long) 3500L else 2000L)
    }
}

fun showGlassToast(context: Context, text: String, long: Boolean = false) {
    if (GlassToastState.hosted) GlassToastState.show(text, long)
    else Toast.makeText(context, text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun showGlassToast(context: Context, resId: Int, long: Boolean = false) =
    showGlassToast(context, context.getString(resId), long)

@Composable
fun GlassToastHost() {
    // Only "hosted" while the launcher is actually in front; otherwise the system toast is used.
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> GlassToastState.hosted = true
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> GlassToastState.hosted = false
                else -> Unit
            }
        }
        GlassToastState.hosted = owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            GlassToastState.hosted = false
        }
    }
    val text = GlassToastState.message ?: return
    Box(modifier = Modifier.fillMaxSize().padding(top = 32.dp, end = 32.dp), contentAlignment = Alignment.TopEnd) {
        GlassSurface(cornerRadius = 16) {
            Text(
                text = text,
                color = DeepinTextPrimary,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
    }
}
