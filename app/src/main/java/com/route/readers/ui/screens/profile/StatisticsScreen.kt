package com.route.readers.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.common.model.PlotType
import co.yml.charts.ui.barchart.BarChart
import co.yml.charts.ui.barchart.models.BarChartData
import co.yml.charts.ui.barchart.models.BarData
import co.yml.charts.ui.barchart.models.BarStyle
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import co.yml.charts.ui.piechart.charts.DonutPieChart
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.ui.piechart.models.PieChartData
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                .verticalScroll(rememberScrollState())
        ) {
            StatisticsSection(
                uiState = uiState,
                onDateChange = viewModel::onDateChange,
                onTabChange = viewModel::onTabChange
            )
        }
    }
}

@Composable
fun StatisticsSection(
    uiState: StatisticsUiState,
    onDateChange: (LocalDate) -> Unit,
    onTabChange: (String) -> Unit
) {
    val tabs = listOf("일", "주", "월", "년", "전체")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            TabRow(
                selectedTabIndex = tabs.indexOf(uiState.selectedTab),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    if (tabs.indexOf(uiState.selectedTab) in tabPositions.indices) {
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(uiState.selectedTab)]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onTabChange(tab) },
                        text = { Text(tab) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.selectedTab) {
                "일" -> DailyStatsContent(date = uiState.selectedDate, stats = uiState.dailyStats, chartData = uiState.chartData, onDateChange = onDateChange)
                "주" -> WeeklyStatsContent(date = uiState.selectedDate, stats = uiState.weeklyStats, chartData = uiState.chartData, onDateChange = onDateChange)
                "월" -> MonthlyStatsContent(date = uiState.selectedDate, stats = uiState.monthlyStats, chartData = uiState.chartData, onDateChange = onDateChange)
                "년" -> YearlyStatsContent(date = uiState.selectedDate, stats = uiState.yearlyStats, chartData = uiState.chartData, onDateChange = onDateChange)
                "전체" -> TotalStatsContent(stats = uiState.totalStats)
            }
        }

        if (uiState.genreStats.isNotEmpty() && uiState.selectedTab != "전체") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text("장르별 독서 현황", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                GenreCharts(genreStats = uiState.genreStats)
            }
        }
    }
}

@Composable
fun DailyStatsContent(date: LocalDate, stats: DailyStats, chartData: List<Point>, onDateChange: (LocalDate) -> Unit) {
    val currentYear = LocalDate.now().year
    val isDifferentYear = date.year != currentYear
    val formattedDate = date.format(DateTimeFormatter.ofPattern("MM월 dd일"))
    val subTitle = if (isDifferentYear) "${date.year}년" else null
    val xAxisLabels = (0..23).map { if (it % 2 == 0) it.toString() else "" }

    StatsHeader(
        title = formattedDate,
        subTitle = subTitle,
        onPrevious = { onDateChange(date.minusDays(1)) },
        onNext = { onDateChange(date.plusDays(1)) },
        isNextEnabled = !date.isEqual(LocalDate.now())
    )
    Spacer(modifier = Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsDetailRow("접속시간", stats.accessTime)
        StatisticsDetailRow("총 독서시간", stats.totalReadingTime)
        StatisticsDetailRow("읽는중인 책", "${stats.readingBookCount}권")
        StatisticsDetailRow("완독한 책", "${stats.finishedBookCount}권")
    }
    Spacer(modifier = Modifier.height(24.dp))
    StatsLineChart(pointsData = chartData, xAxisLabels = xAxisLabels)
}

