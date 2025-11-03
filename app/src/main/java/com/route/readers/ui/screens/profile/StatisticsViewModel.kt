package com.route.readers.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// 장르 데이터 클래스 추가
data class GenreStats(
    val genre: String,
    val count: Int,
    val color: androidx.compose.ui.graphics.Color
)

// UI 상태에 genreStats 추가
data class StatisticsUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTab: String = "일",
    val dailyStats: DailyStats = DailyStats(),
    val weeklyStats: WeeklyStats = WeeklyStats(),
    val monthlyStats: MonthlyStats = MonthlyStats(),
    val yearlyStats: YearlyStats = YearlyStats(),
    val totalStats: TotalStats = TotalStats(),
    val chartData: List<co.yml.charts.common.model.Point> = emptyList(),
    val genreStats: List<GenreStats> = emptyList() // 장르 통계 데이터
)

data class DailyStats(
    val accessTime: String = "",
    val totalReadingTime: String = "",
    val readingBookCount: Int = 0,
    val finishedBookCount: Int = 0
)

data class WeeklyStats(
    val accessTime: String = "",
    val totalReadingTime: String = "",
    val mostReadDay: String = "",
    val finishedBookCount: Int = 0
)

data class MonthlyStats(
    val accessTime: String = "",
    val totalReadingTime: String = "",
    val mostReadWeek: String = "",
    val finishedBookCount: Int = 0
)

data class YearlyStats(
    val accessTime: String = "",
    val totalReadingTime: String = "",
    val mostReadMonth: String = "",
    val finishedBookCount: Int = 0
)

data class TotalStats(
    val firstAccessDate: String = "",
    val totalAccessDays: Int = 0,
    val totalReadingTime: String = "",
    val totalFinishedBookCount: Int = 0
)


class StatisticsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

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

            // 임시 장르 데이터 생성
            val tempGenreStats = listOf(
                GenreStats("소설", 8, androidx.compose.ui.graphics.Color(0xFF00C853)),
                GenreStats("에세이", 5, androidx.compose.ui.graphics.Color(0xFF009688)),
                GenreStats("자기계발", 12, androidx.compose.ui.graphics.Color(0xFF4CAF50)),
                GenreStats("IT", 9, androidx.compose.ui.graphics.Color(0xFF8BC34A)),
                GenreStats("인문", 3, androidx.compose.ui.graphics.Color(0xFFCDDC39))
            )

            // 선택된 탭에 따라 다른 데이터를 생성/로드
            when (currentState.selectedTab) {
                "일" -> {
                    _uiState.update {
                        it.copy(
                            dailyStats = DailyStats("1h 5m", "45m", 2, 0),
                            chartData = (0..23).map { hour ->
                                co.yml.charts.common.model.Point(hour.toFloat(), (0..60).random().toFloat())
                            },
                            genreStats = tempGenreStats
                        )
                    }
                }
                "주" -> {
                    _uiState.update {
                        it.copy(
                            weeklyStats = WeeklyStats("7h 30m", "5h", "수요일", 1),
                            chartData = (0..6).map { day ->
                                co.yml.charts.common.model.Point(day.toFloat(), (30..180).random().toFloat())
                            },
                            genreStats = tempGenreStats
                        )
                    }
                }
                "월" -> {
                    _uiState.update {
                        it.copy(
                            monthlyStats = MonthlyStats("30h", "22h", "2주차", 4),
                            chartData = (0..3).map { week ->
                                co.yml.charts.common.model.Point(week.toFloat(), (3..10).random().toFloat())
                            },
                            genreStats = tempGenreStats
                        )
                    }
                }
                "년" -> {
                    _uiState.update {
                        it.copy(
                            yearlyStats = YearlyStats("350h", "280h", "8월", 30),
                            chartData = (0..11).map { month ->
                                co.yml.charts.common.model.Point(month.toFloat(), (10..40).random().toFloat())
                            },
                            genreStats = tempGenreStats
                        )
                    }
                }
                "전체" -> {
                    _uiState.update {
                        it.copy(
                            totalStats = TotalStats("2023-01-15", 300, "1000h", 80),
                            chartData = (0..11).map { month ->
                                co.yml.charts.common.model.Point(month.toFloat(), (50..200).random().toFloat())
                            },
                            genreStats = tempGenreStats
                        )
                    }
                }
            }
        }
    }
}
