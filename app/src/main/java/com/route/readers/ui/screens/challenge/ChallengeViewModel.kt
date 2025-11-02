package com.route.readers.ui.screens.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.Challenge
import com.route.readers.data.remote.ChallengeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

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
    }

    private fun loadChallenges() {
        viewModelScope.launch {
            val challenges = repository.getChallenges()
            _uiState.value = _uiState.value.copy(challenges = challenges)
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    private fun getNextMonday(): java.util.Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val today = calendar.get(Calendar.DAY_OF_WEEK)
        val daysUntilMonday = if (today == Calendar.MONDAY) 0 else (Calendar.MONDAY - today + 7) % 7
        calendar.add(Calendar.DAY_OF_YEAR, daysUntilMonday)
        
        return calendar.time
    }

    fun createChallenge(title: String, description: String, goal: Int) {
        viewModelScope.launch {
            val startDate = getNextMonday()
            val calendar = Calendar.getInstance()
            calendar.time = startDate
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            val endDate = calendar.time
            
            val newChallenge = Challenge(
                id = System.currentTimeMillis().toString(),
                title = title,
                description = description,
                goal = goal,
                participants = listOf(currentUserId),
                startDate = startDate,
                endDate = endDate,
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
}