@Composable
fun WeeklyStatsContent(date: LocalDate, stats: WeeklyStats, chartData: List<Point>, onDateChange: (LocalDate) -> Unit) {
    val currentYear = LocalDate.now().year
    val isDifferentYear = date.year != currentYear
    val weekFields = WeekFields.of(Locale.KOREA)
    val firstDayOfWeek = date.with(weekFields.firstDayOfWeek)
    val lastDayOfWeek = firstDayOfWeek.plusDays(6)

    val title = "${firstDayOfWeek.monthValue}월 ${firstDayOfWeek.get(weekFields.weekOfMonth())}주차"
    val subTitle = if (isDifferentYear) "${date.year}년" else null
    val xAxisLabels = listOf("월", "화", "수", "목", "금", "토", "일")

    StatsHeader(
        title = title,
        subTitle = subTitle,
        onPrevious = { onDateChange(date.minusWeeks(1)) },
        onNext = { onDateChange(date.plusWeeks(1)) },
        isNextEnabled = lastDayOfWeek.isBefore(LocalDate.now())
    )
    Spacer(modifier = Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsDetailRow("접속시간", stats.accessTime)
        StatisticsDetailRow("총 독서시간", stats.totalReadingTime)
        StatisticsDetailRow("가장 많이 읽은 요일", stats.mostReadDay)
        StatisticsDetailRow("완독한 책", "${stats.finishedBookCount}권")
    }
    Spacer(modifier = Modifier.height(24.dp))
    StatsLineChart(pointsData = chartData, xAxisLabels = xAxisLabels)
}

@Composable
fun MonthlyStatsContent(date: LocalDate, stats: MonthlyStats, chartData: List<Point>, onDateChange: (LocalDate) -> Unit) {
    val currentYear = LocalDate.now().year
    val isDifferentYear = date.year != currentYear
    val yearMonth = YearMonth.from(date)
    val title = "${yearMonth.monthValue}월"
    val subTitle = if (isDifferentYear) "${date.year}년" else null
    val xAxisLabels = listOf("1주", "2주", "3주", "4주", "5주")

    StatsHeader(
        title = title,
        subTitle = subTitle,
        onPrevious = { onDateChange(date.minusMonths(1)) },
        onNext = { onDateChange(date.plusMonths(1)) },
        isNextEnabled = yearMonth.isBefore(YearMonth.now())
    )
    Spacer(modifier = Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsDetailRow("접속시간", stats.accessTime)
        StatisticsDetailRow("총 독서시간", stats.totalReadingTime)
        StatisticsDetailRow("가장 많이 읽은 주", stats.mostReadWeek)
        StatisticsDetailRow("완독한 책", "${stats.finishedBookCount}권")
    }
    Spacer(modifier = Modifier.height(24.dp))
    StatsLineChart(pointsData = chartData, xAxisLabels = xAxisLabels)
}

@Composable
fun YearlyStatsContent(date: LocalDate, stats: YearlyStats, chartData: List<Point>, onDateChange: (LocalDate) -> Unit) {
    val title = "${date.year}년"
    val xAxisLabels = (1..12).map { "${it}월" }

    StatsHeader(
        title = title,
        onPrevious = { onDateChange(date.minusYears(1)) },
        onNext = { onDateChange(date.plusYears(1)) },
        isNextEnabled = date.year < LocalDate.now().year
    )
    Spacer(modifier = Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsDetailRow("접속시간", stats.accessTime)
        StatisticsDetailRow("총 독서시간", stats.totalReadingTime)
        StatisticsDetailRow("가장 많이 읽은 달", stats.mostReadMonth)
        StatisticsDetailRow("완독한 책", "${stats.finishedBookCount}권")
    }
    Spacer(modifier = Modifier.height(24.dp))
    StatsLineChart(pointsData = chartData, xAxisLabels = xAxisLabels)
}

@Composable
fun TotalStatsContent(stats: TotalStats) {
    Box(modifier = Modifier.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Text("전체 통계", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsDetailRow("최초 접속일", stats.firstAccessDate)
        StatisticsDetailRow("총 접속일", "${stats.totalAccessDays}일")
        StatisticsDetailRow("총 독서시간", stats.totalReadingTime)
        StatisticsDetailRow("총 완독 권수", "${stats.totalFinishedBookCount}권")
    }
}

@Composable
fun GenreCharts(genreStats: List<GenreStats>) {
    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        GenreBarChart(genreStats = genreStats)
        GenreDonutChart(genreStats = genreStats)
    }
}

