package com.deepintv.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.deepintv.launcher.MainActivity
import com.deepintv.launcher.R
import com.deepintv.launcher.data.KeyRemapRepository
import com.deepintv.launcher.data.LauncherSettingsRepository
import com.deepintv.launcher.data.RemapAction

/**
 * The only way a normal (non-system, non-rooted) app can open the TV's own power dialog or lock
 * the screen: AccessibilityService.performGlobalAction() is unprotected for a bound, user-enabled
 * accessibility service, unlike PowerManager.goToSleep() or ACTION_REQUEST_SHUTDOWN, which are
 * signature|system-protected and throw a SecurityException here. The person must turn this service
 * on once in Settings > Accessibility; ControlCenter explains why and links there when it's off.
 *
 * It also keeps the Home button pointing here: some TVs (Google TV among them) open their own
 * launcher on Home whatever the default is, so when that launcher's window appears this brings
 * DeepinTV back to the front. Switchable in the control center.
 *
 * It also owns key remapping: with flagRequestFilterKeyEvents it sees every remote key before the
 * foreground app does, so a mapped code is consumed here and never reaches anything else.
 */
class DeepinAccessibilityService : AccessibilityService() {

    private lateinit var remapRepository: KeyRemapRepository
    private lateinit var settingsRepository: LauncherSettingsRepository
    private var lastReopenAt = 0L
    private var mappings: Map<Int, RemapAction> = emptyMap()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        // Some OEM builds only honour flagRequestFilterKeyEvents from the XML config when it's
        // also set here at runtime; without this, onKeyEvent below is never called.
        serviceInfo = serviceInfo?.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        remapRepository = KeyRemapRepository(this)
        settingsRepository = LauncherSettingsRepository(this)
        refreshMappings()
        armSleepTimer()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        // Booting: the TV opens its own launcher first and a window event for it may already have
        // gone by when this service binds, so bring DeepinTV up once if the TV has only just started.
        if (settingsRepository.homeOverride() && SystemClock.elapsedRealtime() < BOOT_WINDOW_MS) {
            Handler(Looper.getMainLooper()).postDelayed({ openLauncher() }, BOOT_OPEN_DELAY_MS)
        }
    }

    private fun openLauncher() {
        lastReopenAt = SystemClock.uptimeMillis()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (settingsRepository.homeOnScreenOff()) performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(screenOffReceiver) }
        if (instance == this) instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (!settingsRepository.homeOverride() || pkg == packageName) return
        // The event's class is often just a view (android.widget.FrameLayout), not the activity,
        // so match on the package of those Home screens.
        if (pkg !in otherHomePackages()) return

        // One Home press can raise several windows in a row: reopen once, not once per window.
        val now = SystemClock.uptimeMillis()
        if (now - lastReopenAt < REOPEN_THROTTLE_MS) return
        openLauncher()
    }

    private val homeIntent get() = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    private var homePackages: Set<String> = emptySet()
    private var homePackagesAt = 0L

    /** Window events fire on every screen change; the PackageManager query behind them is not
     * cheap, and the set only changes when a launcher is installed, so it is kept for a minute. */
    private fun otherHomePackages(): Set<String> {
        val now = SystemClock.uptimeMillis()
        if (homePackagesAt == 0L || now - homePackagesAt > HOME_CACHE_MS) {
            homePackages = otherHomeScreens().map { it.first }.toSet()
            homePackagesAt = now
        }
        return homePackages
    }

    /** The Home screens of the TV's own (system) launchers, found rather than listed so any
     * brand is covered. Launchers the person installed are left alone — opening one of those is
     * their choice — and so are setup/recovery/fallback screens: matching the screen, not the
     * whole app, matters because apps like TV Settings declare a fallback Home of their own. */
    private fun otherHomeScreens(): Set<Pair<String, String>> =
        packageManager.queryIntentActivities(homeIntent, 0)
            .filter { it.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 }
            .map { it.activityInfo.packageName to it.activityInfo.name }
            .filter { (pkg, cls) ->
                pkg != packageName &&
                    listOf("setup", "recovery", "fallback").none { cls.contains(it, ignoreCase = true) || pkg.contains(it, ignoreCase = true) }
            }
            .toSet()

    override fun onInterrupt() = Unit

    fun refreshMappings() {
        mappings = remapRepository.all()
    }

    /** Key codes whose press this service took, so the matching key-up and any auto-repeat are
     * swallowed too: letting only the up through would hand the foreground app a release with no
     * press, and holding a remapped button would otherwise fire its action over and over. */
    private val consumedKeys = mutableSetOf<Int>()

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (event.action == KeyEvent.ACTION_UP) {
            return consumedKeys.remove(code) || super.onKeyEvent(event)
        }
        if (event.action != KeyEvent.ACTION_DOWN) return super.onKeyEvent(event)
        // Auto-repeat of a press already taken.
        if (event.repeatCount > 0) return code in consumedKeys || super.onKeyEvent(event)

        val learn = learnModeCallback
        if (learn != null) {
            learnModeCallback = null
            consumedKeys.add(code)
            learn(code)
            return true
        }

        val action = mappings[code] ?: return super.onKeyEvent(event)
        consumedKeys.add(code)
        performAction(action)
        return true
    }

    private fun performAction(action: RemapAction) {
        when (action) {
            RemapAction.Home -> performGlobalAction(GLOBAL_ACTION_HOME)
            RemapAction.ControlCenter -> onOverlayRequest?.invoke(action)
            RemapAction.AppDrawer -> onOverlayRequest?.invoke(action)
            is RemapAction.Launch -> {
                val intent = packageManager.getLeanbackLaunchIntentForPackage(action.packageName)
                    ?: packageManager.getLaunchIntentForPackage(action.packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent?.let { startActivity(it) }
            }
        }
    }

    fun openPowerDialog() {
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }

    fun lockScreen() {
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val sleepPrefs by lazy { getSharedPreferences("deepintv_sleep", MODE_PRIVATE) }
    private val warnTask = Runnable {
        android.widget.Toast.makeText(this, R.string.sleep_warning, android.widget.Toast.LENGTH_LONG).show()
    }
    private val sleepTask = Runnable {
        sleepPrefs.edit().remove(KEY_SLEEP_DEADLINE).apply()
        // Pause what is playing first, like a remote's pause key, then put the TV to sleep.
        val audio = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
        lockScreen()
    }

    /** Sleep timer: pauses media and locks the screen (the TV goes to standby) after [minutes];
     * 0 cancels it. The deadline is stored, so a service restart re-arms it. */
    fun setSleepTimer(minutes: Int) {
        handler.removeCallbacks(warnTask)
        handler.removeCallbacks(sleepTask)
        if (minutes <= 0) {
            sleepPrefs.edit().remove(KEY_SLEEP_DEADLINE).apply()
            return
        }
        sleepPrefs.edit().putLong(KEY_SLEEP_DEADLINE, System.currentTimeMillis() + minutes * 60_000L).apply()
        armSleepTimer()
    }

    /** Milliseconds left on the sleep timer, or 0 when none is running. */
    fun sleepRemainingMs(): Long =
        (sleepPrefs.getLong(KEY_SLEEP_DEADLINE, 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    private fun armSleepTimer() {
        val left = sleepRemainingMs()
        if (left <= 0L) return
        handler.postDelayed(sleepTask, left)
        if (left > 60_000L) handler.postDelayed(warnTask, left - 60_000L)
    }

    companion object {
        private const val KEY_SLEEP_DEADLINE = "deadline"
        private const val REOPEN_THROTTLE_MS = 1200L
        private const val HOME_CACHE_MS = 60_000L
        private const val BOOT_WINDOW_MS = 3 * 60 * 1000L
        private const val BOOT_OPEN_DELAY_MS = 1500L

        var instance: DeepinAccessibilityService? = null
            private set

        val isEnabled: Boolean get() = instance != null

        /** Set while the remap picker is waiting for the next physical key press; cleared after
         * one capture (or when the picker closes). */
        var learnModeCallback: ((Int) -> Unit)? = null

        /** Set by Home while it's on screen, so a remapped button can open an overlay that only
         * Compose (not this service) knows how to draw. */
        var onOverlayRequest: ((RemapAction) -> Unit)? = null
    }
}
