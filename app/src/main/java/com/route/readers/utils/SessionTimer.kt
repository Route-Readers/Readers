package com.route.readers.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_timer_prefs")

class SessionTimer(private val context: Context) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var startTime: Long = 0L

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        startTime = System.currentTimeMillis()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (startTime > 0) {
            val sessionDuration = System.currentTimeMillis() - startTime
            if (sessionDuration > 1000) {
                scope.launch {
                    saveSessionTime(sessionDuration)
                }
            }
            startTime = 0L
        }
    }

    private suspend fun saveSessionTime(durationMs: Long) {
        val now = Date()
        val calendar = Calendar.getInstance()
        val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val dailyTotalKey = longPreferencesKey(todayDateStr)
        val hourlyKey = longPreferencesKey("${todayDateStr}-h${currentHour}")

        context.dataStore.edit { preferences ->
            val currentDailyTotal = preferences[dailyTotalKey] ?: 0L
            preferences[dailyTotalKey] = currentDailyTotal + durationMs

            val currentHourlyTotal = preferences[hourlyKey] ?: 0L
            preferences[hourlyKey] = currentHourlyTotal + durationMs
        }
    }

    fun getRecentSessionTimes(days: Int): Flow<Map<String, Long>> {
        return context.dataStore.data.map { preferences ->
            val recentData = mutableMapOf<String, Long>()
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            repeat(days) {
                val dateStr = dateFormat.format(calendar.time)
                val key = longPreferencesKey(dateStr)
                recentData[dateStr] = preferences[key] ?: 0L
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            recentData
        }
    }

    fun getHourlySessionTimes(date: LocalDate): Flow<Map<Int, Long>> {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return context.dataStore.data.map { preferences ->
            val hourlyData = mutableMapOf<Int, Long>()
            for (hour in 0..23) {
                val key = longPreferencesKey("${dateStr}-h${hour}")
                hourlyData[hour] = preferences[key] ?: 0L
            }
            hourlyData
        }
    }

    fun getTotalSessionTimeFlow(): Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences.asMap().filterKeys {
                !it.name.contains("-h")
            }.values.filterIsInstance<Long>().sum()
        }

    fun formatDuration(millis: Long): String {
        if (millis < 0) return "0m"
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours)

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
