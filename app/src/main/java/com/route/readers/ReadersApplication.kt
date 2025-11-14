package com.route.readers

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.route.readers.notification.FCMTokenManager
import com.route.readers.utils.SessionTimer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ReadersApplication : Application() {

    lateinit var sessionTimer: SessionTimer
        private set

    override fun onCreate() {
        super.onCreate()
        // SessionTimer 초기화 및 LifecycleObserver 등록
        sessionTimer = SessionTimer(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(sessionTimer)

        // Update FCM token on app startup in a coroutine
        GlobalScope.launch {
            FCMTokenManager.updateFCMToken()
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isAppInForeground = true
            }

            override fun onStop(owner: LifecycleOwner) {
                isAppInForeground = false
            }
        })
    }

    companion object {
        var isAppInForeground: Boolean = false
    }
}
