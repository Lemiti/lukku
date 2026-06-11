package com.startup.lukku.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.startup.lukku.accessibility.overlay.ComposeOverlayManager

class AccountabilityService : AccessibilityService() {

    // Hardcoded hit-list for the MVP
    private val blockedApps = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.twitter.android",      // X
        "org.telegram.messenger",   // Telegram
        "com.android.chrome",       // Chrome
        "com.android.vending"       // Play Store
    )

    private lateinit var overlayManager: ComposeOverlayManager

    // Tracks the last blocked package so we can dismiss the overlay the
    // moment the user navigates away from the blocked app.
    private var activeBlockedPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = ComposeOverlayManager(applicationContext)
        Log.w("LukkuEngine", "Service Connected. The Rooster is watching.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (blockedApps.contains(packageName)) {
            // Only trigger if this is a *new* blocked target — avoids
            // re-inflating the overlay on every sub-window state change
            // within the same blocked app.
            if (activeBlockedPackage != packageName) {
                activeBlockedPackage = packageName
                Log.e("LukkuEngine", "TARGET ACQUIRED: $packageName. INJECTING OVERLAY.")
                overlayManager.showOverlay(packageName)
            }
        } else {
            // User navigated away from the blocked app — tear down overlay.
            if (activeBlockedPackage != null) {
                Log.d("LukkuEngine", "Target left. Hiding overlay. Allowed: $packageName")
                overlayManager.hideOverlay()
                activeBlockedPackage = null
            }
        }
    }

    override fun onInterrupt() {
        Log.e("LukkuEngine", "Service Interrupted.")
        overlayManager.hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.destroy()
        Log.w("LukkuEngine", "Service destroyed. Overlay cleaned up.")
    }
}
