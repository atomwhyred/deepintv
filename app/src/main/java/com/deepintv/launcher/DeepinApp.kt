package com.deepintv.launcher

import android.app.Application
import com.deepintv.launcher.data.SafeModeRepository

class DeepinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val safeMode = SafeModeRepository(this)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { safeMode.recordCrash() }
            previous?.uncaughtException(thread, error)
        }
    }
}
