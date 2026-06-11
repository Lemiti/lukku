package com.startup.lukku.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.startup.lukku.accessibility.overlay.ComposeOverlayManager

class AccountabilityService : AccessibilityService() {

    private lateinit var overlayManager: ComposeOverlayManager

    private val blockedApps = setOf(
        "com.instagram.android",
        "org.telegram.messenger",
        "com.android.chrome",
        "com.android.vending"
    )

    // A list of safe apps (like your home launcher) to trigger the hide overlay command
    private val safeApps = setOf(
        "com.sec.android.app.launcher", // Samsung Home Screen
        "com.startup.lukku"             // Our own app
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.w("LukkuEngine", "Service Connected. The Rooster is watching.")
        
        // Initialize the UI Manager
        overlayManager = ComposeOverlayManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
            if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

            val packageName = event.packageName?.toString() ?: return

            // 1. THE LOOP BREAKER: If the OS reports our own app taking focus, ignore it entirely.
            if (packageName == "com.startup.lukku" || packageName.contains("systemui")) {
                return
            }

            // 2. THE TRIGGER: If it is a blocked app, show the overlay.
            if (blockedApps.contains(packageName)) {
                Log.e("LukkuEngine", "TARGET ACQUIRED: $packageName. INJECTING OVERLAY.")
                overlayManager.showOverlay(packageName)
            } 
            // 3. THE DISMISSAL: If it is any other app (like the home launcher), hide the overlay.
            else {
                overlayManager.hideOverlay()
            }
    }

    override fun onInterrupt() {
        Log.e("LukkuEngine", "Service Interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Prevent memory leaks if the service is killed
        if (::overlayManager.isInitialized) {
            overlayManager.hideOverlay()
        }
    }
}