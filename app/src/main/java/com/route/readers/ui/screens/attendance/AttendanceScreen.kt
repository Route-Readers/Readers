package com.route.readers.ui.screens.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
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
    val consecutiveAttendanceDays by viewModel.consecutiveAttendanceDays.collectAsState()
    val consecutiveReadingDays by viewModel.consecutiveReadingDays.collectAsState()

    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    viewModel.refreshAttendanceData()
                }
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
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
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = monthState.currentMonth.format(DateTimeFormatter.ofPattern("M월", Locale.KOREAN)),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(onClick = { monthState.currentMonth = monthState.currentMonth.plusMonths(1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "다음 달",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoItem(
                        icon = { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) },
                        label = "총 출석일",
                        value = "$totalAttendanceDays 일"
                    )
                    InfoItem(
                        icon = { Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                        label = "총 독서일",
                        value = "$totalReadingDays 일"
                    )
                    InfoItem(
                        icon = { Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFF81C784), modifier = Modifier.size(20.dp)) },
                        label = "연속 출석",
                        value = "$consecutiveAttendanceDays 일"
                    )
                    InfoItem(
                        icon = { Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFE57373), modifier = Modifier.size(20.dp)) },
                        label = "연속 독서",
                        value = "$consecutiveReadingDays 일"
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoItem(icon: @Composable () -> Unit, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DayContent(day: Day, attendanceData: AttendanceData?) {
    val isToday = day.date == java.time.LocalDate.now()
    val baseModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)

    Box(
        modifier = if (isToday) baseModifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape) else baseModifier,
        contentAlignment = Alignment.Center
    ) {
        if (!day.isFromCurrentMonth) {
            // Do nothing for days not in the current month
        } else if (attendanceData != null) {
            val hasAttendance = attendanceData.points > 0
            val hasReading = attendanceData.event != null

            Box(
                modifier = Modifier
                    .fillMaxSize(0.85f)
                    .clip(CircleShape)
                    .background(
                        when {
                            hasAttendance && hasReading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            hasAttendance -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            hasReading -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            else -> Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (hasReading) {
                    Icon(
                        imageVector = Icons.Filled.Book,
                        contentDescription = "독서 기록",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (hasAttendance) {
                    Icon(
                        Icons.Default.Check,
                        "출석",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(text = day.date.dayOfMonth.toString(), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        } else {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (day.date.dayOfWeek == DayOfWeek.SUNDAY) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
