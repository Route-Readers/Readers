package com.route.readers.ui.screens.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.route.readers.ui.theme.DarkRed
import io.github.boguszpawlowski.composecalendar.StaticCalendar
import io.github.boguszpawlowski.composecalendar.day.Day
import io.github.boguszpawlowski.composecalendar.rememberCalendarState
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onNavigateBack: () -> Unit,
    viewModel: AttendanceViewModel
) {
    val attendanceDataMap by viewModel.attendanceData.collectAsState()
    val totalAttendanceDays by viewModel.totalAttendanceDays.collectAsState()
    val totalReadingDays by viewModel.totalReadingDays.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAttendanceData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("출석 기록") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로 가기")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "총 출석일",
                            tint = Color(0xFFF57C00)
                        )
                        Text(
                            text = "$totalAttendanceDays 일",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                        )

                        Icon(
                            Icons.Filled.Book,
                            contentDescription = "총 독서일",
                            tint = Color(0xFF8B0000)
                        )
                        Text(
                            text = "$totalReadingDays 일",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val calendarState = rememberCalendarState(initialMonth = YearMonth.now())

            StaticCalendar(
                calendarState = calendarState,
                modifier = Modifier.padding(horizontal = 16.dp),
                monthHeader = { monthState ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${monthState.currentMonth.year}년",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(onClick = { monthState.currentMonth = monthState.currentMonth.minusMonths(1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "이전 달",
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Text(
                                text = monthState.currentMonth.format(DateTimeFormatter.ofPattern("M월", Locale.KOREAN)),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(onClick = { monthState.currentMonth = monthState.currentMonth.plusMonths(1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "다음 달",
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                },
                daysOfWeekHeader = { daysOfWeek ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysOfWeek.forEach { dayOfWeek ->
                            Text(
                                textAlign = TextAlign.Center,
                                text = dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.KOREAN),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                },
                dayContent = { day ->
                    val data = attendanceDataMap[day.date]
                    DayContent(day, data)
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DayContent(day: Day, attendanceData: AttendanceData?) {
    val isAttended = attendanceData != null

    if (!day.isFromCurrentMonth) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        if (isAttended) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.7f)
                    .clip(CircleShape)
                    .background(DarkRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                when (attendanceData?.event) {
                    "leaf_1", "leaf_2" -> Icon(
                        imageVector = Icons.Filled.Book,
                        contentDescription = "특별 보상",
                        tint = Color(0xFF8B0000),
                        modifier = Modifier.size(20.dp)
                    )

                    else -> if ((attendanceData?.points ?: 0) > 0) {
                        Icon(
                            Icons.Default.Check,
                            "출석",
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (day.date.dayOfWeek == DayOfWeek.SUNDAY) Color.Red else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
