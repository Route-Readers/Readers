package com.route.readers.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.route.readers.notification.ReadingNotificationManager
import com.route.readers.notification.ReadingNotificationService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTestScreen() {
    val context = LocalContext.current
    val notificationManager = remember { ReadingNotificationManager(context) }
    var permissionStatus by remember { mutableStateOf("확인 중...") }
    
    LaunchedEffect(Unit) {
        permissionStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                == PackageManager.PERMISSION_GRANTED) {
                "✅ 알림 권한 허용됨"
            } else {
                "❌ 알림 권한 필요 (설정에서 허용해주세요)"
            }
        } else {
            "✅ 알림 권한 불필요 (Android 12 이하)"
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📚 독서 알림 테스트",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = permissionStatus,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "알림 기능 테스트",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Button(
                    onClick = { 
                        try {
                            notificationManager.showReadingReminder()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text("🔔 즉시 알림 보내기")
                }
                
                Button(
                    onClick = {
                        try {
                            val serviceIntent = Intent(context, ReadingNotificationService::class.java)
                            context.startService(serviceIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text("⏰ 10초마다 알림 시작")
                }
                
                Button(
                    onClick = {
                        try {
                            val serviceIntent = Intent(context, ReadingNotificationService::class.java)
                            context.stopService(serviceIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("⏹️ 알림 중지")
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💡 문제 해결 방법:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "1. 설정 > 앱 > Readers > 알림 허용\n2. 배터리 최적화 해제\n3. 백그라운드 앱 새로고침 허용",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "알림 메시지 예시:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val messages = listOf(
                    "📚 돌아와서 책을 함께 읽어봐요!",
                    "🌹 하루라도 책을 읽지 않으면 입안에 가시가 돋칠거에요ㅜㅜ"
                )
                
                messages.forEach { message ->
                    Text(
                        text = "• $message",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
