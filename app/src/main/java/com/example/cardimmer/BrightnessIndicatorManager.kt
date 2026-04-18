package com.example.cardimmer

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView

class BrightnessIndicatorManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var indicatorView: View? = null
    private var progressBar: ProgressBar? = null
    private var textView: TextView? = null

    fun show(initialLevel: Float) {
        if (indicatorView != null) return

        indicatorView = LayoutInflater.from(context).inflate(R.layout.layout_brightness_indicator, null)
        progressBar = indicatorView?.findViewById(R.id.progressBrightness)
        textView = indicatorView?.findViewById(R.id.textBrightness)

        updateLevel(initialLevel)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            y = 200 // Offset from top
        }

        try {
            windowManager.addView(indicatorView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateLevel(level: Float) {
        val percentage = (level * 100).toInt()
        progressBar?.progress = percentage
        textView?.text = "$percentage%"
    }

    fun hide() {
        indicatorView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            indicatorView = null
            progressBar = null
            textView = null
        }
    }
}
