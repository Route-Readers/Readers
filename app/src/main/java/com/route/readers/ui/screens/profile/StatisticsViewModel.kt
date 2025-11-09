package com.route.readers.ui.screens.profile

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.model.Point
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.ReadingSession
import com.route.readers.data.remote.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// UI State Data Classes
data class DailyStats(
    val accessTime: String = "0분",
    val totalReadingTime: String = "0분",
    val readingBookCount: Int = 0,
    val finishedBookCount: Int = 0
)

data class WeeklyStats(
    val accessTime: String = "0분",
    val totalReadingTime: String = "0분",
    val mostReadDay: String = "없음",
    val finishedBookCount: Int = 0
)

data class MonthlyStats(
    val accessTime: String = "0분",
    val totalReadingTime: String = "0분",
    val mostReadWeek: String = "없음",
    val finishedBookCount: Int = 0
)

data class YearlyStats(
    val accessTime: String = "0분",
    val totalReadingTime: String = "0분",
    val mostReadMonth: String = "없음",
    val finishedBookCount: Int = 0
)

data class TotalStats(
    val firstAccessDate: String = "기록 없음",
    val totalAccessDays: Int = 0,
    val totalReadingTime: String = "0분",
    val totalFinishedBookCount: Int = 0
)

data class GenreStats(
    val genre: String,
    val count: Int,
    val color: Color
)

data class StatisticsUiState(
    val selectedTab: String = "일",
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyStats: DailyStats = DailyStats(),
    val weeklyStats: WeeklyStats = WeeklyStats(),
    val monthlyStats: MonthlyStats = MonthlyStats(),
    val yearlyStats: YearlyStats = YearlyStats(),
    val totalStats: TotalStats = TotalStats(),
    val chartData: List<Point> = emptyList(),
    val genreStats: List<GenreStats> = emptyList(),
    val isLoading: Boolean = true
)

