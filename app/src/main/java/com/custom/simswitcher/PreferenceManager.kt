package com.custom.simswitcher

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sim_switcher_prefs", Context.MODE_PRIVATE)

    var sim1Mode: Int
        get() = prefs.getInt(KEY_SIM1_MODE, 11) // Default: 11 (LTE Only / 4G)
        set(value) = prefs.edit().putInt(KEY_SIM1_MODE, value).apply()

    var sim2Mode: Int
        get() = prefs.getInt(KEY_SIM2_MODE, 9) // Default: 9 (LTE/3G/2G Auto)
        set(value) = prefs.edit().putInt(KEY_SIM2_MODE, value).apply()

    var sim1DurationMinutes: Int
        get() = prefs.getInt(KEY_SIM1_DURATION, 15) // Default: 15 mins
        set(value) = prefs.edit().putInt(KEY_SIM1_DURATION, value).apply()

    var sim2DurationMinutes: Int
        get() = prefs.getInt(KEY_SIM2_DURATION, 3) // Default: 3 mins
        set(value) = prefs.edit().putInt(KEY_SIM2_DURATION, value).apply()

    var toggleAirplaneMode: Boolean
        get() = prefs.getBoolean(KEY_TOGGLE_AIRPLANE, true)
        set(value) = prefs.edit().putBoolean(KEY_TOGGLE_AIRPLANE, value).apply()

    var autoRotationEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_ROTATION, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_ROTATION, value).apply()

    var currentPhase: Int
        get() = prefs.getInt(KEY_CURRENT_PHASE, 1) // 1 = SIM1 4G, 2 = SIM2 3G/4G
        set(value) = prefs.edit().putInt(KEY_CURRENT_PHASE, value).apply()

    var isServiceRunning: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_RUNNING, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_RUNNING, value).apply()

    var useShizuku: Boolean
        get() = prefs.getBoolean(KEY_USE_SHIZUKU, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_SHIZUKU, value).apply()

    companion object {
        private const val KEY_SIM1_MODE = "sim1_mode"
        private const val KEY_SIM2_MODE = "sim2_mode"
        private const val KEY_SIM1_DURATION = "sim1_duration_min"
        private const val KEY_SIM2_DURATION = "sim2_duration_min"
        private const val KEY_TOGGLE_AIRPLANE = "toggle_airplane"
        private const val KEY_AUTO_ROTATION = "auto_rotation"
        private const val KEY_CURRENT_PHASE = "current_phase"
        private const val KEY_SERVICE_RUNNING = "service_running"
        private const val KEY_USE_SHIZUKU = "use_shizuku"
    }
}
