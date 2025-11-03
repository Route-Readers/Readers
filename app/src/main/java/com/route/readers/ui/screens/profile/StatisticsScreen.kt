package com.route.readers.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

// --- 데이터 클래스 정의 (기존과 동일) ---
data class DailyStats(
    val accessTime: String,
    val totalReadingTime: String,
    val readingBookCount: Int,
    val finishedBookCount: Int
)

data class WeeklyStats(
    val accessTime: String,
    val totalReadingTime: String,
    val mostReadDay: String,
    val finishedBookCount: Int
)

data class MonthlyStats(
    val accessTime: String,
    val totalReadingTime: String,
    val mostReadWeek: String,
    val finishedBookCount: Int
)

data class YearlyStats(
    val accessTime: String,
    val totalReadingTime: String,
    val mostReadMonth: String,
    val finishedBookCount: Int
)

data class TotalStats(
    val firstAccessDate: String,
    val totalAccessDays: Int,
    val totalReadingTime: String,
    val totalFinishedBookCount: Int
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit
    // viewModel: StatisticsViewModel = viewModel()
) {
    // --- 임시 데이터 (기존과 동일, 나중에 ViewModel에서 관리) ---
    val dailyStats = DailyStats("58분", "32분", 3, 1)
    val weeklyStats = WeeklyStats("5시간 12분", "3시간 40분", "월요일", 2)
    val monthlyStats = MonthlyStats("22시간", "15시간", "3주차", 5)
    val yearlyStats = YearlyStats("210시간", "140시간", "7월", 25)
    val totalStats = TotalStats("2023년 1월 15일", 258, "500시간", 60)

    // --- 각 탭별로 현재 선택된 날짜/기간을 상태로 관리 ---
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // --- 데이터를 가져오는 로직 (현재는 임시) ---
    LaunchedEffect(selectedDate) {
        // 이 블록은 selectedDate가 바뀔 때마다 실행됩니다.
        // 여기에 데이터 로딩 로직을 추가할 수 있습니다.
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("독서 통계", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            StatisticsSection(
                selectedDate = selectedDate,
                onDateChange = { newDate -> selectedDate = newDate },
                dailyStats = dailyStats,
                weeklyStats = weeklyStats,
                monthlyStats = monthlyStats,
                yearlyStats = yearlyStats,
                totalStats = totalStats
            )
        }
    }
}

@Composable
fun StatisticsSection(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    dailyStats: DailyStats,
    weeklyStats: WeeklyStats,
    monthlyStats: MonthlyStats,
    yearlyStats: YearlyStats,
    totalStats: TotalStats
) {
    var selectedTab by remember { mutableStateOf("일") }
    val tabs = listOf("일", "주", "월", "년", "전체")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        TabRow(
            selectedTabIndex = tabs.indexOf(selectedTab),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab)]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabs.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (selectedTab) {
            "일" -> DailyStatsContent(
                date = selectedDate,
                stats = dailyStats,
                onDateChange = { onDateChange(it) }
            )
            "주" -> WeeklyStatsContent(
                date = selectedDate,
                stats = weeklyStats,
                onDateChange = { onDateChange(it) }
            )
            "월" -> MonthlyStatsContent(
                date = selectedDate,
                stats = monthlyStats,
                onDateChange = { onDateChange(it) }
            )
            "년" -> YearlyStatsContent(
                date = selectedDate,
                stats = yearlyStats,
                onDateChange = { onDateChange(it) }
            )
            "전체" -> TotalStatsContent(stats = totalStats)
        }
    }
}

// --- 각 탭에 대한 컨텐츠 Composable ---

@Composable
fun DailyStatsContent(date: LocalDate, stats: DailyStats, onDateChange: (LocalDate) -> Unit) {
    val currentYear = LocalDate.now().year
    val isDifferentYear = date.year != currentYear

    val formatter = if (isDifferentYear) {
        DateTimeFormatter.ofPattern("MM월 dd일")
    } else {
        DateTimeFormatter.ofPattern("MM월 dd일")
    }
    val formattedDate = date.format(formatter)
    val subTitle = if (isDifferentYear) "${date.year}년" else null

    StatsHeader(
        title = formattedDate,
        subTitle = subTitle,
        onPrevious = { onDateChange(date.minusDays(1)) },
        onNext = { onDateChange(date.plusDays(1)) }
    )
    Spacer(modifier = Modifier.height(24.dp))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsDetailRow("접속시간", stats.accessTime)
        StatisticsDetailRow("총 독서시간", stats.totalReadingTime)
        StatisticsDetailRow("읽는중인 책", "${stats.readingBookCount}권")
        StatisticsDetailRow("완독한 책", "${stats.finishedBookCount}권")
    }
}

