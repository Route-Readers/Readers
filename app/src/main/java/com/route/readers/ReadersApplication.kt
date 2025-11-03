package com.route.readers

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.route.readers.utils.SessionTimer

class ReadersApplication : Application() {

    lateinit var sessionTimer: SessionTimer
        private set

    override fun onCreate() {
        super.onCreate()
        // SessionTimer 초기화 및 LifecycleObserver 등록
        sessionTimer = SessionTimer(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(sessionTimer)
    }
}
