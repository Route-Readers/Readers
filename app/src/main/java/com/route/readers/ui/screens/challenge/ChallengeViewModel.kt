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
    val challenges: List<Challenge> = emptyList(),
    val showCreateDialog: Boolean = false
)

class ChallengeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengeUiState())
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    private val repository = ChallengeRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init {
        loadChallenges()
        createWeeklyChallengesIfNeeded()
    }

    private fun loadChallenges() {
        viewModelScope.launch {
            val challenges = repository.getAvailableChallenges()
            _uiState.value = _uiState.value.copy(challenges = challenges)
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

    private fun createWeeklyChallengesIfNeeded() {
        viewModelScope.launch {
            val existingChallenges = repository.getAvailableChallenges()
            if (existingChallenges.isEmpty()) {
                // 기본 챌린지 3개 생성
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
                        type = ChallengeType.CONSECUTIVE_READING_WITH_FRIEND,
                        goal = 1,
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
                loadChallenges()
            }
        }
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
                goal = goal,
                participants = listOf(currentUserId),
                startDate = startDate,
                endDate = endDate,
                weekNumber = weekNumber,
                progress = mapOf(currentUserId to 0)
            )
            repository.createChallenge(newChallenge)
            loadChallenges()
            hideCreateDialog()
        }
    }

    fun joinChallenge(challengeId: String) {
        viewModelScope.launch {
            repository.joinChallenge(challengeId, currentUserId)
            loadChallenges()
        }
    }
    
    fun updateDailyProgress(challengeId: String, pagesRead: Int) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
            repository.updateDailyProgress(challengeId, currentUserId, today, pagesRead)
            loadChallenges()
        }
    }
}
