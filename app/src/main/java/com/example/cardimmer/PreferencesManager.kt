package com.example.cardimmer

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "CarDimmerPrefs"
        private const val KEY_IS_ENABLED = "is_enabled"
        private const val KEY_DIM_LEVEL = "dim_level"
        private const val KEY_BUTTON_SIZE = "button_size"
        private const val KEY_BUTTON_OPACITY = "button_opacity"
        private const val KEY_AUTO_HIDE = "auto_hide"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_BUTTON_X = "button_x"
        private const val KEY_BUTTON_Y = "button_y"
    }

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ENABLED, value).apply()

    var dimLevel: Float
        get() = prefs.getFloat(KEY_DIM_LEVEL, 0.5f) // 0.0 to 0.9
        set(value) {
            val clamped = value.coerceIn(0f, 0.9f)
            prefs.edit().putFloat(KEY_DIM_LEVEL, clamped).apply()
        }

    var buttonSize: Int
        get() = prefs.getInt(KEY_BUTTON_SIZE, 1) // 0: Small, 1: Medium, 2: Large
        set(value) = prefs.edit().putInt(KEY_BUTTON_SIZE, value).apply()

    var buttonOpacity: Float
        get() = prefs.getFloat(KEY_BUTTON_OPACITY, 0.8f)
        set(value) = prefs.edit().putFloat(KEY_BUTTON_OPACITY, value).apply()

    var autoHide: Boolean
        get() = prefs.getBoolean(KEY_AUTO_HIDE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_HIDE, value).apply()

    var autoStart: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START, value).apply()

    var buttonX: Int
        get() = prefs.getInt(KEY_BUTTON_X, 100)
        set(value) = prefs.edit().putInt(KEY_BUTTON_X, value).apply()

    var buttonY: Int
        get() = prefs.getInt(KEY_BUTTON_Y, 100)
        set(value) = prefs.edit().putInt(KEY_BUTTON_Y, value).apply()

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
