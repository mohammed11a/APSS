package com.example.cardimmer

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent
import java.util.Calendar

class DimmerAccessibilityService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefs: PreferencesManager
    private var dimOverlayManager: DimOverlayManager? = null

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == Intent.ACTION_TIME_TICK) {
                checkScheduleAndUpdateOverlay()
                ensureOverlayServiceRunning()
            } else if (action == Intent.ACTION_SCREEN_ON) {
                // Instantly wake up the floating buttons when car screen turns on
                ensureOverlayServiceRunning()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PreferencesManager(this)
        
        // Register listener for preference changes
        val sharedPreferences = getSharedPreferences("CarDimmerPrefs", MODE_PRIVATE)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        // Register time tick and screen on receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(timeReceiver, filter)

        dimOverlayManager = DimOverlayManager(this, prefs)
        
        checkScheduleAndUpdateOverlay()
        ensureOverlayServiceRunning()
    }

    private fun ensureOverlayServiceRunning() {
        // Advanced Watchdog: Ensure OverlayService (floating buttons) is running
        // Since AccessibilityService is guaranteed to start/stay alive by Android, we use it to wake up the rest of the app!
        if (prefs.autoStart && (prefs.isEnabled || prefs.isMuteEnabled)) {
            try {
                val serviceIntent = Intent(this, OverlayService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used, we only need the service for creating trusted overlays
    }

    override fun onInterrupt() {
        // Not used
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "is_enabled", "is_schedule_enabled", "schedule_start_hour", "schedule_end_hour" -> {
                checkScheduleAndUpdateOverlay()
            }
            "dim_level" -> {
                if (prefs.isEnabled) {
                    dimOverlayManager?.updateDimLevel(prefs.dimLevel)
                }
            }
        }
    }

    private fun checkScheduleAndUpdateOverlay() {
        if (!prefs.isEnabled) {
            dimOverlayManager?.hide()
            return
        }

        if (prefs.isScheduleEnabled) {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

            val start = prefs.scheduleStartHour
            val end = prefs.scheduleEndHour

            val isDaytime = if (start < end) {
                currentHour in start until end
            } else {
                currentHour >= start || currentHour < end
            }

            if (isDaytime) {
                dimOverlayManager?.hide()
            } else {
                dimOverlayManager?.show()
                dimOverlayManager?.updateDimLevel(prefs.dimLevel)
            }
        } else {
            dimOverlayManager?.show()
            dimOverlayManager?.updateDimLevel(prefs.dimLevel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val sharedPreferences = getSharedPreferences("CarDimmerPrefs", MODE_PRIVATE)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        unregisterReceiver(timeReceiver)
        dimOverlayManager?.hide()
    }
}
