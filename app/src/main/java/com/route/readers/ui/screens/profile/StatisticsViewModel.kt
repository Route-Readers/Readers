package com.route.readers.ui.screens.profile

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.model.Point
import com.route.readers.ReadersApplication
import com.route.readers.data.remote.MyLibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GenreStats(
    val genre: String,
    val count: Int,
    val color: Color
)

data class StatisticsUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTab: String = "일",
    val dailyStats: DailyStats = DailyStats(),
    val weeklyStats: WeeklyStats = WeeklyStats(),
    val monthlyStats: MonthlyStats = MonthlyStats(),
    val yearlyStats: YearlyStats = YearlyStats(),
    val totalStats: TotalStats = TotalStats(),
    val chartData: List<Point> = emptyList(),
    val genreStats: List<GenreStats> = emptyList()
)

data class DailyStats(
    val accessTime: String = "0m",
    val totalReadingTime: String = "0m",
    val readingBookCount: Int = 0,
    val finishedBookCount: Int = 0
)

data class WeeklyStats(
    val accessTime: String = "0m",
    val totalReadingTime: String = "0m",
    val mostReadDay: String = "-",
    val finishedBookCount: Int = 0
)

data class MonthlyStats(
    val accessTime: String = "0m",
    val totalReadingTime: String = "0m",
    val mostReadWeek: String = "-",
    val finishedBookCount: Int = 0
)

data class YearlyStats(
    val accessTime: String = "0m",
    val totalReadingTime: String = "0m",
    val mostReadMonth: String = "-",
    val finishedBookCount: Int = 0
)