@Composable
fun WeeklyStatsContent(date: LocalDate, stats: WeeklyStats, onDateChange: (LocalDate) -> Unit) {
    val currentYear = LocalDate.now().year
    val isDifferentYear = date.year != currentYear

    val weekFields = WeekFields.of(Locale.getDefault())
    val weekOfMonth = date.get(weekFields.weekOfMonth())
    val month = date.monthValue
    val title = "${month}월 ${weekOfMonth}주차"
    val subTitle = if (isDifferentYear) "${date.year}년" else null

    StatsHeader(
        title = title,
        subTitle = subTitle,
        onPrevious = { onDateChange(date.minusWeeks(1)) },
        onNext = { onDateChange(date.plusWeeks(1)) }
    )
    Spacer(modifier = Modifier.height(24.dp))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsDetailRow("접속시간", stats.accessTime)
        StatisticsDetailRow("총 독서시간", stats.totalReadingTime)
        StatisticsDetailRow("가장 많이 읽은 요일", stats.mostReadDay)
        StatisticsDetailRow("완독한 책", "${stats.finishedBookCount}권")
    }
}

@Composable
fun MonthlyStatsContent(date: LocalDate, stats: MonthlyStats, onDateChange: (LocalDate) -> Unit) {
    val currentYear = LocalDate.now().year
    val isDifferentYear = date.year != currentYear
    val yearMonth = YearMonth.from(date)
    val title = "${yearMonth.monthValue}월"
    val subTitle = if (isDifferentYear) "${date.year}년" else null

    StatsHeader(
        title = title,
        subTitle = subTitle,
        onPrevious = { onDateChange(date.minusMonths(1)) },
        onNext = { onDateChange(date.plusMonths(1)) }
    )
    Spacer(modifier = Modifier.height(24.dp))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsDetailRow("접속시간", stats.accessTime)
        StatisticsDetailRow("총 독서시간", stats.totalReadingTime)
        StatisticsDetailRow("가장 많이 읽은 주", stats.mostReadWeek)
        StatisticsDetailRow("완독한 책", "${stats.finishedBookCount}권")
    }
}

@Composable
fun YearlyStatsContent(date: LocalDate, stats: YearlyStats, onDateChange: (LocalDate) -> Unit) {
    val title = "${date.year}년"

    // '년' 탭에서는 항상 연도가 표시되므로 subTitle이 필요 없습니다.
    StatsHeader(
        title = title,
        onPrevious = { onDateChange(date.minusYears(1)) },
        onNext = { onDateChange(date.plusYears(1)) }
    )
    Spacer(modifier = Modifier.height(24.dp))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsDetailRow("접속시간", stats.accessTime)
        StatisticsDetailRow("총 독서시간", stats.totalReadingTime)
        StatisticsDetailRow("가장 많이 읽은 달", stats.mostReadMonth)
        StatisticsDetailRow("완독한 책", "${stats.finishedBookCount}권")
    }
}

@Composable
fun TotalStatsContent(stats: TotalStats) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsDetailRow("최초 접속일", stats.firstAccessDate)
        StatisticsDetailRow("총 접속일", "${stats.totalAccessDays}일")
        StatisticsDetailRow("총 독서시간", stats.totalReadingTime)
        StatisticsDetailRow("총 완독 권수", "${stats.totalFinishedBookCount}권")
    }
}


// --- 재사용 가능한 Composable ---

@Composable
fun StatsHeader(
    title: String,
    subTitle: String? = null, // subTitle 파라미터 추가 (선택적)
    onPrevious: (() -> Unit)? = null, // 이전/다음이 없는 경우를 위해 nullable로 변경
    onNext: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (onPrevious != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "이전",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onPrevious)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (subTitle != null) {
                Text(
                    text = subTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        if (onNext != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "다음",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onNext)
            )
        }
    }
}

@Composable
fun StatisticsDetailRow(title: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, fontWeight = FontWeight.SemiBold, color = valueColor, style = MaterialTheme.typography.bodyLarge)
    }
}
