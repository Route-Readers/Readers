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
        modifier = Modifier.fillMaxWidth(),
        factory = { context: Context ->
            AdView(context).apply {
                // AdSize는 AdView에 직접 설정하는 것이 일반적입니다.
                // 이 예제에서는 BANNER 크기를 사용합니다.
                this.setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test Ad Unit ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
