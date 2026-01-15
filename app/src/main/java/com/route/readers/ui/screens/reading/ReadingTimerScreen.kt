package com.route.readers.ui.screens.reading

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.route.readers.data.model.MyBook
import com.route.readers.data.model.ReadingSession
import com.route.readers.data.remote.FirestoreRepository
import com.route.readers.ui.theme.PrimaryRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.UUID

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
    onFinishReading: (Int) -> Unit,
    onDisposeReading: (Int) -> Unit,
    startNow: Boolean = false
) {
    val firestoreRepository = remember { FirestoreRepository() }
    val coroutineScope = rememberCoroutineScope()
    val (initialIsRunning, initialSeconds) = TimerState.getState(book.isbn)
    var isRunning by remember { mutableStateOf(startNow || initialIsRunning) }
    var seconds by remember { mutableStateOf(initialSeconds) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showBackDialog by remember { mutableStateOf(false) }
    var sessionStartTime by remember { mutableStateOf<Date?>(null) }

    // 타이머 시작 시 세션 시작 시간 기록
    LaunchedEffect(isRunning) {
        if (isRunning && sessionStartTime == null) {
            sessionStartTime = Date()
        }
        if (isRunning) {
            while (isRunning) {
                delay(1000)
                seconds++
            }
        }
    }

    // 타이머 상태 저장
    LaunchedEffect(isRunning, seconds) {
        TimerState.setState(book.isbn, isRunning, seconds)
    }

    // 시스템 뒤로가기 버튼 처리
    BackHandler {
        if (isRunning || seconds > 0) {
            showFinishDialog = true
        } else {
            onNavigateBack()
        }
    }

    // 화면이 사라질 때 독서 시간을 저장
    DisposableEffect(Unit) {
        onDispose {
            if (seconds > 0) {
                coroutineScope.launch {
                    saveReadingSession(firestoreRepository, book.isbn, sessionStartTime, seconds)
                }
                onDisposeReading(seconds)
            }
        }
    }

    val minutes = seconds / 60
    val remainingSeconds = seconds % 60

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isRunning || seconds > 0) {
                            showFinishDialog = true
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
                TextButton(onClick = { showFinishDialog = true }) {
                    Text(
                        text = "독서 종료",
                        color = PrimaryRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Book Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                Text(
                    text = book.title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 34.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = book.author,
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Central Circular Timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(300.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "breathing")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (isRunning) 1.05f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = if (isRunning) 0.12f else 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                // Breathing Glow
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .scale(pulseScale)
                            .background(
                                color = PrimaryRed.copy(alpha = pulseAlpha),
                                shape = CircleShape
                            )
                    )
                }

                // Progress Arc
                val progress = book.progressPercentage / 100f
                Canvas(modifier = Modifier.size(260.dp)) {
                    // Track
                    drawCircle(
                        color = Color(0xFFF5F5F5),
                        style = Stroke(width = 12.dp.toPx())
                    )

                    // Progress
                    drawArc(
                        color = PrimaryRed,
                        startAngle = -90f,
                        sweepAngle = 360 * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Timer Text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format("%02d:%02d", minutes, remainingSeconds),
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif,
                        color = PrimaryRed,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${book.progressPercentage}% 완료됨",
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Play/Pause Button
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        spotColor = PrimaryRed.copy(alpha = 0.25f),
                        ambientColor = PrimaryRed.copy(alpha = 0.15f)
                    )
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { isRunning = !isRunning },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "일시정지" else "시작",
                    tint = PrimaryRed,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { 
                Text(
                    "독서 세션 종료", 
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default
                ) 
            },
            text = { 
                Text(
                    "약 ${minutes}분 동안 독서하셨습니다.\n이 기록을 저장하고 종료하시겠습니까?",
                    fontFamily = FontFamily.Default
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        TimerState.setState(book.isbn, false, 0)
                        onFinishReading(seconds)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(10.dp)
                ) { 
                    Text("저장 및 종료", fontWeight = FontWeight.Bold) 
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("취소", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// 독서 세션을 Firestore에 저장하는 함수
private suspend fun saveReadingSession(
    firestoreRepository: FirestoreRepository,
    bookId: String,
    startTime: Date?,
    durationInSeconds: Int
) {
    if (durationInSeconds > 0 && startTime != null) {
        val calendar = Calendar.getInstance()
        calendar.time = startTime

        val session = ReadingSession(
            sessionId = UUID.randomUUID().toString(),
            bookId = bookId,
            startTime = startTime,
            endTime = Date(),
            durationInSeconds = durationInSeconds,
            pagesRead = 0,
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
            weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
        )

        firestoreRepository.addReadingSession(session)
    }
}
