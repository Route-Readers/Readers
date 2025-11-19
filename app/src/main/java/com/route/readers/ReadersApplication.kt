package com.route.readers

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.MobileAds
import com.route.readers.notification.FCMTokenManager
import com.route.readers.utils.SessionTimer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ReadersApplication : Application() {

    lateinit var sessionTimer: SessionTimer
        private set

    override fun onCreate() {
        super.onCreate()
        // Initialize Mobile Ads SDK
        MobileAds.initialize(this)

        // SessionTimer 초기화 및 LifecycleObserver 등록
        sessionTimer = SessionTimer(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(sessionTimer)

        // Update FCM token on app startup in a coroutine
        GlobalScope.launch {
            FCMTokenManager.updateFCMToken()
        }
    }
}
