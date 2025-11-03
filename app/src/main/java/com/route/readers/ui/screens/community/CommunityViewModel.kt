package com.route.readers.ui.screens.community

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.Challenge
import com.route.readers.data.remote.AddFriendResult
import com.route.readers.data.remote.ChallengeRepository
import com.route.readers.data.remote.FriendsRepository
import com.route.readers.data.remote.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class Friend(
    val id: String = "",
    val name: String,
    val currentBook: String,
    val isOnline: Boolean,
    val lastActive: String
)

data class CommunityUiState(
    val friends: List<Friend> = emptyList(),
    val challenges: List<Challenge> = emptyList(),
    val userActiveChallenge: Challenge? = null,
    val addFriendMessage: String? = null,
    val friendToDelete: Friend? = null,
    val isNotificationSending: Boolean = false
) {
    val displayedFriends: List<Friend> = friends.take(5)
    val hasMoreFriends: Boolean = friends.size > 5
}

class CommunityViewModel(context: Context? = null) : ViewModel() {
    private val friendsRepository = FriendsRepository()
    private val notificationRepository = NotificationRepository(context)
    private val challengeRepository = ChallengeRepository()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            friendsRepository.friends.collect { friends ->
                _uiState.value = _uiState.value.copy(friends = friends)
            }
        }
        loadFriends()
        viewModelScope.launch {
            createWeeklyChallengesIfNeeded()
            loadChallenges()
        }
    }
    
    private fun loadFriends() {
        viewModelScope.launch {
            friendsRepository.loadFriends()
        }
    }
    
    private fun getCurrentWeekNumber(): Int {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val week = calendar.get(java.util.Calendar.WEEK_OF_YEAR)
        return year * 100 + week
    }
    
    private fun getThisMonday(): java.util.Date {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        val today = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (today == java.util.Calendar.SUNDAY) 6 else today - java.util.Calendar.MONDAY
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysFromMonday)
        
        return calendar.time
    }
    
    private fun getNextSunday(): java.util.Date {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = getThisMonday()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 6)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        
        return calendar.time
    }
    
    private suspend fun createWeeklyChallengesIfNeeded() {
        val existingChallenges = challengeRepository.getAvailableChallenges()
        if (existingChallenges.isEmpty()) {
            val startDate = getThisMonday()
            val endDate = getNextSunday()
            val weekNumber = getCurrentWeekNumber()
            
            val defaultChallenges = listOf(
                Challenge(
                    id = "challenge_${weekNumber}_1",
                    title = "매일 30페이지 읽기",
                    description = "하루에 30페이지씩 7일 동안 읽기",
                    type = com.route.readers.data.model.ChallengeType.DAILY_PAGES_READING,
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
                    type = com.route.readers.data.model.ChallengeType.CONSECUTIVE_READING_WITH_FRIEND,
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
                    type = com.route.readers.data.model.ChallengeType.DAILY_PAGES_READING,
                    goal = 50,
                    startDate = startDate,
                    endDate = endDate,
                    weekNumber = weekNumber,
                    reward = "100 토큰"
                )
            )
            
            defaultChallenges.forEach { challengeRepository.createChallenge(it) }
        }
    }
    
    private fun loadChallenges() {
        viewModelScope.launch {
            // 이번 주 챌린지 가져오기
            val availableChallenges = challengeRepository.getAvailableChallenges()
            
            // 사용자가 참여 중인 챌린지 찾기
            val userChallenge = challengeRepository.getUserActiveChallenge(currentUserId)
            
            _uiState.value = _uiState.value.copy(
                challenges = availableChallenges,
                userActiveChallenge = userChallenge
            )
        }
    }
    
    fun joinChallenge(challengeId: String) {
        viewModelScope.launch {
            challengeRepository.joinChallenge(challengeId, currentUserId)
            loadChallenges()
        }
    }
    
    fun resetChallenge() {
        viewModelScope.launch {
            _uiState.value.userActiveChallenge?.let { currentChallenge ->
                challengeRepository.leaveChallenge(currentChallenge.id, currentUserId)
            }
            loadChallenges()
        }
    }
    
    fun sendReadingNotification() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isNotificationSending = true)
            try {
                notificationRepository.sendReadingNotificationToFriends()
                _uiState.value = _uiState.value.copy(
                    addFriendMessage = "친구들에게 독서 알림을 보냈습니다! 📚",
                    isNotificationSending = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    addFriendMessage = "알림 전송에 실패했습니다. 다시 시도해주세요.",
                    isNotificationSending = false
                )
            }
        }
    }
    
    fun addFriend(friendName: String) {
        viewModelScope.launch {
            val result = friendsRepository.addFriend(friendName)
            val message = when (result) {
                is AddFriendResult.Success -> "친구가 추가되었습니다!"
                is AddFriendResult.UserNotFound -> "존재하지 않는 사용자입니다."
                is AddFriendResult.AlreadyFriend -> "이미 친구로 추가된 사용자입니다."
                is AddFriendResult.Error -> "오류가 발생했습니다: ${result.message}"
            }
            _uiState.value = _uiState.value.copy(addFriendMessage = message)
        }
    }
    
    fun showDeleteConfirmation(friend: Friend) {
        _uiState.value = _uiState.value.copy(friendToDelete = friend)
    }
    
    fun confirmDeleteFriend() {
        _uiState.value.friendToDelete?.let { friend ->
            viewModelScope.launch {
                friendsRepository.removeFriend(friend.id)
            }
        }
        _uiState.value = _uiState.value.copy(friendToDelete = null)
    }
    
    fun cancelDeleteFriend() {
        _uiState.value = _uiState.value.copy(friendToDelete = null)
    }
    
    fun clearAddFriendMessage() {
        _uiState.value = _uiState.value.copy(addFriendMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        friendsRepository.stopListening()
    }
}