data class TotalStats(
    val firstAccessDate: String = "-",
    val totalAccessDays: Int = 0,
    val totalReadingTime: String = "0m",
    val totalFinishedBookCount: Int = 0
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val sessionTimer = getApplication<ReadersApplication>().sessionTimer
    private val myLibraryRepository = MyLibraryRepository()

    init {
        fetchStatistics()
    }

    fun onDateChange(newDate: LocalDate) {
        _uiState.update { it.copy(selectedDate = newDate) }
        fetchStatistics()
    }

    fun onTabChange(newTab: String) {
        _uiState.update { it.copy(selectedTab = newTab) }
        fetchStatistics()
    }

    private fun fetchStatistics() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val allBooks = myLibraryRepository.getMyBooks()
            val readingBookCount = allBooks.count { !it.isCompleted }
            val finishedBookCount = allBooks.count { it.isCompleted }
            val tempGenreStats = listOf(
                GenreStats("소설", 8, Color(0xFF00C853)),
                GenreStats("에세이", 5, Color(0xFF009688)),
                GenreStats("자기계발", 12, Color(0xFF4CAF50)),
                GenreStats("IT", 9, Color(0xFF8BC34A)),
                GenreStats("인문", 3, Color(0xFFCDDC39))
            )

            when (currentState.selectedTab) {
                "일" -> {
                    val selectedDateKey =
                        currentState.selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val dailyDataMap = sessionTimer.getRecentSessionTimes(365).first()
                    val accessMillis = dailyDataMap[selectedDateKey] ?: 0L
                    val formattedTime = sessionTimer.formatDuration(accessMillis)

                    val hourlyData =
                        sessionTimer.getHourlySessionTimes(currentState.selectedDate).first()
                    val chartPoints = (0..23).map { hour ->
                        val minutes =
                            TimeUnit.MILLISECONDS.toMinutes(hourlyData[hour] ?: 0L).toFloat()
                        Point(hour.toFloat(), minutes)
                    }

                    _uiState.update {
                        it.copy(
                            dailyStats = DailyStats(
                                accessTime = formattedTime,
                                totalReadingTime = "0m",
                                readingBookCount = readingBookCount,
                                finishedBookCount = finishedBookCount
                            ),
                            chartData = chartPoints,
                            genreStats = tempGenreStats
                        )
                    }
                }

                "주" -> {
                    val weekFields = WeekFields.of(Locale.KOREA)
                    val firstDayOfWeek =
                        currentState.selectedDate.with(weekFields.firstDayOfWeek)
                    val dateKeys = (0..6).map {
                        firstDayOfWeek.plusDays(it.toLong())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    }

                    val weeklyData = sessionTimer.getRecentSessionTimes(365).first()
                        .filterKeys { it in dateKeys }
                    val totalMillis = weeklyData.values.sum()
                    val formattedTime = sessionTimer.formatDuration(totalMillis)

                    val mostReadDayData = weeklyData.maxByOrNull { it.value }
                    val mostReadDay = if (mostReadDayData != null && mostReadDayData.value > 0) {
                        LocalDate.parse(mostReadDayData.key).dayOfWeek.getDisplayName(
                            TextStyle.FULL,
                            Locale.KOREAN
                        )
                    } else {
                        "-"
                    }

                    val chartPoints = dateKeys.mapIndexed { index, dateKey ->
                        val minutes =
                            TimeUnit.MILLISECONDS.toMinutes(weeklyData[dateKey] ?: 0L).toFloat()
                        Point(index.toFloat(), minutes)
                    }

                    _uiState.update {
                        it.copy(
                            weeklyStats = WeeklyStats(
                                accessTime = formattedTime,
                                totalReadingTime = "0m",
                                mostReadDay = mostReadDay,
                                finishedBookCount = finishedBookCount
                            ),
                            chartData = chartPoints,
                            genreStats = tempGenreStats
                        )
                    }
                }

                "월" -> {
                    val yearMonth = currentState.selectedDate
                    val firstDayOfMonth = yearMonth.withDayOfMonth(1)
                    val lastDayOfMonth = yearMonth.withDayOfMonth(yearMonth.lengthOfMonth())

                    val dateKeys = (0 until lastDayOfMonth.dayOfMonth).map {
                        firstDayOfMonth.plusDays(it.toLong())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    }

                    val monthlyData = sessionTimer.getRecentSessionTimes(365).first()
                        .filterKeys { it in dateKeys }
                    val totalMillis = monthlyData.values.sum()
                    val formattedTime = sessionTimer.formatDuration(totalMillis)

                    val weeklyMinutes = Array(5) { 0L }
                    val weekFields = WeekFields.of(Locale.KOREA)

                    monthlyData.forEach { (dateStr, millis) ->
                        val date = LocalDate.parse(dateStr)
                        val weekOfMonth = date.get(weekFields.weekOfMonth()) - 1
                        if (weekOfMonth in 0..4) {
                            weeklyMinutes[weekOfMonth] += TimeUnit.MILLISECONDS.toMinutes(millis)
                        }
                    }

                    val mostReadWeekIndex =
                        weeklyMinutes.indices.maxByOrNull { weeklyMinutes[it] } ?: -1
                    val mostReadWeek =
                        if (mostReadWeekIndex != -1 && weeklyMinutes[mostReadWeekIndex] > 0) "${mostReadWeekIndex + 1}주차" else "-"

                    _uiState.update {
                        it.copy(
                            monthlyStats = MonthlyStats(
                                accessTime = formattedTime,
                                totalReadingTime = "0m",
                                mostReadWeek = mostReadWeek,
                                finishedBookCount = finishedBookCount
                            ),
                            chartData = weeklyMinutes.mapIndexed { index, minutes ->
                                Point(
                                    index.toFloat(),
                                    minutes.toFloat()
                                )
                            },
                            genreStats = tempGenreStats
                        )
                    }
                }

                "년", "전체" -> {
                    val totalMillis = sessionTimer.getTotalSessionTimeFlow().first()
                    val formattedTime = sessionTimer.formatDuration(totalMillis)

                    _uiState.update {
                        it.copy(
                            yearlyStats = it.yearlyStats.copy(
                                accessTime = formattedTime,
                                finishedBookCount = finishedBookCount
                            ),
                            totalStats = it.totalStats.copy(
                                totalReadingTime = formattedTime,
                                totalFinishedBookCount = finishedBookCount
                            ),
                            chartData = emptyList(),
                            genreStats = tempGenreStats
                        )
                    }
                }
            }
        }
    }
}
