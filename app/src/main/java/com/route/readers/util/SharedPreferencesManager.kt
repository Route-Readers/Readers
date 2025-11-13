package com.route.readers.util

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesManager {
    private const val PREF_NAME = "readers_prefs"
    private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    private const val KEY_NOTIFICATION_HOUR = "notification_hour"
    private const val KEY_NOTIFICATION_MINUTE = "notification_minute"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setNotificationEnabled(context: Context, isEnabled: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_NOTIFICATION_ENABLED, isEnabled).apply()
    }

    fun isNotificationEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_NOTIFICATION_ENABLED, false)
    }

    fun setNotificationTime(context: Context, hour: Int, minute: Int) {
        getPreferences(context).edit()
            .putInt(KEY_NOTIFICATION_HOUR, hour)
            .putInt(KEY_NOTIFICATION_MINUTE, minute)
            .apply()
    }

    fun getNotificationTime(context: Context): Pair<Int, Int> {
        val prefs = getPreferences(context)
        val hour = prefs.getInt(KEY_NOTIFICATION_HOUR, 20) // Default to 20:00
        val minute = prefs.getInt(KEY_NOTIFICATION_MINUTE, 0)
        return Pair(hour, minute)
    }
}
