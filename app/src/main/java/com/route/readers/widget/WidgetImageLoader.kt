package com.route.readers.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object WidgetImageLoader {
    
    suspend fun loadBitmap(context: Context, imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                if (imageUrl.isEmpty()) return@withContext null
                
                Log.d("WidgetImageLoader", "Loading image: $imageUrl")
                
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                
                val inputStream: InputStream = connection.inputStream
                
                // BitmapFactory 옵션 설정으로 품질 개선
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888 // 고품질 설정
                    inDither = false
                    inScaled = false
                }
                
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                connection.disconnect()
                
                bitmap?.let {
                    // 위젯용으로 크기 조정 (고품질 스케일링)
                    val density = context.resources.displayMetrics.density
                    val targetWidth = (48 * density).toInt()
                    val targetHeight = (64 * density).toInt()
                    
                    Bitmap.createScaledBitmap(it, targetWidth, targetHeight, true)
                }
            } catch (e: Exception) {
                Log.e("WidgetImageLoader", "Failed to load image: ${e.message}")
                null
            }
        }
    }
}
