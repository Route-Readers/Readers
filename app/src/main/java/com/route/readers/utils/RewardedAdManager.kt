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

    private var rewardedAd: RewardedAd? = null
    private val adUnitId = "ca-app-pub-3940256099942544/5224354917" // Test Ad Unit ID

    init {
        loadAd()
    }

    private fun loadAd() {
        RewardedAd.load(context, adUnitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
        })
    }

    fun showAd(activity: Activity, onRewardEarned: () -> Unit, onAdNotReady: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { rewardItem ->
                grantToken()
                onRewardEarned()
            }
            rewardedAd = null
            loadAd()
        } else {
            onAdNotReady()
            loadAd()
        }
    }

    private fun grantToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
        
        userRef.get().addOnSuccessListener { doc ->
            if (doc.exists() && doc.contains("tokens")) {
                userRef.update("tokens", FieldValue.increment(1))
            } else {
                userRef.set(mapOf("tokens" to 1), com.google.firebase.firestore.SetOptions.merge())
            }
        }
    }

    fun isAdReady(): Boolean = rewardedAd != null
}
