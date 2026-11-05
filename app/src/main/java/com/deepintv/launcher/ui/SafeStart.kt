package com.deepintv.launcher.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.deepintv.launcher.R

/** Starts [intent]; an app uninstalled a moment ago, or one that refuses to open, shows a toast
 * instead of crashing the launcher. True when it started. */
fun safeStartActivity(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (e: RuntimeException) {
    Log.w("SafeStart", "Could not start $intent", e)
    Toast.makeText(context, R.string.app_launch_failed, Toast.LENGTH_SHORT).show()
    false
}
