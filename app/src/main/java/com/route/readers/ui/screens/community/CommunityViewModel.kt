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
        loadChallenges()
    }
    
    private fun loadFriends() {
        viewModelScope.launch {
            friendsRepository.loadFriends()
        }
    }
    
    private fun loadChallenges() {
        viewModelScope.launch {
            val challenges = challengeRepository.getChallenges()
            // 3가지 기본 챌린지 생성
            val defaultChallenges = listOf(
                Challenge(
                    id = "challenge_7days_reading",
                    title = "7일 연속 독서하기",
                    description = "일주일 동안 매일 책을 읽어보세요!",
                    type = com.route.readers.data.model.ChallengeType.CONSECUTIVE_READING_WITH_FRIEND,
                    participants = listOf(),
                    goal = 7,
                    progress = emptyMap(),
                    startDate = java.util.Date(),
                    endDate = java.util.Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)),
                    isCompleted = false,
                    reward = "경험치 100XP"
                ),
                Challenge(
                    id = "challenge_30pages_daily",
                    title = "하루 30페이지 읽기",
                    description = "매일 30페이지씩 읽어보세요!",
                    type = com.route.readers.data.model.ChallengeType.DAILY_PAGES_READING,
                    participants = listOf(),
                    goal = 7, // 7일 동안 매일 30페이지
                    progress = emptyMap(),
                    startDate = java.util.Date(),
                    endDate = java.util.Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)),
                    isCompleted = false,
                    reward = "경험치 150XP"
                ),
                Challenge(
                    id = "challenge_1book_weekly",
                    title = "1주일에 한 권 읽기",
                    description = "일주일 안에 책 한 권을 완독해보세요!",
                    type = com.route.readers.data.model.ChallengeType.CONSECUTIVE_READING_WITH_FRIEND,
                    participants = listOf(),
                    goal = 1,
                    progress = emptyMap(),
                    startDate = java.util.Date(),
                    endDate = java.util.Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)),
                    isCompleted = false,
                    reward = "경험치 200XP"
                )
            )
            
            val finalChallenges = if (challenges.isEmpty() || challenges.size < 3) {
                defaultChallenges.forEach { challengeRepository.createChallenge(it) }
                defaultChallenges
            } else {
                challenges
            }
            
            // 사용자가 참여 중인 챌린지 찾기
            val userChallenge = finalChallenges.find { it.participants.contains(currentUserId) }
            
            _uiState.value = _uiState.value.copy(
                challenges = finalChallenges,
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
