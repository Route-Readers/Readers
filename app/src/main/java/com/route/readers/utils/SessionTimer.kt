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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// DataStore 인스턴스 생성
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_timer_prefs")

class SessionTimer(private val context: Context) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var startTime: Long = 0L

    companion object {
        private val TOTAL_SESSION_TIME_KEY = longPreferencesKey("total_session_time_ms")
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // 앱이 포그라운드로 전환될 때 시간 측정 시작
        startTime = System.currentTimeMillis()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // 앱이 백그라운드로 전환될 때 시간 저장
        if (startTime > 0) {
            val sessionDuration = System.currentTimeMillis() - startTime
            scope.launch {
                saveSessionTime(sessionDuration)
            }
        }
    }

    private suspend fun saveSessionTime(durationMs: Long) {
        context.dataStore.edit { preferences ->
            val currentTotal = preferences[TOTAL_SESSION_TIME_KEY] ?: 0L
            preferences[TOTAL_SESSION_TIME_KEY] = currentTotal + durationMs
        }
    }

    // 총 접속 시간을 가져오는 함수
    fun getTotalSessionTimeFlow() = context.dataStore.data
        .map { preferences ->
            preferences[TOTAL_SESSION_TIME_KEY] ?: 0L
        }

    // 밀리초를 "Xh Ym" 형태의 문자열로 변환하는 유틸리티 함수
    fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}
