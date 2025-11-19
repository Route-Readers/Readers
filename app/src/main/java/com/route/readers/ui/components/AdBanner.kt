package com.route.readers.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBanner() {
    AndroidView(
        factory = { context: Context ->
            AdView(context).apply {
                val adaptiveAdSize = AdSize.getPortraitAnchoredAdaptiveBannerAdSize(context, 300)
                this.setAdSize(adaptiveAdSize)
                adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test Ad Unit ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
