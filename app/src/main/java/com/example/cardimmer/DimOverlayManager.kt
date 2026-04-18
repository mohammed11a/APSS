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

    fun show() {
        if (dimView != null) return

        dimView = View(context).apply {
            setBackgroundColor(Color.argb((prefs.dimLevel * 255).toInt(), 0, 0, 0))
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR, // Fallback for older, though app is target 24+, mostly 26+ is TYPE_APPLICATION_OVERLAY
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager.addView(dimView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateDimLevel(level: Float) {
        val safeLevel = level.coerceIn(0f, 0.9f)
        dimView?.setBackgroundColor(Color.argb((safeLevel * 255).toInt(), 0, 0, 0))
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