class StatisticsViewModel : ViewModel() {
    private val firestoreRepository = FirestoreRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        if (currentUserId == null) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
        else{
            loadStatistics()
            }
    }

    fun onDateChange(newDate: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = newDate)
        loadStatistics()
    }

    fun onTabChange(newTab: String) {
        _uiState.value = _uiState.value.copy(selectedTab = newTab)
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            if (currentUserId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            val (startDateMillis, endDateMillis) = getDateRangeForSelectedTab()
            val startDate: Date? = if (startDateMillis != null) Date(startDateMillis) else null
            val endDate: Date? = if (endDateMillis != null) Date(endDateMillis) else null
            val sessions = firestoreRepository.getReadingSessions(currentUserId, startDate, endDate)
            val allBooks = firestoreRepository.getMyBooks() // 모든 책 정보 필요 (장르, 완료 여부 등)

            val finishedBooksInPeriod = allBooks.filter { book ->
                book.isCompleted && book.completedDate?.let { completedDate ->
                    completedDate >= (startDate?.time ?: 0L) &&
                    completedDate <= (endDate?.time ?: Long.MAX_VALUE)
                } ?: false
            }

            when (_uiState.value.selectedTab) {
                "일" -> _uiState.value = _uiState.value.copy(
                    dailyStats = calculateDailyStats(sessions, finishedBooksInPeriod),
                    chartData = calculateDailyChartData(sessions),
                    genreStats = calculateGenreStats(sessions, allBooks)
                )
                "주" -> _uiState.value = _uiState.value.copy(
                    weeklyStats = calculateWeeklyStats(sessions, finishedBooksInPeriod),
                    chartData = calculateWeeklyChartData(sessions),
                    genreStats = calculateGenreStats(sessions, allBooks)
                )
                "월" -> _uiState.value = _uiState.value.copy(
                    monthlyStats = calculateMonthlyStats(sessions, finishedBooksInPeriod),
                    chartData = calculateMonthlyChartData(sessions),
                    genreStats = calculateGenreStats(sessions, allBooks)
                )
                "년" -> _uiState.value = _uiState.value.copy(
                    yearlyStats = calculateYearlyStats(sessions, finishedBooksInPeriod),
                    chartData = calculateYearlyChartData(sessions),
                    genreStats = calculateGenreStats(sessions, allBooks)
                )
                "전체" -> _uiState.value = _uiState.value.copy(
                    totalStats = calculateTotalStats(sessions, allBooks)
                )
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun getDateRangeForSelectedTab(): Pair<Long?, Long?> {
        val selectedDate = _uiState.value.selectedDate
        val result: Pair<Long?, Long?>

        if (_uiState.value.selectedTab == "일") {
            val startOfDayInstant = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDayInstant = selectedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant()
            result = Pair(startOfDayInstant.toEpochMilli(), endOfDayInstant.toEpochMilli())
        } else if (_uiState.value.selectedTab == "주") {
            val weekFields = WeekFields.of(Locale.KOREA)
            val firstDayOfWeek = selectedDate.with(weekFields.dayOfWeek(), 1) // 월요일
            val lastDayOfWeek = firstDayOfWeek.plusDays(6) // 일요일
            val startOfWeekInstant = firstDayOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfWeekInstant = lastDayOfWeek.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant()
            result = Pair(startOfWeekInstant.toEpochMilli(), endOfWeekInstant.toEpochMilli())
        } else if (_uiState.value.selectedTab == "월") {
            val firstDayOfMonth = selectedDate.withDayOfMonth(1)
            val lastDayOfMonth = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth())
            val startOfMonthInstant = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfMonthInstant = lastDayOfMonth.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant()
            result = Pair(startOfMonthInstant.toEpochMilli(), endOfMonthInstant.toEpochMilli())
        } else if (_uiState.value.selectedTab == "년") {
            val firstDayOfYear = selectedDate.withDayOfYear(1)
            val lastDayOfYear = selectedDate.withDayOfYear(selectedDate.lengthOfYear())
            val startOfYearInstant = firstDayOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfYearInstant = lastDayOfYear.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant()
            result = Pair(startOfYearInstant.toEpochMilli(), endOfYearInstant.toEpochMilli())
        } else if (_uiState.value.selectedTab == "전체") {
            result = Pair(null, null)
        } else {
            result = Pair(null, null)
        }
        return result
    }

    private fun calculateDailyStats(sessions: List<ReadingSession>, finishedBooks: List<com.route.readers.data.model.MyBook>): DailyStats {
        val totalDuration = sessions.sumOf { it.durationInSeconds }
        val readingBookCount = sessions.distinctBy { it.bookId }.size
        return DailyStats(
            totalReadingTime = formatDuration(totalDuration),
            readingBookCount = readingBookCount,
            finishedBookCount = finishedBooks.size
        )
    }

    private fun calculateWeeklyStats(sessions: List<ReadingSession>, finishedBooks: List<com.route.readers.data.model.MyBook>): WeeklyStats {
        val totalDuration = sessions.sumOf { it.durationInSeconds }
        val mostReadDay = sessions.groupBy { it.dayOfWeek }
            .maxByOrNull { it.value.sumOf { s -> s.durationInSeconds } }
            ?.key
            ?.let { dayOfWeek ->
                when (dayOfWeek) {
                    Calendar.MONDAY -> "월요일"
                    Calendar.TUESDAY -> "화요일"
                    Calendar.WEDNESDAY -> "수요일"
                    Calendar.THURSDAY -> "목요일"
                    Calendar.FRIDAY -> "금요일"
                    Calendar.SATURDAY -> "토요일"
                    Calendar.SUNDAY -> "일요일"
                    else -> "없음"
                }
            } ?: "없음"
        return WeeklyStats(
            totalReadingTime = formatDuration(totalDuration),
            mostReadDay = mostReadDay,
            finishedBookCount = finishedBooks.size
        )
    }

    private fun calculateMonthlyStats(sessions: List<ReadingSession>, finishedBooks: List<com.route.readers.data.model.MyBook>): MonthlyStats {
        val totalDuration = sessions.sumOf { it.durationInSeconds }
        val mostReadWeek = sessions.groupBy { it.weekOfYear }
            .maxByOrNull { it.value.sumOf { s -> s.durationInSeconds } }
            ?.key
            ?.let { week -> "${week}주차" } ?: "없음"
        return MonthlyStats(
            totalReadingTime = formatDuration(totalDuration),
            mostReadWeek = mostReadWeek,
            finishedBookCount = finishedBooks.size
        )
    }

    private fun calculateYearlyStats(sessions: List<ReadingSession>, finishedBooks: List<com.route.readers.data.model.MyBook>): YearlyStats {
        val totalDuration = sessions.sumOf { it.durationInSeconds }
        val mostReadMonth = sessions.groupBy { it.month }
            .maxByOrNull { it.value.sumOf { s -> s.durationInSeconds } }
            ?.key
            ?.let { month -> "${month}월" } ?: "없음"
        return YearlyStats(
            totalReadingTime = formatDuration(totalDuration),
            mostReadMonth = mostReadMonth,
            finishedBookCount = finishedBooks.size
        )
    }

    private suspend fun calculateTotalStats(sessions: List<ReadingSession>, allBooks: List<com.route.readers.data.model.MyBook>): TotalStats {
        val totalDuration = sessions.sumOf { it.durationInSeconds }
        val totalFinishedBookCount = allBooks.count { it.isCompleted }

        val firstSessionDate = sessions.minOfOrNull { it.startTime?.time ?: Long.MAX_VALUE }
        val firstAccessDate = if (firstSessionDate != null && firstSessionDate != Long.MAX_VALUE) {
            SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault()).format(Date(firstSessionDate))
        } else {
            "기록 없음"
        }

        val totalAccessDays = sessions.map {
            it.startTime?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
        }.filterNotNull().distinct().size

        return TotalStats(
            firstAccessDate = firstAccessDate,
            totalAccessDays = totalAccessDays,
            totalReadingTime = formatDuration(totalDuration),
            totalFinishedBookCount = totalFinishedBookCount
        )
    }

    private fun calculateDailyChartData(sessions: List<ReadingSession>): List<Point> {
        val hourlyData = sessions.groupBy {
            it.startTime?.toInstant()?.atZone(ZoneId.systemDefault())?.hour ?: 0
        }.mapValues { entry ->
            entry.value.sumOf { it.durationInSeconds } / 60 // 분 단위
        }

        return (0..23).map { hour ->
            Point(x = hour.toFloat(), y = hourlyData[hour]?.toFloat() ?: 0f)
        }
    }

    private fun calculateWeeklyChartData(sessions: List<ReadingSession>): List<Point> {
        val dailyData = sessions.groupBy { it.dayOfWeek }
            .mapValues { entry ->
                entry.value.sumOf { it.durationInSeconds } / 60 // 분 단위
            }
        // 월(1) ~ 일(7) 순서로 정렬
        return (1..7).map { dayOfWeek ->
            Point(x = dayOfWeek.toFloat(), y = dailyData[dayOfWeek]?.toFloat() ?: 0f)
        }
    }

    private fun calculateMonthlyChartData(sessions: List<ReadingSession>): List<Point> {
        val weekFields = WeekFields.of(Locale.KOREA)
        val weeklyData = sessions.groupBy {
            it.startTime?.toInstant()?.atZone(ZoneId.systemDefault())?.get(weekFields.weekOfMonth()) ?: 0
        }.mapValues { entry ->
            entry.value.sumOf { it.durationInSeconds } / 60 // 분 단위
        }

        return (1..5).map { weekOfMonth ->
            Point(x = weekOfMonth.toFloat(), y = weeklyData[weekOfMonth]?.toFloat() ?: 0f)
        }
    }

    private fun calculateYearlyChartData(sessions: List<ReadingSession>): List<Point> {
        val monthlyData = sessions.groupBy { it.month }
            .mapValues { entry ->
                entry.value.sumOf { it.durationInSeconds } / 60 // 분 단위
            }
        return (1..12).map { month ->
            Point(x = month.toFloat(), y = monthlyData[month]?.toFloat() ?: 0f)
        }
    }

    private fun calculateGenreStats(sessions: List<ReadingSession>, allBooks: List<com.route.readers.data.model.MyBook>): List<GenreStats> {
        val sessionBookIds = sessions.map { it.bookId }.distinct()
        val booksInPeriod = allBooks.filter { sessionBookIds.contains(it.isbn) }

        val genreCounts = booksInPeriod.flatMap { it.genres }
            .groupBy { it }
            .mapValues { it.value.size }

        val totalCount = genreCounts.values.sum()
        if (totalCount == 0) return emptyList()

        val colors = listOf(
            Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF66BB6A), Color(0xFFFFCA28),
            Color(0xFF26A69A), Color(0xFF7E57C2), Color(0xFFFFA726), Color(0xFF29B6F6)
        )
        var colorIndex = 0

        return genreCounts.entries.sortedByDescending { it.value }.map { (genre, count) ->
            GenreStats(genre, count, colors[colorIndex++ % colors.size])
        }
    }

    private fun formatDuration(totalSeconds: Int): String {
        if (totalSeconds == 0) return "0분"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}시간 ${minutes}분"
            minutes > 0 -> "${minutes}분"
            else -> "${seconds}초"
        }
    }
}