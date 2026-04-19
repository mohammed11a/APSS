package com.example.cardimmer

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import kotlin.math.abs

class FloatingButtonManager(
    private val context: Context,
    private val prefs: PreferencesManager
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var floatingView: ImageView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val brightnessIndicatorManager = BrightnessIndicatorManager(context)

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var isDimMode = false
    private var initialDimLevel = 0f

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop * 3 // Increased threshold for car screens
    private var isDragging = false
    private var hasLongPressed = false

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            if (!isDragging) {
                hasLongPressed = true
                isDimMode = true
                initialDimLevel = prefs.dimLevel
                // Visual feedback for entering dim mode
                floatingView?.scaleX = 1.2f
                floatingView?.scaleY = 1.2f
                brightnessIndicatorManager.show(initialDimLevel)
            }
        }
    })

    private val handler = Handler(Looper.getMainLooper())
    private val autoHideRunnable = Runnable {
        if (prefs.autoHide) {
            floatingView?.alpha = 0.01f // Hide completely but keep it touchable
        }
    }

    fun show() {
        if (floatingView != null) return

        floatingView = ImageView(context).apply {
            setImageResource(R.drawable.ic_floating_btn)
            alpha = prefs.buttonOpacity
            setOnTouchListener(createTouchListener())
        }

        val sizePx = getButtonSizePx()

        layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.buttonX
            y = prefs.buttonY
        }

        try {
            windowManager.addView(floatingView, layoutParams)
            resetAutoHideTimer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getButtonSizePx(): Int {
        val dp = when (prefs.buttonSize) {
            0 -> 36f // Small
            1 -> 48f // Medium
            else -> 64f // Large
        }
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun createTouchListener() = View.OnTouchListener { view, event ->
        resetAutoHideTimer()

        gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams?.x ?: 0
                initialY = layoutParams?.y ?: 0
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                hasLongPressed = false
                isDimMode = false

                view.alpha = prefs.buttonOpacity
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY

                if (!prefs.isLocked && !isDragging && !hasLongPressed && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    isDragging = true
                }

                if (isDimMode) {
                    // Slide up to decrease dim (clearer), slide down to increase dim (darker)
                    // Let's say 500px is full range
                    val deltaDim = dy / 500f
                    var newDim = initialDimLevel + deltaDim
                    newDim = newDim.coerceIn(0f, 0.9f)
                    prefs.dimLevel = newDim
                    brightnessIndicatorManager.updateLevel(newDim)
                } else if (isDragging) {
                    layoutParams?.x = initialX + dx.toInt()
                    layoutParams?.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(floatingView, layoutParams)
                }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDimMode) {
                    // Exit dim mode
                    isDimMode = false
                    view.scaleX = 1.0f
                    view.scaleY = 1.0f
                    brightnessIndicatorManager.hide()
                } else if (isDragging) {
                    // Save new position
                    prefs.buttonX = layoutParams?.x ?: 0
                    prefs.buttonY = layoutParams?.y ?: 0
                }
                
                resetAutoHideTimer()
                true
            }
            else -> false
        }
    }

    private fun resetAutoHideTimer() {
        handler.removeCallbacks(autoHideRunnable)
        floatingView?.alpha = prefs.buttonOpacity
        if (prefs.autoHide) {
            handler.postDelayed(autoHideRunnable, 3000)
        }
    }

    fun updateSettings() {
        if (floatingView != null) {
            val sizePx = getButtonSizePx()
            layoutParams?.width = sizePx
            layoutParams?.height = sizePx
            windowManager.updateViewLayout(floatingView, layoutParams)
            resetAutoHideTimer()
        }
    }

    fun hide() {
        handler.removeCallbacks(autoHideRunnable)
        floatingView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            floatingView = null
        }
    }
}
