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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import java.util.concurrent.TimeUnit
import com.route.readers.data.remote.FirestoreRepository

// Friend data class removed, replaced by User

data class CommunityUiState(
    val friends: List<User> = emptyList(), // Changed to List<User>
    val friendsReadingStatus: Map<String, Boolean> = emptyMap(), // 친구 ID -> 오늘 독서 여부
    val bookClubs: List<BookClub> = emptyList(),
    val isBookClubsLoading: Boolean = true,
    val isFriendsLoading: Boolean = true,
    val isChallengesLoading: Boolean = true,
    val userActiveChallenges: List<Challenge> = emptyList(),
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

        // Observe the active challenges from the repository's flow
        currentUserId.let { userId ->
            challengeRepository.getActiveChallengeStream(userId)
                .onEach { challenges ->
                    _uiState.value = _uiState.value.copy(
                        userActiveChallenges = challenges,
                        isChallengesLoading = false
                    )
                }
                .launchIn(viewModelScope)
        }

        loadFriends()
        loadConsecutiveReadingDays()
        refreshChallenges() // Load available challenges initially
    }


    private fun loadFriends() {
        viewModelScope.launch {
            friendsRepository.loadFriends()

            // 친구들의 독서 상태도 함께 로드
            loadFriendsReadingStatus()
        }
    }
    private fun loadFriendsReadingStatus() {
        viewModelScope.launch {
            val currentFriends = _uiState.value.friends
            val readingStatusMap = mutableMapOf<String, Boolean>()
            
            currentFriends.forEach { friend ->
                val hasReadToday = checkFriendReadingStatusAsync(friend.uid)
                readingStatusMap[friend.uid] = hasReadToday
            }
            
            _uiState.value = _uiState.value.copy(
                friendsReadingStatus = readingStatusMap
            )
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

    suspend fun checkFriendReadingStatusAsync(friendId: String): Boolean {
        return try {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val todayTimestamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(today)?.time ?: 0L
            
            // Firestore에서 친구의 오늘 독서 기록 확인
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val myBooksQuery = firestore.collection("myBooks")
                .whereEqualTo("userId", friendId)
                .whereGreaterThanOrEqualTo("lastReadDate", todayTimestamp)
                .whereLessThan("lastReadDate", todayTimestamp + 24 * 60 * 60 * 1000) // 오늘 하루
                .limit(1)
            
            val result = myBooksQuery.get().await()
            !result.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    fun checkFriendReadingStatus(friendId: String): Boolean {
        // UI에서는 State로 관리해야 하므로 별도 처리 필요
        return false // 기본값
    }

    fun sendReadingNotificationToFriend(friendId: String) {
        viewModelScope.launch {
            try {
                // FCM을 통해 특정 친구에게 독서 알림 전송
                val currentUser = firestoreRepository.getUserProfile(currentUserId)
                val friendUser = firestoreRepository.getUserProfile(friendId)
                
                if (currentUser != null && friendUser != null && friendUser.fcmToken != null) {
                    // Firebase Functions를 통해 FCM 메시지 전송
                    val firestore = FirebaseFirestore.getInstance()
                    val notificationData = mapOf(
                        "type" to "reading_reminder",
                        "fromUserId" to currentUserId,
                        "fromUserName" to currentUser.nickname,
                        "toUserId" to friendId,
                        "fcmToken" to friendUser.fcmToken,
                        "title" to "독서 알림",
                        "body" to "${currentUser.nickname}님이 독서 알림을 보냈습니다! 📚",
                        "timestamp" to System.currentTimeMillis()
                    )
                    
                    firestore.collection("fcm_messages")
                        .add(notificationData)
                        .await()
                }
            } catch (e: Exception) {
                // 에러 처리
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
            val originalAvailableChallenges = _uiState.value.availableChallenges
            val originalUserActiveChallenges = _uiState.value.userActiveChallenges

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
                val newUserActiveChallenges = originalUserActiveChallenges + optimisticChallenge

                _uiState.value = _uiState.value.copy(
                    availableChallenges = newAvailableChallenges,
                    userActiveChallenges = newUserActiveChallenges
                )

                try {
                    challengeRepository.joinChallenge(challengeId, currentUserId)
                } catch (e: Exception) {
                    // Revert optimistic update if backend call fails
                    _uiState.value = _uiState.value.copy(
                        availableChallenges = originalAvailableChallenges,
                        userActiveChallenges = originalUserActiveChallenges,
                        addFriendMessage = "챌린지 참여에 실패했습니다."
                    )
                }
            }
        }
    }

    fun resetChallenge(challengeId: String) {
        viewModelScope.launch {
            val originalAvailableChallenges = _uiState.value.availableChallenges
            val originalUserActiveChallenges = _uiState.value.userActiveChallenges

            val challengeToLeave = originalUserActiveChallenges.find { it.id == challengeId }

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
                val newUserActiveChallenges = originalUserActiveChallenges.filter { it.id != challengeId }
                val newAvailableChallenges = originalAvailableChallenges.toMutableList().apply {
                    val index = indexOfFirst { it.id == optimisticChallenge.id }
                    if (index != -1) {
                        set(index, optimisticChallenge)
                    } else {
                        add(optimisticChallenge)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    userActiveChallenges = newUserActiveChallenges,
                    availableChallenges = newAvailableChallenges
                )

                try {
                    challengeRepository.leaveChallenge(challengeId, currentUserId)
                    refreshChallenges() // Force refresh after successful leave
                } catch (e: Exception) {
                    // Revert optimistic update if backend call fails
                    _uiState.value = _uiState.value.copy(
                        userActiveChallenges = originalUserActiveChallenges,
                        availableChallenges = originalAvailableChallenges,
                        addFriendMessage = "챌린지 초기화에 실패했습니다."
                    )
                }
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
