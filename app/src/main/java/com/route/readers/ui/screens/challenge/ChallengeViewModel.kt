package com.route.readers.ui.screens.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.Challenge
import com.route.readers.data.model.ChallengeType
import com.route.readers.data.remote.ChallengeRepository
import com.route.readers.data.remote.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.tasks.await
import com.route.readers.data.remote.AttendanceRepository
import com.route.readers.ui.screens.attendance.AttendanceData
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ChallengeUiState(
    val isLoading: Boolean = true,
    val userChallenges: List<Challenge> = emptyList(),
    val availableChallenges: List<Challenge> = emptyList(),
    val showCreateDialog: Boolean = false,
    val consecutiveReadingDays: Int = 0
)

class ChallengeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengeUiState())
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    private val firestoreRepository = FirestoreRepository()
    private val repository = ChallengeRepository(firestoreRepository)
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    private val attendanceRepository = AttendanceRepository()

    init {
        if (currentUserId.isNotBlank()) {
            // Observe the active challenge from the repository's new real-time stream
            repository.getActiveChallengeStream(currentUserId)
                .onEach { challenges ->
                    _uiState.value = _uiState.value.copy(
                        userChallenges = challenges,
                        isLoading = false
                    )
                    // When the challenges are loaded, if any are consecutive reading ones, update their progress
                    if (challenges.any { it.type == ChallengeType.CONSECUTIVE_READING }) {
                        updateConsecutiveReadingProgress()
                    }
                }
                .launchIn(viewModelScope)
        }

        loadAvailableChallenges()
        initDefaultChallenges()
    }

    private fun updateConsecutiveReadingProgress() {
        viewModelScope.launch {
            val attendanceData = attendanceRepository.getAttendanceData()
            val consecutiveReadingDays = attendanceRepository.calculateConsecutiveDays(attendanceData) { it.event != null }

            // Update the UI state
            _uiState.value = _uiState.value.copy(consecutiveReadingDays = consecutiveReadingDays)

            // Update Firestore for all consecutive reading challenges
            val userChallenges = _uiState.value.userChallenges
            userChallenges.filter { it.type == ChallengeType.CONSECUTIVE_READING }.forEach { challenge ->
                if ((challenge.progress[currentUserId] ?: 0) != consecutiveReadingDays) {
                    repository.updateChallengeProgress(challenge.id, currentUserId, consecutiveReadingDays)
                }
            }
        }
    }
    private fun loadAvailableChallenges() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val currentWeekNumber = getCurrentWeekNumber()
                val weeklyChallenges = repository.getChallengesForWeek(currentWeekNumber)
                _uiState.value = _uiState.value.copy(
                    availableChallenges = weeklyChallenges.filter { it.type != ChallengeType.CUSTOM },
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun refreshAvailableChallenges() {
        loadAvailableChallenges()
        updateConsecutiveReadingProgress()
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

    private fun initDefaultChallenges() {
        viewModelScope.launch {
            val currentWeekNumber = getCurrentWeekNumber()
            val existingChallenges = repository.getChallengesForWeek(currentWeekNumber)
            if (existingChallenges.isEmpty()) {
                createWeeklyChallengesIfNeeded()
            }
            refreshAvailableChallenges()
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
            refreshAvailableChallenges()
            hideCreateDialog()
        }
    }

    fun joinChallenge(challengeId: String) {
        viewModelScope.launch {
            val originalAvailableChallenges = _uiState.value.availableChallenges
            val originalUserChallenges = _uiState.value.userChallenges

            val challengeToJoin = originalAvailableChallenges.find { it.id == challengeId }

            challengeToJoin?.let { challenge ->
                val updatedParticipants = challenge.participants + currentUserId
                val updatedProgress = challenge.progress.toMutableMap().apply { this[currentUserId] = 0 }
                val updatedJoinDates = challenge.joinDate.toMutableMap().apply { this[currentUserId] = java.util.Date() }

                val optimisticChallenge = challenge.copy(
                    participants = updatedParticipants,
                    progress = updatedProgress,
                    joinDate = updatedJoinDates
                )

                // Optimistic UI Update
                val newAvailableChallenges = originalAvailableChallenges.filter { it.id != challengeId }
                val newUserChallenges = originalUserChallenges + optimisticChallenge

                _uiState.value = _uiState.value.copy(
                    availableChallenges = newAvailableChallenges,
                    userChallenges = newUserChallenges
                )

                try {
                    repository.joinChallenge(challengeId, currentUserId)
                } catch (e: Exception) {
                    // Revert optimistic update if backend call fails
                    _uiState.value = _uiState.value.copy(
                        availableChallenges = originalAvailableChallenges,
                        userChallenges = originalUserChallenges
                    )
                    // Optionally, show an error message to the user
                }
            }
        }
    }

    fun leaveChallenge(challengeId: String) {
        viewModelScope.launch {
            val originalAvailableChallenges = _uiState.value.availableChallenges
            val originalUserChallenges = _uiState.value.userChallenges

            val challengeToLeave = originalUserChallenges.find { it.id == challengeId }

            challengeToLeave?.let { challenge ->
                val updatedParticipants = challenge.participants.filter { it != currentUserId }
                val updatedProgress = challenge.progress.toMutableMap().also { it.remove(currentUserId) }
                val updatedJoinDates = challenge.joinDate.toMutableMap().also { it.remove(currentUserId) }

                val optimisticChallenge = challenge.copy(
                    participants = updatedParticipants,
                    progress = updatedProgress,
                    joinDate = updatedJoinDates
                )

                // Optimistic UI Update
                val newUserChallenges = originalUserChallenges.filter { it.id != challengeId }
                val newAvailableChallenges = originalAvailableChallenges.toMutableList().apply {
                    val index = indexOfFirst { it.id == optimisticChallenge.id }
                    if (index != -1) {
                        set(index, optimisticChallenge)
                    } else {
                        add(optimisticChallenge)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    userChallenges = newUserChallenges,
                    availableChallenges = newAvailableChallenges
                )

                try {
                    repository.leaveChallenge(challengeId, currentUserId)
                    refreshAvailableChallenges() // Force refresh after successful leave
                } catch (e: Exception) {
                    // Revert optimistic update if backend call fails
                    _uiState.value = _uiState.value.copy(
                        userChallenges = originalUserChallenges,
                        availableChallenges = originalAvailableChallenges
                    )
                    // Optionally, show an error message to the user
                }
            }
        }
    }

    fun onPagesRead(pagesRead: Int) {
        viewModelScope.launch {
            val userChallenges = _uiState.value.userChallenges
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
            userChallenges.filter { it.type == ChallengeType.DAILY_PAGES_READING }.forEach { challenge ->
                repository.updateDailyProgress(challenge.id, currentUserId, today)
            }
        }
    }

    
    suspend fun getChallengeProgress(challenge: Challenge): Pair<Int, Int> {
        return when (challenge.type) {
            ChallengeType.DAILY_PAGES_READING -> {
                val dailyGoalMetDays = getDailyGoalMetDaysFromActualData(challenge.goal)
                Pair(dailyGoalMetDays, 7)
            }
            ChallengeType.CONSECUTIVE_READING, 
            ChallengeType.CONSECUTIVE_READING_WITH_FRIEND -> {
                Pair(_uiState.value.consecutiveReadingDays, challenge.goal.takeIf { it > 0 } ?: 1)
            }
            else -> {
                Pair(challenge.progress[currentUserId] ?: 0, challenge.goal.takeIf { it > 0 } ?: 1)
            }
        }
    }

    private suspend fun getDailyGoalMetDaysFromActualData(goalPages: Int): Int {
        // Get last 7 days of actual reading data
        var goalMetDays = 0
        for (i in 0..6) {
            val date = java.time.LocalDate.now().minusDays(i.toLong())
            val dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            
            // Try to get actual pages read from user's daily reading data
            val actualPagesRead = getActualDailyPagesRead(dateStr)
            
            if (actualPagesRead >= goalPages) {
                goalMetDays++
            }
        }
        return goalMetDays
    }

    // Add a suspend function to get actual daily pages
    internal suspend fun getActualDailyPagesRead(dateStr: String): Int {
        return try {
            // Direct Firestore access without using FirestoreRepository private methods
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val dailyReadingRef = firestore.collection("users").document(currentUserId)
                .collection("daily_reading").document(dateStr)
            val snapshot = dailyReadingRef.get().await()
            snapshot.getLong("pagesRead")?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getDaysRemaining(challenge: Challenge): Int {
        return challenge.joinDate[currentUserId]?.let { joinDate ->
            val joinLocalDate = joinDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val todayLocalDate = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
            val elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(joinLocalDate, todayLocalDate).toInt()
            (7 - elapsedDays).coerceAtLeast(0)
        } ?: 0
    }
}
