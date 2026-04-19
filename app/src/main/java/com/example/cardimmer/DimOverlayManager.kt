package com.example.cardimmer

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager

class DimOverlayManager(private val context: Context, private val prefs: PreferencesManager) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var dimView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    fun show() {
        if (dimView != null) return

        dimView = View(context).apply {
            setBackgroundColor(Color.BLACK)
        }

        val safeLevel = prefs.dimLevel.coerceIn(0f, 0.8f)

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (context is android.accessibilityservice.AccessibilityService)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            alpha = safeLevel
        }

        try {
            windowManager.addView(dimView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateDimLevel(level: Float) {
        val safeLevel = level.coerceIn(0f, 0.8f)
        layoutParams?.alpha = safeLevel
        if (dimView != null && layoutParams != null) {
            try {
                windowManager.updateViewLayout(dimView, layoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        prefs.dimLevel = safeLevel
    }

    fun hide() {
        dimView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            dimView = null
        }
    }
}
