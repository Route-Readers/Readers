package com.route.readers.ui.components

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.route.readers.R

@Composable
fun NativeAdComposable() {
    val context = LocalContext.current
    val nativeAdState = remember { mutableStateOf<NativeAd?>(null) }
    val adLoader = remember {
        AdLoader.Builder(context, "ca-app-pub-3940256099942544/2247696110") // Test Ad Unit ID
            .forNativeAd { ad: NativeAd ->
                nativeAdState.value = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Handle the failure by logging, hiding the ad space, etc.
                }
            })
            .build()
    }

    DisposableEffect(adLoader) {
        adLoader.loadAd(AdRequest.Builder().build())
        onDispose {
            nativeAdState.value?.destroy()
        }
    }

    nativeAdState.value?.let { nativeAd ->
        AndroidView(
            factory = {
                val adView = LayoutInflater.from(it).inflate(R.layout.native_ad_layout, null) as NativeAdView
                adView.headlineView = adView.findViewById(R.id.ad_headline)
                adView.bodyView = adView.findViewById(R.id.ad_body)
                adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                adView.iconView = adView.findViewById(R.id.ad_app_icon)
                adView.mediaView = adView.findViewById<MediaView>(R.id.ad_media)
                adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
                adView
            },
            update = { adView ->
                (adView.headlineView as? TextView)?.text = nativeAd.headline
                (adView.bodyView as? TextView)?.text = nativeAd.body
                (adView.callToActionView as? Button)?.text = nativeAd.callToAction
                (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
                adView.mediaView?.setMediaContent(nativeAd.mediaContent!!)
                (adView.advertiserView as? TextView)?.text = nativeAd.advertiser
                adView.setNativeAd(nativeAd)
            }
        )
    }
}
