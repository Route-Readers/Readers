package com.route.readers.ui.screens.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.Challenge
import com.route.readers.data.model.ChallengeType
import com.route.readers.data.remote.ChallengeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ChallengeUiState(
    val isLoading: Boolean = true,
    val userChallenge: Challenge? = null,
    val availableChallenges: List<Challenge> = emptyList(),
    val showCreateDialog: Boolean = false
)

class ChallengeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengeUiState())
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    private val repository = ChallengeRepository()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init {
        initChallenges()
    }

    fun refreshChallenges() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val currentWeekNumber = getCurrentWeekNumber()
            val weeklyChallenges = repository.getChallengesForWeek(currentWeekNumber)

            val userJoinedChallenge = weeklyChallenges.find { it.participants.contains(currentUserId) }

            if (userJoinedChallenge != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userChallenge = userJoinedChallenge,
                    availableChallenges = emptyList()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userChallenge = null,
                    availableChallenges = weeklyChallenges.filter { it.type != ChallengeType.CUSTOM }
                )
            }
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    private fun getThisMonday(): java.util.Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val today = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (today == Calendar.SUNDAY) 6 else today - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

        return calendar.time
    }

    private fun getNextSunday(): java.util.Date {
        val calendar = Calendar.getInstance()
        calendar.time = getThisMonday()
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)

        return calendar.time
    }

    private fun getCurrentWeekNumber(): Int {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val week = calendar.get(Calendar.WEEK_OF_YEAR)
        return year * 100 + week
    }

    private fun initChallenges() {
        viewModelScope.launch {
            val currentWeekNumber = getCurrentWeekNumber()
            val existingChallenges = repository.getChallengesForWeek(currentWeekNumber)
            if (existingChallenges.isEmpty()) {
                viewModelScope.launch { createWeeklyChallengesIfNeeded() }
            }
            refreshChallenges()
        }
    }

    private suspend fun createWeeklyChallengesIfNeeded() {
        val startDate = getThisMonday()
        val endDate = getNextSunday()
        val weekNumber = getCurrentWeekNumber()

        val defaultChallenges = listOf(
            Challenge(
                id = "challenge_${weekNumber}_1",
                title = "매일 30페이지 읽기",
                description = "하루에 30페이지씩 7일 동안 읽기",
                type = ChallengeType.DAILY_PAGES_READING,
                goal = 30,
                startDate = startDate,
                endDate = endDate,
                weekNumber = weekNumber,
                reward = "50 토큰"
            ),
            Challenge(
                id = "challenge_${weekNumber}_2",
                title = "7일 연속 독서",
                description = "7일 동안 매일 책 읽기",
                type = ChallengeType.CONSECUTIVE_READING,
                goal = 7,
                startDate = startDate,
                endDate = endDate,
                weekNumber = weekNumber,
                reward = "30 토큰"
            ),
            Challenge(
                id = "challenge_${weekNumber}_3",
                title = "매일 50페이지 읽기",
                description = "하루에 50페이지씩 7일 동안 읽기",
                type = ChallengeType.DAILY_PAGES_READING,
                goal = 50,
                startDate = startDate,
                endDate = endDate,
                weekNumber = weekNumber,
                reward = "100 토큰"
            )
        )

        defaultChallenges.forEach { repository.createChallenge(it) }
    }

    fun createChallenge(title: String, description: String, goal: Int) {
        viewModelScope.launch {
            val startDate = getThisMonday()
            val endDate = getNextSunday()
            val weekNumber = getCurrentWeekNumber()

            val newChallenge = Challenge(
                id = "challenge_${weekNumber}_${System.currentTimeMillis()}",
                title = title,
                description = description,
                type = ChallengeType.CUSTOM,
                goal = goal,
                participants = listOf(currentUserId),
                startDate = startDate,
                endDate = endDate,
                weekNumber = weekNumber,
                progress = mapOf(currentUserId to 0)
            )
            repository.createChallenge(newChallenge)
            refreshChallenges()
            hideCreateDialog()
        }
    }

    fun joinChallenge(challengeId: String) {
        viewModelScope.launch {
            repository.joinChallenge(challengeId, currentUserId)
            refreshChallenges()
        }
    }

    fun updateDailyProgress(challengeId: String, pagesRead: Int) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
            repository.updateDailyProgress(challengeId, currentUserId, today, pagesRead)
            refreshChallenges()
        }
    }

    // 페이지 업데이트 시 자동으로 호출되는 함수
    fun onPagesRead(pagesRead: Int) {
        viewModelScope.launch {
            val userChallenge = _uiState.value.userChallenge
            if (userChallenge != null && userChallenge.type == ChallengeType.DAILY_PAGES_READING) {
                updateDailyProgress(userChallenge.id, pagesRead)
            }
        }
    }
}