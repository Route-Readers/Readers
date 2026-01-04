package com.route.readers

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.MobileAds
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.notification.FCMTokenManager
import com.route.readers.utils.SessionTimer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

        // 프로필 리셋 마이그레이션 (한 번만 실행)
        runProfileResetMigration()
    }

    private fun runProfileResetMigration() {
        val prefs = getSharedPreferences("app_migrations", Context.MODE_PRIVATE)
        if (prefs.getBoolean("profile_reset_v1_done", false)) return

        GlobalScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val users = db.collection("users").get().await()
                
                for (doc in users.documents) {
                    doc.reference.update(
                        mapOf(
                            "profileCharacter" to null,
                            "profileBackgroundColor" to null
                        )
                    )
                }
                
                prefs.edit().putBoolean("profile_reset_v1_done", true).apply()
                android.util.Log.d("Migration", "Profile reset migration completed")
            } catch (e: Exception) {
                android.util.Log.e("Migration", "Profile reset migration failed", e)
            }
        }
    }
}
