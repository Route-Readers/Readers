package com.route.readers.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RewardedAdManager(private val context: Context) {

    private var rewardedAd1: RewardedAd? = null  // 토큰 1개용
    private var rewardedAd3: RewardedAd? = null  // 토큰 3개용
    
    // 테스트 ID (배포 시 실제 광고 단위 ID로 교체)
    private val adUnitId1 = "ca-app-pub-3940256099942544/5224354917"
    private val adUnitId3 = "ca-app-pub-3940256099942544/5224354917"

    init {
        loadAd1()
        loadAd3()
    }

    private fun loadAd1() {
        RewardedAd.load(context, adUnitId1, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd1 = null }
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd1 = ad }
        })
    }

    private fun loadAd3() {
        RewardedAd.load(context, adUnitId3, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd3 = null }
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd3 = ad }
        })
    }

    fun showAdFor1Token(activity: Activity, onRewardEarned: () -> Unit, onAdNotReady: () -> Unit) {
        val ad = rewardedAd1
        if (ad != null) {
            ad.show(activity) { grantTokens(1); onRewardEarned() }
            rewardedAd1 = null
            loadAd1()
        } else {
            onAdNotReady()
            loadAd1()
        }
    }

    fun showAdFor3Tokens(activity: Activity, onRewardEarned: () -> Unit, onAdNotReady: () -> Unit) {
        val ad = rewardedAd3
        if (ad != null) {
            ad.show(activity) { grantTokens(3); onRewardEarned() }
            rewardedAd3 = null
            loadAd3()
        } else {
            onAdNotReady()
            loadAd3()
        }
    }

    private fun grantTokens(amount: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        
        userRef.get().addOnSuccessListener { doc ->
            if (doc.exists() && doc.contains("tokens")) {
                userRef.update("tokens", FieldValue.increment(amount.toLong()))
            } else {
                userRef.set(mapOf("tokens" to amount), com.google.firebase.firestore.SetOptions.merge())
            }
        }
    }
}
