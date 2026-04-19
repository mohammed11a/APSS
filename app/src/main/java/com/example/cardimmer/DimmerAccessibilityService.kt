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
            if (intent?.action == Intent.ACTION_TIME_TICK) {
                checkScheduleAndUpdateOverlay()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PreferencesManager(this)
        
        // Register listener for preference changes
        val sharedPreferences = getSharedPreferences("CarDimmerPrefs", MODE_PRIVATE)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        // Register time tick receiver
        val filter = IntentFilter(Intent.ACTION_TIME_TICK)
        registerReceiver(timeReceiver, filter)

        dimOverlayManager = DimOverlayManager(this, prefs)
        
        checkScheduleAndUpdateOverlay()
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
