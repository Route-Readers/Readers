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
                
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                
                val inputStream: InputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                connection.disconnect()
                
                // 위젯용으로 크기 조정 (48x64dp)
                val density = context.resources.displayMetrics.density
                val targetWidth = (48 * density).toInt()
                val targetHeight = (64 * density).toInt()
                
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            } catch (e: Exception) {
                Log.e("WidgetImageLoader", "Failed to load image: ${e.message}")
                null
            }
        }
    }
}
