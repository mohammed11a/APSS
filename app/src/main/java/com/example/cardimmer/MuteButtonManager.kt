package com.example.cardimmer

import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import kotlin.math.abs

class MuteButtonManager(
    private val context: Context,
    private val prefs: PreferencesManager
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var floatingView: ImageView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var isDragging = false
    private var isClick = false

    private val handler = Handler(Looper.getMainLooper())
    private val autoHideRunnable = Runnable {
        if (prefs.autoHide) {
            floatingView?.alpha = 0.01f
        }
    }

    fun show() {
        if (floatingView != null) return

        floatingView = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_lock_silent_mode_off)
            alpha = prefs.buttonOpacity
            setOnTouchListener(createTouchListener())
            updateIcon()
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
            x = prefs.muteButtonX
            y = prefs.muteButtonY
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
            0 -> 36f
            1 -> 48f
            else -> 64f
        }
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun createTouchListener() = View.OnTouchListener { view, event ->
        resetAutoHideTimer()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams?.x ?: 0
                initialY = layoutParams?.y ?: 0
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                isClick = true
                view.alpha = prefs.buttonOpacity
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY

                if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    isDragging = true
                    isClick = false
                }

                if (isDragging) {
                    layoutParams?.x = initialX + dx.toInt()
                    layoutParams?.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(floatingView, layoutParams)
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                if (isClick) {
                    toggleMute()
                } else if (isDragging) {
                    prefs.muteButtonX = layoutParams?.x ?: 0
                    prefs.muteButtonY = layoutParams?.y ?: 0
                }
                resetAutoHideTimer()
                true
            }
            MotionEvent.ACTION_CANCEL -> {
                resetAutoHideTimer()
                true
            }
            else -> false
        }
    }

    private fun toggleMute() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (isMuted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_SHOW_UI
            )
        } else {
            // Fallback for older APIs
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (currentVol == 0) {
                // Approximate unmute (requires storing previous volume, but let's just step up)
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            } else {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            }
        }
        updateIcon()
    }

    private fun updateIcon() {
        val isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        } else {
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        }
        
        if (isMuted) {
            floatingView?.setImageResource(android.R.drawable.ic_lock_silent_mode)
        } else {
            floatingView?.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
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
            updateIcon()
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
