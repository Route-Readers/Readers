package com.route.readers.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

// --- 데이터 클래스 정의 ---
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
    // --- 임시 데이터 (나중에 ViewModel에서 관리) ---
    val dailyStats = DailyStats("58분", "32분", 3, 1)
    val weeklyStats = WeeklyStats("5시간 12분", "3시간 40분", "월요일", 2)
    val monthlyStats = MonthlyStats("22시간", "15시간", "3주차", 5)
    val yearlyStats = YearlyStats("210시간", "140시간", "7월", 25)
    val totalStats = TotalStats("2023년 1월 15일", 258, "500시간", 60)

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
            "일" -> DailyStatsContent(stats = dailyStats)
            "주" -> WeeklyStatsContent(stats = weeklyStats)
            "월" -> MonthlyStatsContent(stats = monthlyStats)
            "년" -> YearlyStatsContent(stats = yearlyStats)
            "전체" -> TotalStatsContent(stats = totalStats)
        }
    }
}

// --- 각 탭에 대한 컨텐츠 Composable ---

@Composable
fun DailyStatsContent(stats: DailyStats) {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("MM월 dd일")
    val formattedDate = today.format(formatter)

    StatsHeader(title = formattedDate)
    Spacer(modifier = Modifier.height(24.dp))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsDetailRow("접속시간", stats.accessTime)
        StatisticsDetailRow("총 독서시간", stats.totalReadingTime)
        StatisticsDetailRow("읽는중인 책", "${stats.readingBookCount}권")
        StatisticsDetailRow("완독한 책", "${stats.finishedBookCount}권")
    }
}

@Composable
fun WeeklyStatsContent(stats: WeeklyStats) {
    val today = LocalDate.now()
    val weekFields = WeekFields.of(Locale.getDefault())
    val weekOfMonth = today.get(weekFields.weekOfMonth())
    val month = today.monthValue
    val title = "${month}월 ${weekOfMonth}주차"

    StatsHeader(title = title)
    Spacer(modifier = Modifier.height(24.dp))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsDetailRow("접속시간", stats.accessTime)
        StatisticsDetailRow("총 독서시간", stats.totalReadingTime)
        StatisticsDetailRow("가장 많이 읽은 요일", stats.mostReadDay)
        StatisticsDetailRow("완독한 책", "${stats.finishedBookCount}권")
    }
}

@Composable
fun MonthlyStatsContent(stats: MonthlyStats) {
    val today = YearMonth.now()
    val title = "${today.monthValue}월"

    StatsHeader(title = title)
    Spacer(modifier = Modifier.height(24.dp))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsDetailRow("접속시간", stats.accessTime)
        StatisticsDetailRow("총 독서시간", stats.totalReadingTime)
        StatisticsDetailRow("가장 많이 읽은 주", stats.mostReadWeek)
        StatisticsDetailRow("완독한 책", "${stats.finishedBookCount}권")
    }
}

@Composable
fun YearlyStatsContent(stats: YearlyStats) {
    val today = LocalDate.now()
    val title = "${today.year}년"

    StatsHeader(title = title)
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
fun StatsHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "이전",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "다음",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
