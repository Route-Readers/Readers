package com.route.readers.ui.screens.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color.White)
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
                                color = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "이전 달",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        monthState.currentMonth = monthState.currentMonth.minusMonths(1)
                                    }
                            )
                            Text(
                                text = monthState.currentMonth.format(DateTimeFormatter.ofPattern("M월", Locale.KOREAN)),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "다음 달",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        monthState.currentMonth = monthState.currentMonth.plusMonths(1)
                                    }
                            )
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
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
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
                    .background(Color(0xFFFFF7E6)),
                contentAlignment = Alignment.Center
            ) {
                when (attendanceData?.event) {
                    "leaf_1", "leaf_2" -> Icon(
                        imageVector = Icons.Filled.Book,
                        contentDescription = "특별 보상",
                        tint = Color.Green,
                        modifier = Modifier.size(20.dp)
                    )
                    else -> Icon(
                        Icons.Default.Check,
                        "출석",
                        tint = Color(0xFFF57C00),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (day.date.dayOfWeek == DayOfWeek.SUNDAY) Color.Red else Color.Black
            )
        }
    }
}
