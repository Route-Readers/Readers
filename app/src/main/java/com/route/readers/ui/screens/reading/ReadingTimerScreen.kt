package com.route.readers.ui.screens.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.route.readers.data.model.MyBook
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingTimerScreen(
    book: MyBook,
    onNavigateBack: () -> Unit,
    onFinishReading: (Int) -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    var seconds by remember { mutableStateOf(0) }
    var showFinishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isRunning) {
                delay(1000)
                seconds++
            }
        }
    }

    val minutes = seconds / 60
    val remainingSeconds = seconds % 60

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("독서 중") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    TextButton(onClick = { showFinishDialog = true }) {
                        Text("독서 완료")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = book.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = book.author,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "${book.currentPage}페이지",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "${book.progressPercentage}%",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "${book.totalPages}페이지까지",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "독서 중",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = String.format("%d:%02d", minutes, remainingSeconds),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "친구들의 밑줄",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👥 2개",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("김현우", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(" 42p", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("2시간 전", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(Color(0xFFFFF3CD), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "습관이 만들어지는 과정에서 가장 중요한 것은 일관성이다.",
                            fontSize = 14.sp,
                            color = Color(0xFF856404)
                        )
                    }
                    
                    Text("토론하기 (2)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        androidx.compose.foundation.shape.CircleShape
                    )
                    .clickable { isRunning = !isRunning },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "일시정지" else "시작",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("독서 완료") },
            text = { Text("${minutes}분 ${remainingSeconds}초 동안 독서하셨습니다.\n독서를 완료하시겠습니까?") },
            confirmButton = {
                Button(onClick = {
                    showFinishDialog = false
                    onFinishReading(seconds)
                }) { Text("완료") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("취소") }
            }
        )
    }
}
