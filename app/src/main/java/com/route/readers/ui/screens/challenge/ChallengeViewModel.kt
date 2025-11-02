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

    fun createChallenge(title: String, description: String, goal: Int) {
        viewModelScope.launch {
            val newChallenge = Challenge(
                id = System.currentTimeMillis().toString(),
                title = title,
                description = description,
                goal = goal,
                participants = listOf(currentUserId),
                startDate = java.util.Date(),
                endDate = java.util.Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000),
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
