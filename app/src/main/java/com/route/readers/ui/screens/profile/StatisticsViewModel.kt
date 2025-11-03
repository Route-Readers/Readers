package com.route.readers.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// ViewModel에서 UI로 전달할 상태를 정의하는 데이터 클래스
data class StatisticsUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTab: String = "일",
    val dailyStats: DailyStats = DailyStats(),
    val weeklyStats: WeeklyStats = WeeklyStats(),
    val monthlyStats: MonthlyStats = MonthlyStats(),
    val yearlyStats: YearlyStats = YearlyStats(),
    val totalStats: TotalStats = TotalStats(),
    val chartData: List<co.yml.charts.common.model.Point> = emptyList()
)

// 비어있는 기본값을 위한 data class 기본 생성자
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
        // ViewModel이 생성될 때 초기 데이터 로드
        fetchStatistics()
    }

    fun onDateChange(newDate: LocalDate) {
        _uiState.update { it.copy(selectedDate = newDate) }
        fetchStatistics() // 날짜가 바뀌면 데이터 다시 로드
    }

    fun onTabChange(newTab: String) {
        _uiState.update { it.copy(selectedTab = newTab) }
        fetchStatistics() // 탭이 바뀌면 데이터 다시 로드
    }

    private fun fetchStatistics() {
        viewModelScope.launch {
            // 현재 상태 값을 가져옴
            val currentState = _uiState.value
            val date = currentState.selectedDate
            val tab = currentState.selectedTab

            // 선택된 탭에 따라 다른 데이터를 생성/로드 (현재는 임시 데이터 생성)
            when (tab) {
                "일" -> {
                    _uiState.update {
                        it.copy(
                            dailyStats = DailyStats("1h 5m", "45m", 2, 0),
                            chartData = (0..23).map { hour ->
                                co.yml.charts.common.model.Point(hour.toFloat(), (0..60).random().toFloat())
                            }
                        )
                    }
                }
                "주" -> {
                    _uiState.update {
                        it.copy(
                            weeklyStats = WeeklyStats("7h 30m", "5h", "수요일", 1),
                            chartData = (0..6).map { day ->
                                co.yml.charts.common.model.Point(day.toFloat(), (30..180).random().toFloat())
                            }
                        )
                    }
                }
                "월" -> {
                    _uiState.update {
                        it.copy(
                            monthlyStats = MonthlyStats("30h", "22h", "2주차", 4),
                            chartData = (0..3).map { week ->
                                co.yml.charts.common.model.Point(week.toFloat(), (3..10).random().toFloat())
                            }
                        )
                    }
                }
                "년" -> {
                    _uiState.update {
                        it.copy(
                            yearlyStats = YearlyStats("350h", "280h", "8월", 30),
                            chartData = (0..11).map { month ->
                                co.yml.charts.common.model.Point(month.toFloat(), (10..40).random().toFloat())
                            }
                        )
                    }
                }
                "전체" -> {
                    _uiState.update {
                        it.copy(
                            totalStats = TotalStats("2023-01-15", 300, "1000h", 80),
                            chartData = (0..11).map { month ->
                                co.yml.charts.common.model.Point(month.toFloat(), (50..200).random().toFloat())
                            }
                        )
                    }
                }
            }
        }
    }
}
