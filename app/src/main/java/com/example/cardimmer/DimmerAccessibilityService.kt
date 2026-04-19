package com.example.cardimmer

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent

class DimmerAccessibilityService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefs: PreferencesManager
    private var dimOverlayManager: DimOverlayManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PreferencesManager(this)
        
        // Register listener for preference changes
        val sharedPreferences = getSharedPreferences("CarDimmerPrefs", MODE_PRIVATE)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        dimOverlayManager = DimOverlayManager(this, prefs)
        
        if (prefs.isEnabled) {
            dimOverlayManager?.show()
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
            "is_enabled" -> {
                if (prefs.isEnabled) {
                    dimOverlayManager?.show()
                } else {
                    dimOverlayManager?.hide()
                }
            }
            "dim_level" -> {
                if (prefs.isEnabled) {
                    dimOverlayManager?.updateDimLevel(prefs.dimLevel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val sharedPreferences = getSharedPreferences("CarDimmerPrefs", MODE_PRIVATE)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        dimOverlayManager?.hide()
    }
}
