package com.startup.lukku.accessibility.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

private const val TAG = "LukkuOverlay"

/**
 * ComposeOverlayManager
 *
 * Draws a full-screen Jetpack Compose overlay directly from a Service context
 * using TYPE_ACCESSIBILITY_OVERLAY. Because there is no host Activity, this
 * class manually implements the three owner interfaces that Compose's
 * ViewTreeOwners infrastructure requires before it will attach and render.
 *
 * Lifecycle contract:
 *  - showOverlay()  → ON_CREATE → ON_START → ON_RESUME
 *  - hideOverlay()  → ON_PAUSE  → ON_STOP  → ON_DESTROY
 */
class ComposeOverlayManager(private val context: Context) :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    // ─── LifecycleOwner ──────────────────────────────────────────────────────

    private val lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    // ─── ViewModelStoreOwner ─────────────────────────────────────────────────

    private val _viewModelStore = ViewModelStore()

    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    // ─── SavedStateRegistryOwner ─────────────────────────────────────────────

    private val savedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ─── WindowManager ───────────────────────────────────────────────────────

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var composeView: ComposeView? = null

    // ─── State ───────────────────────────────────────────────────────────────

    private var isShowing = false

    // ─── Initialiser ─────────────────────────────────────────────────────────

    init {
        // SavedState must be initialised before any lifecycle event fires.
        savedStateRegistryController.performRestore(Bundle())
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Inflates the overlay and attaches it to the WindowManager.
     * Safe to call multiple times — subsequent calls while visible are no-ops.
     *
     * @param packageName The intercepted package that triggered this overlay,
     *                    reserved for future contextual messaging.
     */
    fun showOverlay(packageName: String) {
        if (isShowing) return

        Log.w(TAG, "showOverlay() → package=$packageName")

        // 1. Forge the upward lifecycle path required by Compose.
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // 2. Build the ComposeView and wire all ViewTree owners BEFORE
        //    attaching to the window — wiring after attach causes a crash.
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@ComposeOverlayManager)
            setViewTreeViewModelStoreOwner(this@ComposeOverlayManager)
            setViewTreeSavedStateRegistryOwner(this@ComposeOverlayManager)

            setContent {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0A)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "STOP",
                            color = Color(0xFFE53935),
                            fontSize = 96.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "You are wasting your potential.",
                            color = Color(0xFFBDBDBD),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 3. Build WindowManager layout params.
        //    TYPE_ACCESSIBILITY_OVERLAY is the only type that sits above
        //    all other apps without requiring SYSTEM_ALERT_WINDOW.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // 4. Attach to window and track state.
        windowManager.addView(view, params)
        composeView = view
        isShowing = true

        Log.w(TAG, "Overlay attached to WindowManager.")
    }

    /**
     * Tears down the overlay and clears all references to prevent memory leaks.
     * Safe to call when no overlay is visible.
     */
    fun hideOverlay() {
        if (!isShowing) return

        Log.w(TAG, "hideOverlay() → tearing down overlay.")

        // 1. Descend the lifecycle — triggers Compose to unsubscribe from
        //    all state flows and cancel coroutines before the view detaches.
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        // 2. Detach the view from the window and null the reference.
        composeView?.let { view ->
            runCatching { windowManager.removeView(view) }
                .onFailure { Log.e(TAG, "removeView failed: ${it.message}") }
        }
        composeView = null
        isShowing = false

        Log.w(TAG, "Overlay removed from WindowManager.")
    }

    /**
     * Call this when the host Service itself is destroyed (onDestroy).
     * Clears the ViewModelStore so ViewModel instances are not leaked.
     */
    fun destroy() {
        hideOverlay()
        _viewModelStore.clear()
    }
}
