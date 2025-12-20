package com.route.readers.ui.screens.community

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import java.util.concurrent.TimeUnit
import com.route.readers.data.remote.FirestoreRepository

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
    val isNotificationSending: Boolean = false,
    val consecutiveReadingDays: Int = 0
) {
    val displayedFriends: List<User> = friends.take(5) // Changed to List<User>
    val hasMoreFriends: Boolean = friends.size > 5
}

class CommunityViewModel(application: Application) : AndroidViewModel(application) {
    private val friendsRepository = FriendsRepository()
    private val bookClubRepository = BookClubRepository()
    private val notificationRepository = NotificationRepository(null)
    private val firestoreRepository = FirestoreRepository()
    private val challengeRepository = ChallengeRepository(firestoreRepository)
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
            challengeRepository.getActiveChallengeStream(userId)
                .onEach { challenge ->
                    _uiState.value = _uiState.value.copy(
                        userActiveChallenge = challenge,
                        isChallengesLoading = false
                    )
                }
                .launchIn(viewModelScope)
        }

        loadFriends()
        loadConsecutiveReadingDays()
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

    private fun loadConsecutiveReadingDays() {
        viewModelScope.launch {
            try {
                val user = firestoreRepository.getUserProfile(currentUserId)
                val consecutiveDays = user?.consecutiveReadingDays ?: 0
                _uiState.value = _uiState.value.copy(consecutiveReadingDays = consecutiveDays)
            } catch (e: Exception) {
                // Handle error silently
            }
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

    fun getChallengeProgress(challenge: Challenge): Pair<Int, Int> {
        return when (challenge.type) {
            com.route.readers.data.model.ChallengeType.DAILY_PAGES_READING -> {
                // Get actual daily reading data from Firestore
                val dailyGoalMetDays = getDailyGoalMetDaysFromActualData(challenge.goal)
                Pair(dailyGoalMetDays, 7)
            }
            com.route.readers.data.model.ChallengeType.CONSECUTIVE_READING,
            com.route.readers.data.model.ChallengeType.CONSECUTIVE_READING_WITH_FRIEND -> {
                Pair(_uiState.value.consecutiveReadingDays, 7)
            }
            else -> {
                Pair(challenge.progress[currentUserId] ?: 0, challenge.goal.takeIf { it > 0 } ?: 1)
            }
        }
    }

    private fun getDailyGoalMetDaysFromActualData(goalPages: Int): Int {
        // Get last 7 days of actual reading data
        var goalMetDays = 0
        for (i in 0..6) {
            val date = java.time.LocalDate.now().minusDays(i.toLong())
            val dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            
            // Try to get actual pages read from user's daily reading data
            // This would need to be implemented to fetch from Firestore daily_reading collection
            val actualPagesRead = getActualPagesReadForDate(dateStr)
            
            if (actualPagesRead >= goalPages) {
                goalMetDays++
            }
        }
        return goalMetDays
    }

    private fun getActualPagesReadForDate(dateStr: String): Int {
        // Use a simple approach - try to get from user's total pages read
        // This is a synchronous approximation since we can't use suspend functions here
        return try {
            // For now, return a default value
            // In a real implementation, this would need to be refactored to use suspend functions
            0
        } catch (e: Exception) {
            0
        }
    }

    // Add a suspend function to get actual daily pages
    suspend fun getActualDailyPagesRead(dateStr: String): Int {
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

    // 북클럽 관련 함수들
    fun createBookClub(name: String, description: String, bookTitle: String, author: String, meetingDate: String, bookCover: String = "", bookGenre: String = "", bookDescription: String = "") {
        viewModelScope.launch {
            val bookClub = BookClub(
                name = name,
                description = description,
                currentBook = bookTitle,
                currentBookAuthor = author,
                currentBookCover = bookCover,
                currentBookGenre = bookGenre,
                currentBookDescription = bookDescription,
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
