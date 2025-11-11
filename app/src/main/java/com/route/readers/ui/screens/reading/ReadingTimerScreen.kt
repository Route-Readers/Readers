package com.route.readers.ui.screens.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.route.readers.data.model.MyBook
import kotlinx.coroutines.delay

// 전역 타이머 상태 저장
object TimerState {
    private val timerStates = mutableMapOf<String, Pair<Boolean, Int>>()
    private val completedBooks = mutableMapOf<String, Boolean>()
    
    fun getState(bookIsbn: String): Pair<Boolean, Int> {
        return timerStates[bookIsbn] ?: Pair(false, 0)
    }
    
    fun setState(bookIsbn: String, isRunning: Boolean, seconds: Int) {
        timerStates[bookIsbn] = Pair(isRunning, seconds)
    }
    
    fun setCompleted(bookIsbn: String, completed: Boolean) {
        completedBooks[bookIsbn] = completed
    }
    
    fun isCompleted(bookIsbn: String): Boolean {
        return completedBooks[bookIsbn] ?: false
    }
    
    fun clearCompleted(bookIsbn: String) {
        completedBooks.remove(bookIsbn)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingTimerScreen(
    book: MyBook,
    onNavigateBack: () -> Unit,
    onFinishReading: (Int) -> Unit
) {
    val (initialIsRunning, initialSeconds) = TimerState.getState(book.isbn)
    var isRunning by remember { mutableStateOf(initialIsRunning) }
    var seconds by remember { mutableStateOf(initialSeconds) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showBackDialog by remember { mutableStateOf(false) }

    // 타이머 상태 저장
    LaunchedEffect(isRunning, seconds) {
        TimerState.setState(book.isbn, isRunning, seconds)
    }

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
                    IconButton(onClick = { 
                        if (isRunning) {
                            showBackDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
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
                fontSize = when {
                    book.title.length > 30 -> 16.sp
                    book.title.length > 20 -> 20.sp
                    else -> 24.sp
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
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
                        CircleShape
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

    if (showBackDialog) {
        AlertDialog(
            onDismissRequest = { showBackDialog = false },
            title = { Text("타이머가 실행 중입니다") },
            text = { Text("타이머를 일시정지한 후 나가시겠습니까?") },
            confirmButton = {
                Button(onClick = {
                    isRunning = false
                    showBackDialog = false
                }) { Text("일시정지") }
            },
            dismissButton = {
                TextButton(onClick = { showBackDialog = false }) { Text("취소") }
            }
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("독서 완료") },
            text = { Text("${minutes}분 ${remainingSeconds}초 동안 독서하셨습니다.\n독서를 완료하시겠습니까?") },
            confirmButton = {
                Button(onClick = {
                    showFinishDialog = false
                    TimerState.setState(book.isbn, false, 0) // 완료 시 타이머 초기화
                    TimerState.setCompleted(book.isbn, true) // 완료 상태 설정
                    onFinishReading(seconds)
                }) { Text("완료") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("취소") }
            }
        )
    }
}
