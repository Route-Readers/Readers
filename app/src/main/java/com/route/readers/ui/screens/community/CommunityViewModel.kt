package com.route.readers.ui.screens.community

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.Prefs
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import java.util.concurrent.TimeUnit

// Friend data class removed, replaced by User

data class CommunityUiState(
    val friends: List<User> = emptyList(), // Changed to List<User>
    val bookClubs: List<BookClub> = emptyList(),
    val isBookClubsLoading: Boolean = true,
    val isFriendsLoading: Boolean = true,
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

class CommunityViewModel(application: Application) : AndroidViewModel(application) {
    private val friendsRepository = FriendsRepository()
    private val bookClubRepository = BookClubRepository()
    private val notificationRepository = NotificationRepository(null)
    private val challengeRepository = ChallengeRepository()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val sharedPreferences =
        application.getSharedPreferences(Prefs.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )


    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    init {
        val selectedChallengeId = sharedPreferences.getString(Prefs.KEY_SELECTED_CHALLENGE, null)
        selectedChallengeId?.let {
            loadSelectedChallenge(it)
        }


        viewModelScope.launch {
            friendsRepository.friends.collect { friends ->
                _uiState.value = _uiState.value.copy(
                    friends = friends,
                    isFriendsLoading = false
                )
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

        // Observe the active challenge from the repository's flow
        currentUserId.let { userId ->
            challengeRepository.userActiveChallenges
                .mapNotNull { it[userId] }
                .onEach { challenge ->
                    _uiState.value = _uiState.value.copy(
                        userActiveChallenge = challenge,
                        isChallengesLoading = false
                    )
                }
                .launchIn(viewModelScope)

            // Trigger initial refresh of active challenge in the repository
            challengeRepository.refreshUserActiveChallenge(userId)
        }

        loadFriends()
        refreshChallenges() // Load available challenges initially
    }

    private fun loadSelectedChallenge(challengeId: String) {
        viewModelScope.launch {
            try {
                val challenge = challengeRepository.getChallenge(challengeId)
                _uiState.value = _uiState.value.copy(
                    userActiveChallenge = challenge,
                    isChallengesLoading = false
                )
            } catch (e: Exception) {
                // Handle error
            }
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

    fun refreshChallenges() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChallengesLoading = true)
            try {
                val currentWeekNumber = getCurrentWeekNumber()
                val weeklyChallenges = challengeRepository.getChallengesForWeek(currentWeekNumber)
                _uiState.value = _uiState.value.copy(
                    availableChallenges = weeklyChallenges.filter { it.type != com.route.readers.data.model.ChallengeType.CUSTOM },
                    isChallengesLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isChallengesLoading = false)
            }
        }
    }

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
                sharedPreferences.edit().putString(Prefs.KEY_SELECTED_CHALLENGE, challengeId).apply()
                challengeRepository.refreshUserActiveChallenge(currentUserId)

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
                    sharedPreferences.edit().remove(Prefs.KEY_SELECTED_CHALLENGE).apply()
                    challengeRepository.refreshUserActiveChallenge(currentUserId)
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
