package com.route.readers.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.BookClub
import com.route.readers.data.model.Challenge
import com.route.readers.data.model.User // Added User import
import com.route.readers.data.remote.BookClubRepository
import com.route.readers.data.remote.ChallengeRepository
import com.route.readers.data.remote.FriendsRepository
import com.route.readers.data.remote.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// Friend data class removed, replaced by User

data class CommunityUiState(
    val friends: List<User> = emptyList(), // Changed to List<User>
    val bookClubs: List<BookClub> = emptyList(),
    val isBookClubsLoading: Boolean = true,
    val isChallengesLoading: Boolean = true,
    val userActiveChallenge: Challenge? = null,
    val availableChallenges: List<Challenge> = emptyList(),
    val addFriendMessage: String? = null,
    val friendToDelete: User? = null, // Changed to User?
    val isNotificationSending: Boolean = false
) {
    val displayedFriends: List<User> = friends.take(5) // Changed to List<User>
    val hasMoreFriends: Boolean = friends.size > 5
}

class CommunityViewModel : ViewModel() {
    private val friendsRepository = FriendsRepository()
    private val bookClubRepository = BookClubRepository()
    private val notificationRepository = NotificationRepository(null)
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
        viewModelScope.launch {
            bookClubRepository.getAllBookClubsFlow().collect { bookClubs ->
                val bookClubsWithJoinStatus = bookClubs.map { bookClub ->
                    bookClub.copy(isJoined = bookClub.members.contains(currentUserId))
                }
                _uiState.value = _uiState.value.copy(
                    bookClubs = bookClubsWithJoinStatus,
                    isBookClubsLoading = false
                )
            }
        }
        loadFriends()
        initChallenges()
    }
    
    private fun loadFriends() {
        viewModelScope.launch {
            friendsRepository.loadFriends()
        }
    }
    
    private fun getCurrentWeekNumber(): Int {
        val calendar = java.util.Calendar.getInstance()
        return calendar.get(java.util.Calendar.WEEK_OF_YEAR)
    }
    
    private fun initChallenges() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChallengesLoading = true)
            
            try {
                val userChallenge = challengeRepository.getUserActiveChallenge(currentUserId)
                val availableChallenges = challengeRepository.getChallenges()
                
                _uiState.value = _uiState.value.copy(
                    userActiveChallenge = userChallenge,
                    availableChallenges = availableChallenges,
                    isChallengesLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isChallengesLoading = false)
            }
        }
    }
    
    fun refreshChallenges() {
        initChallenges()
    }
    
    // addFriend function removed, as friendship is now managed by mutual following.
    // If a follow action is needed from UI, it should call friendsRepository.followUser.
    
    fun sendReadingNotification() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isNotificationSending = true)
            
            try {
                notificationRepository.sendReadingNotificationToFriends()
                _uiState.value = _uiState.value.copy(
                    addFriendMessage = "친구들에게 독서 알림을 보냈습니다!",
                    isNotificationSending = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    addFriendMessage = "알림 전송에 실패했습니다.",
                    isNotificationSending = false
                )
            }
        }
    }
    
    fun joinChallenge(challengeId: String) {
        viewModelScope.launch {
            try {
                challengeRepository.joinChallenge(challengeId, currentUserId)
                refreshChallenges()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    addFriendMessage = "챌린지 참여에 실패했습니다."
                )
            }
        }
    }
    
    fun resetChallenge() {
        viewModelScope.launch {
            try {
                _uiState.value.userActiveChallenge?.let { challenge ->
                    challengeRepository.leaveChallenge(challenge.id, currentUserId)
                    refreshChallenges()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    addFriendMessage = "챌린지 초기화에 실패했습니다."
                )
            }
        }
    }
    
    fun showDeleteConfirmation(friend: User) { // Changed parameter to User
        _uiState.value = _uiState.value.copy(friendToDelete = friend)
    }
    
    fun confirmDeleteFriend() {
        _uiState.value.friendToDelete?.let { userToDelete -> // Changed to userToDelete
            viewModelScope.launch {
                friendsRepository.unfollowUser(userToDelete.uid) // Call unfollowUser with user UID
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
    
    // 북클럽 관련 함수들
    fun createBookClub(name: String, description: String, bookTitle: String, author: String, meetingDate: String) {
        viewModelScope.launch {
            val bookClub = BookClub(
                name = name,
                description = description,
                currentBook = bookTitle,
                currentBookAuthor = author,
                nextMeetingDate = meetingDate,
                memberCount = 1,
                members = listOf(currentUserId),
                createdBy = currentUserId,
                createdAt = System.currentTimeMillis(),
                bookTitle = bookTitle
            )
            bookClubRepository.createBookClub(bookClub)
        }
    }
    
    fun joinBookClub(bookClubId: String) {
        viewModelScope.launch {
            bookClubRepository.joinBookClub(bookClubId, currentUserId)
        }
    }
    
    fun leaveBookClub(bookClubId: String) {
        viewModelScope.launch {
            bookClubRepository.leaveBookClub(bookClubId, currentUserId)
        }
    }
    
    fun deleteBookClub(bookClubId: String) {
        viewModelScope.launch {
            bookClubRepository.deleteBookClub(bookClubId, currentUserId)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        friendsRepository.stopListening()
    }
}