@Composable
fun GenreBarChart(genreStats: List<GenreStats>) {
    val maxRange = (genreStats.maxOfOrNull { it.count } ?: 0).let { if (it == 0) 5 else it + (it/4) }
    val barData = genreStats.map {
        BarData(
            point = Point(x = genreStats.indexOf(it).toFloat(), y = it.count.toFloat()),
            color = it.color,
            label = it.genre,
        )
    }

    val xAxisData = AxisData.Builder()
        .axisStepSize(30.dp)
        .steps(barData.size - 1)
        .startDrawPadding(20.dp)
        .bottomPadding(40.dp)
        .axisLabelAngle(20f)
        .axisLabelColor(MaterialTheme.colorScheme.onSurfaceVariant)
        .axisLineColor(Color.Transparent)
        .labelData { index -> barData.getOrNull(index)?.label ?: "" }
        .build()

    val yAxisData = AxisData.Builder()
        .steps(5)
        .labelAndAxisLinePadding(10.dp)
        .axisLineColor(Color.Transparent)
        .axisLabelColor(MaterialTheme.colorScheme.onSurfaceVariant)
        .labelData { index -> (index * (maxRange / 5)).toString() }
        .build()

    val chartData = BarChartData(
        chartData = barData,
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        barStyle = BarStyle(barWidth = 25.dp),
        backgroundColor = Color.Transparent
    )
    if (barData.isNotEmpty()){
        BarChart(modifier = Modifier.height(250.dp), barChartData = chartData)
    }
}

@Composable
fun GenreDonutChart(genreStats: List<GenreStats>) {
    val donutChartData = PieChartData(
        slices = genreStats.map {
            PieChartData.Slice(
                label = it.genre,
                value = it.count.toFloat(),
                color = it.color
            )
        },
        plotType = PlotType.Donut
    )

    val donutChartConfig = PieChartConfig(
        strokeWidth = 50f,
        activeSliceAlpha = .9f,
        isAnimationEnable = true,
        chartPadding = 30,
        showSliceLabels = false
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center){
                if(donutChartData.slices.all { it.value == 0f }){
                    Text("데이터 없음", fontSize = 14.sp)
                } else {
                    DonutPieChart(
                        modifier = Modifier.fillMaxSize(),
                        pieChartData = donutChartData,
                        pieChartConfig = donutChartConfig
                    )
                }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                genreStats.forEach {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(it.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${it.genre} (${it.count}권)", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun StatsLineChart(pointsData: List<Point>, xAxisLabels: List<String>) {
    val steps = 4
    val yMax = pointsData.maxOfOrNull { it.y }?.takeIf { it > 0f } ?: 5f
    val yAxisData = AxisData.Builder()
        .steps(steps)
        .labelAndAxisLinePadding(20.dp)
        .axisLineColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        .axisLabelColor(MaterialTheme.colorScheme.onSurfaceVariant)
        .axisLabelFontSize(12.sp)
        .labelData { i ->
            val yScale = yMax / steps
            String.format("%.0f", (i * yScale))
        }
        .build()

    val xAxisData = AxisData.Builder()
        .axisStepSize(40.dp)
        .steps(pointsData.size.let { if(it > 0) it - 1 else 0 })
        .axisLineColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        .axisLabelColor(MaterialTheme.colorScheme.onSurfaceVariant)
        .axisLabelFontSize(12.sp)
        .labelData { i -> xAxisLabels.getOrElse(i) { "" } }
        .build()

    val lineChartData = LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(
                Line(
                    dataPoints = pointsData,
                    lineStyle = LineStyle(
                        color = MaterialTheme.colorScheme.primary,
                        width = 4f
                    ),
                    intersectionPoint = IntersectionPoint(color = MaterialTheme.colorScheme.primary),
                    selectionHighlightPoint = SelectionHighlightPoint(color = MaterialTheme.colorScheme.primary),
                    shadowUnderLine = ShadowUnderLine(
                        alpha = 0.2f,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                Color.Transparent
                            )
                        )
                    ),
                    selectionHighlightPopUp = SelectionHighlightPopUp(
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            ),
        ),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        gridLines = GridLines(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        backgroundColor = MaterialTheme.colorScheme.surface
    )

    if (pointsData.any{ it.y > 0 }) {
        LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            lineChartData = lineChartData
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ){
            Text("기록된 데이터가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatsHeader(
    title: String,
    subTitle: String? = null,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    isNextEnabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        val prevButtonColor = MaterialTheme.colorScheme.onSurfaceVariant
        val nextButtonColor = if (isNextEnabled) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent

        if (onPrevious != null) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "이전",
                    tint = prevButtonColor
                )
            }
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
            IconButton(onClick = onNext, enabled = isNextEnabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "다음",
                    tint = nextButtonColor
                )
            }
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
