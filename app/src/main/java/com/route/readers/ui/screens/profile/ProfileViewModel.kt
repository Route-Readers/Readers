package com.route.readers.ui.screens.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.route.readers.data.model.Book
import com.route.readers.data.model.MyBook
import com.route.readers.data.model.User
import com.route.readers.data.remote.BookRepository
import com.route.readers.data.remote.FirestoreRepository
import com.route.readers.data.remote.MyLibraryRepository
import com.route.readers.data.remote.WishlistRepository
import com.route.readers.ui.screens.attendance.AttendanceViewModel
import com.route.readers.ui.screens.feed.FeedItem
import com.route.readers.ui.screens.feed.toFeedItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

open class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val bookRepository = BookRepository()
    private val wishlistRepository = WishlistRepository()
    private val myLibraryRepository = MyLibraryRepository()
    private val firestoreRepository = FirestoreRepository()
    private val attendanceViewModel = AttendanceViewModel()
    private val currentUserId = auth.currentUser?.uid

    protected val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _setupState = MutableStateFlow<ProfileSetupState>(ProfileSetupState.Idle)
    val setupState: StateFlow<ProfileSetupState> = _setupState

    private val _blockedUsers = MutableStateFlow<List<User>>(emptyList())
    val blockedUsers: StateFlow<List<User>> = _blockedUsers.asStateFlow()

    fun checkNicknameAvailability(nickname: String) {
        if (nickname.length !in 2..12) {
            _setupState.value = ProfileSetupState.Error("닉네임은 2~12자 사이로 입력해주세요.")
            return
        }
        _setupState.value = ProfileSetupState.Loading
        viewModelScope.launch {
            try {
                val documents =
                    db.collection("users").whereEqualTo("nickname", nickname).get().await()
                if (documents.isEmpty) {
                    _setupState.value = ProfileSetupState.Error("사용 가능한 닉네임입니다.")
                } else {
                    _setupState.value = ProfileSetupState.Error("이미 사용 중인 닉네임입니다.")
                }
            } catch (e: Exception) {
                _setupState.value = ProfileSetupState.Error("오류 발생: ${e.message}")
            }
        }
    }

    fun createOrUpdateUserProfile(
        nickname: String,
        profileImageUri: Uri?,
        genres: List<String>,
        styles: List<String>
    ) {
        if (currentUserId == null) {
            _setupState.value = ProfileSetupState.Error("사용자 인증 정보가 없습니다. 다시 로그인해주세요.")
            return
        }
        _setupState.value = ProfileSetupState.Loading
        viewModelScope.launch {
            try {
                val imageUrl = if (profileImageUri != null) {
                    uploadProfileImage(profileImageUri)
                } else {
                    null
                }
                saveUserData(nickname, imageUrl, genres, styles)
            } catch (e: Exception) {
                _setupState.value = ProfileSetupState.Error("프로필 설정에 실패했습니다: ${e.message}")
            }
        }
    }

    private suspend fun uploadProfileImage(imageUri: Uri): String {
        val storageRef = storage.reference
        val imageFileName = "${currentUserId}-${UUID.randomUUID()}.jpg"
        val imagesRef = storageRef.child("profile_images/$imageFileName")
        imagesRef.putFile(imageUri).await()
        return imagesRef.downloadUrl.await().toString()
    }

        private suspend fun saveUserData(

            nickname: String,

            profileImageUrl: String?,

            genres: List<String>,

            styles: List<String>

        ) {

            if (currentUserId == null) throw IllegalStateException("Current User ID is null")

            val userProfileData = mapOf(

                "uid" to currentUserId,

                "nickname" to nickname,

                "email" to auth.currentUser?.email,

                "profileImageUrl" to profileImageUrl,

                "readingGenres" to genres,

                "readingStyles" to styles,

                "savedFeeds" to emptyList<String>(),

                "blockedUsers" to emptyList<String>(),

                "level" to 1,

                "title" to "새싹",

                "titles" to listOf("새싹"),

                "totalPoints" to 0,

                "claimedAchievements" to emptyList<String>(),

                "followerCount" to 0,

                "followingCount" to 0,

                "readBookCount" to 0,

                "followers" to emptyList<String>(),

                "following" to emptyList<String>(),

                "isCurrentlyReading" to false,

                "isPrivate" to false,

                "consecutiveDays" to 0,

                "consecutiveReadingDays" to 0,

                "totalReadingDays" to 0

            )

            db.collection("users").document(currentUserId).set(userProfileData).await()

            _setupState.value = ProfileSetupState.Success

        }

    

        open fun fetchUserProfile(userId: String?) {

            val targetUserId = userId ?: currentUserId

            if (targetUserId == null) {

                _uiState.value = ProfileUiState.Error("사용자 정보를 찾을 수 없습니다.")

                return

            }

            _uiState.value = ProfileUiState.Loading

            viewModelScope.launch {

                try {

                    val (consecutiveAttendanceDays, consecutiveReadingDays) = attendanceViewModel.refreshAttendanceData()

    

                    val userDocument = db.collection("users").document(targetUserId).get().await()

                    var user: User? = userDocument.toObject(User::class.java)

    

                    if (user != null) {

                        val isMyProfile = targetUserId == currentUserId

                        val isFollowing =

                            if (currentUserId != null) user.followers.contains(currentUserId) else false

    

                        val readBooksDeferred = async { firestoreRepository.getReadBooks(targetUserId) }

                        val readBooks = readBooksDeferred.await()

                        val actualReadBookCount = readBooks.size.toLong()

    

                        val updates = mutableMapOf<String, Any>()

                        var userNeedsUpdate = false

    

                        if (user.readBookCount != actualReadBookCount) {

                            updates["readBookCount"] = actualReadBookCount

                            userNeedsUpdate = true

                        }

    

                        if (user.consecutiveDays != consecutiveAttendanceDays) {

                            updates["consecutiveDays"] = consecutiveAttendanceDays

                            userNeedsUpdate = true

                        }

                        if (user.consecutiveReadingDays != consecutiveReadingDays) {

                            updates["consecutiveReadingDays"] = consecutiveReadingDays

                            userNeedsUpdate = true

                        }

    

                        if (userNeedsUpdate) {

                            db.collection("users").document(targetUserId).update(updates).await()

                        }

    

                        user = user.copy(

                            readBookCount = actualReadBookCount,

                            consecutiveDays = consecutiveAttendanceDays,

                            consecutiveReadingDays = consecutiveReadingDays

                        )

    

                        val allAchievements = getAchievementsForUser(user.readBookCount.toInt(), consecutiveAttendanceDays, consecutiveReadingDays)

                        val (ongoingAchievements, completedAchievements) = allAchievements.partition { !it.isCompleted }

    

                        if (user.isPrivate && !isMyProfile && !isFollowing) {

                            _uiState.value = ProfileUiState.Success(

                                user = user,

                                isFollowing = isFollowing,

                                isMyProfile = isMyProfile,

                                isBlocked = false,

                                readBooks = emptyList(),

                                recommendedBooks = emptyList(),

                                favoriteBooks = emptyList(),

                                achievements = allAchievements,

                                ongoingChallenges = emptyList(),

                                completedChallenges = emptyList(),

                                ongoingAchievements = ongoingAchievements,

                                completedAchievements = completedAchievements,

                                myPosts = emptyList(),

                                savedPosts = emptyList(),

                                likedFeedIds = emptySet(),

                                bookmarkedFeedIds = emptySet(),

                                wishlist = emptyList(),

                                myLibrary = emptyList(),

                                myLibraryBooks = emptyList(),

                                userInfoMap = emptyMap()

                            )

                            return@launch

                        }

    

                        val currentUserDoc =

                            currentUserId?.let { db.collection("users").document(it).get().await() }

                        val currentUserBlocked =

                            currentUserDoc?.toObject(User::class.java)?.blockedUsers ?: emptyList()

                        val isBlocked = currentUserBlocked.contains(targetUserId)

    

                        val validFollowers = user.followers.filter { it != targetUserId }

                        val validFollowing = user.following.filter { it != targetUserId }

                        val actualFollowerCount = validFollowers.size.toLong()

                        val actualFollowingCount = validFollowing.size.toLong()

    

                        if (user.followerCount != actualFollowerCount || user.followingCount != actualFollowingCount) {

                            db.collection("users").document(targetUserId)

                                .update(

                                    mapOf(

                                        "followerCount" to actualFollowerCount,

                                        "followingCount" to actualFollowingCount

                                    )

                                ).await()

                        }

    

                        val updatedUser = user.copy(

                            followers = validFollowers,

                            following = validFollowing,

                            followerCount = actualFollowerCount,

                            followingCount = actualFollowingCount

                        )

                        val recommendedBooksDeferred = async { fetchRecommendedBooks(updatedUser.readingGenres) }

                        val myPostsDeferred = async { fetchMyPosts(targetUserId) }

    

                        val wishlistBooksDeferred = async {

                            val userWishlistIsbns = wishlistRepository.getWishlist()

                            userWishlistIsbns.mapNotNull { isbn ->

                                bookRepository.getBookDetail(isbn)

                            }

                        }

    

                        val savedPostsResult = async {

                            if (isMyProfile) fetchSavedPosts(targetUserId) else emptyList()

                        }.await()

    

                        val allPosts = (myPostsDeferred.await() + savedPostsResult).distinctBy { it.id }

    

                        val authorIds = allPosts.mapNotNull {

                            when (it) {

                                is FeedItem.BookReview -> it.authorId

                                is FeedItem.FollowNotification -> it.authorId

                                else -> null

                            }

                        }.distinct()

    

                        val userInfoMap = if (authorIds.isNotEmpty()) {

                            db.collection("users").whereIn("uid", authorIds).get().await()

                                .toObjects(User::class.java)

                                .associateBy { it.uid }

                                .toMutableMap()

                        } else {

                            mutableMapOf()

                        }

                        userInfoMap[updatedUser.uid] = updatedUser

    

                        val likedFeedIds = allPosts

                            .filterIsInstance<FeedItem.BookReview>()

                            .filter { it.likedBy.contains(currentUserId) }

                            .map { it.id }

                            .toSet()

    

                        val bookmarkedFeedIds = allPosts

                            .filter { it.isBookmarked }

                            .map { it.id }

                            .toSet()

    

                        val wishlist = wishlistRepository.getWishlist()

                        val myLibraryBooks = myLibraryRepository.getMyBooks()

                        val myLibraryIsbns = myLibraryBooks.map { it.isbn }

    

                        _uiState.value = ProfileUiState.Success(

                            user = updatedUser,

                            isFollowing = isFollowing,

                            isMyProfile = isMyProfile,

                            isBlocked = isBlocked,

                            readBooks = readBooks,

                            recommendedBooks = recommendedBooksDeferred.await(),

                            favoriteBooks = wishlistBooksDeferred.await(),

                            achievements = allAchievements,

                            ongoingChallenges = emptyList(),

                            completedChallenges = emptyList(),

                            ongoingAchievements = ongoingAchievements,

                            completedAchievements = completedAchievements,

                            myPosts = myPostsDeferred.await(),

                            savedPosts = savedPostsResult,

                            likedFeedIds = likedFeedIds,

                            bookmarkedFeedIds = bookmarkedFeedIds,

                            wishlist = wishlist,

                            myLibrary = myLibraryIsbns,

                            myLibraryBooks = myLibraryBooks,

                            userInfoMap = userInfoMap

                        )

                    } else {

                        _uiState.value = ProfileUiState.Error("프로필 정보를 변환하는 데 실패했습니다.")

                    }

                } catch (e: Exception) {

                    _uiState.value = ProfileUiState.Error("프로필을 불러오는 중 오류가 발생했습니다: ${e.message}")

                    Log.e("ProfileViewModel", "fetchUserProfile failed", e)

                }

            }

        }

    

        fun claimAchievementPoints(achievementId: String, points: Int) {

            val currentUserId = this.currentUserId ?: return

            val currentState = _uiState.value

            if (currentState !is ProfileUiState.Success) return

    

            viewModelScope.launch {

                try {

                    val userRef = db.collection("users").document(currentUserId)

    

                    db.runTransaction { transaction ->

                        val snapshot = transaction.get(userRef)

                        val currentPoints = snapshot.getLong("totalPoints")?.toInt() ?: 0

                        val newTotalPoints = currentPoints + points

    

                        transaction.update(userRef, "totalPoints", newTotalPoints)

                        transaction.update(userRef, "claimedAchievements", FieldValue.arrayUnion(achievementId))

                    }.await()

    

                    val updatedUser = currentState.user.copy(

                        totalPoints = currentState.user.totalPoints + points,

                        claimedAchievements = currentState.user.claimedAchievements + achievementId

                    )

                    _uiState.value = currentState.copy(user = updatedUser)

    

                } catch (e: Exception) {

                    Log.e("ProfileViewModel", "Failed to claim achievement points", e)

                }

            }

        }

    

            fun updateUserTitle(title: String) {

    

                val currentUserId = this.currentUserId ?: return

    

                val currentState = _uiState.value

    

                if (currentState !is ProfileUiState.Success) return

    

        

    

                viewModelScope.launch {

    

                    try {

    

                        val userRef = db.collection("users").document(currentUserId)

    

                        val newTitle = if (currentState.user.title == title) null else title

    

        

    

                        db.runTransaction { transaction ->

    

                            transaction.update(userRef, "title", newTitle)

    

                            if (newTitle != null) {

    

                                transaction.update(userRef, "titles", FieldValue.arrayUnion(title))

    

                            }

    

                        }.await()

    

        

    

                        val updatedUser = currentState.user.copy(title = newTitle)

    

                        _uiState.value = currentState.copy(user = updatedUser)

    

        

    

                    } catch (e: Exception) {

    

                        Log.e("ProfileViewModel", "Failed to update user title", e)

    

                    }

    

                }

    

            }

    private fun getAchievementsForUser(
        readBookCount: Int,
        consecutiveAttendanceDays: Int,
        consecutiveReadingDays: Int
    ): List<Achievement> {
        return listOf(
            Achievement(
                id = "read_1",
                title = "책 1권 읽기",
                description = "첫 번째 책을 완독하세요",
                category = "독서",
                currentProgress = readBookCount,
                targetProgress = 1
            ),
            Achievement(
                id = "read_10",
                title = "책 10권 읽기",
                description = "10권의 책을 완독하세요",
                category = "독서",
                currentProgress = readBookCount,
                targetProgress = 10
            ),
            Achievement(
                id = "read_50",
                title = "책 50권 읽기",
                description = "50권의 책을 완독하세요",
                category = "독서",
                currentProgress = readBookCount,
                targetProgress = 50
            ),
            Achievement(
                id = "read_100",
                title = "책 100권 읽기",
                description = "100권의 책을 완독하세요",
                category = "독서",
                currentProgress = readBookCount,
                targetProgress = 100
            ),
            Achievement(
                id = "attendance_7",
                title = "연속 7일 출석",
                description = "7일 연속으로 출석하세요",
                category = "출석",
                currentProgress = consecutiveAttendanceDays,
                targetProgress = 7
            ),
            Achievement(
                id = "attendance_30",
                title = "연속 30일 출석",
                description = "30일 연속으로 출석하세요",
                category = "출석",
                currentProgress = consecutiveAttendanceDays,
                targetProgress = 30
            ),
            Achievement(
                id = "attendance_100",
                title = "연속 100일 출석",
                description = "100일 연속으로 출석하세요",
                category = "출석",
                currentProgress = consecutiveAttendanceDays,
                targetProgress = 100
            ),
            Achievement(
                id = "reading_7",
                title = "연속 7일 독서",
                description = "7일 연속으로 독서하세요",
                category = "독서",
                currentProgress = consecutiveReadingDays,
                targetProgress = 7
            ),
            Achievement(
                id = "reading_30",
                title = "연속 30일 독서",
                description = "30일 연속으로 독서하세요",
                category = "독서",
                currentProgress = consecutiveReadingDays,
                targetProgress = 30
            ),
            Achievement(
                id = "reading_100",
                title = "연속 100일 독서",
                description = "100일 연속으로 독서하세요",
                category = "독서",
                currentProgress = consecutiveReadingDays,
                targetProgress = 100
            )
        )
    }

    private suspend fun fetchMyPosts(userId: String): List<FeedItem> {
        return try {
            val snapshot = db.collection("feeds")
                .whereEqualTo("authorId", userId)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toFeedItem() }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Failed to fetch my posts", e)
            emptyList()
        }
    }

    private suspend fun fetchSavedPosts(userId: String): List<FeedItem> {
        return try {
            val snapshot = db.collection("feeds")
                .whereArrayContains("bookmarkedBy", userId)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toFeedItem() }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Failed to fetch saved posts", e)
            emptyList()
        }
    }

    fun updateProfileImage(imageUri: Uri) {
        if (currentUserId == null) return
        val currentState = _uiState.value
        if (currentState !is ProfileUiState.Success || !currentState.isMyProfile) return
        viewModelScope.launch {
            try {
                val imageUrl = uploadProfileImage(imageUri)
                db.collection("users").document(currentUserId).update("profileImageUrl", imageUrl)
                    .await()
                val updatedUser = currentState.user.copy(profileImageUrl = imageUrl)
                _uiState.value = currentState.copy(user = updatedUser)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update profile image", e)
            }
        }
    }

    fun updateProfileCharacter(character: String?, backgroundColor: String?) {
        if (currentUserId == null) return
        val currentState = _uiState.value
        if (currentState !is ProfileUiState.Success || !currentState.isMyProfile) return
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any?>()
                updates["profileCharacter"] = character
                updates["profileBackgroundColor"] = backgroundColor
                if (character != null) {
                    updates["profileImageUrl"] = null
                }
                db.collection("users").document(currentUserId).update(updates).await()
                val updatedUser = currentState.user.copy(
                    profileCharacter = character,
                    profileBackgroundColor = backgroundColor,
                    profileImageUrl = if (character != null) null else currentState.user.profileImageUrl
                )
                _uiState.value = currentState.copy(
                    user = updatedUser,
                    userInfoMap = currentState.userInfoMap + (updatedUser.uid to updatedUser)
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update profile character", e)
            }
        }
    }

    private suspend fun fetchRecommendedBooks(genres: List<String>): List<Book> {
        if (genres.isEmpty()) return emptyList()
        return try {
            coroutineScope {
                val deferredBookLists = genres.map { genre ->
                    async { bookRepository.getBookSearch(query = genre, maxResults = 3) }
                }
                deferredBookLists.awaitAll().flatMap { it }.distinctBy { it.isbn }
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "추천 도서 API 호출 실패", e)
            emptyList()
        }
    }

    fun followUser(targetUserId: String) {
        if (currentUserId == null || currentUserId == targetUserId) return
        refreshUiStateForFollow(targetUserId, true)
        viewModelScope.launch {
            try {
                val targetUserRef = db.collection("users").document(targetUserId)
                val currentUserRef = db.collection("users").document(currentUserId)
                val currentUserName =
                    currentUserRef.get().await().getString("nickname") ?: "알 수 없음"
                db.runBatch { batch ->
                    batch.update(targetUserRef, "followers", FieldValue.arrayUnion(currentUserId))
                    batch.update(targetUserRef, "followerCount", FieldValue.increment(1))
                    batch.update(
                        currentUserRef,
                        "following",
                        FieldValue.arrayUnion(targetUserId)
                    )
                    batch.update(currentUserRef, "followingCount", FieldValue.increment(1))
                }.await()
                val followNotification = hashMapOf(
                    "type" to "FOLLOW_NOTIFICATION",
                    "authorId" to currentUserId,
                    "userName" to currentUserName,
                    "followerId" to currentUserId,
                    "receiverId" to targetUserId,
                    "isFollowedBack" to false,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "likeCount" to 0,
                    "commentCount" to 0
                )
                db.collection("feeds").add(followNotification).await()
            } catch (e: Exception) {
                refreshUiStateForFollow(targetUserId, false)
            }
        }
    }

    fun unfollowUser(targetUserId: String) {
        if (currentUserId == null || currentUserId == targetUserId) return
        refreshUiStateForFollow(targetUserId, false)
        viewModelScope.launch {
            try {
                val targetUserRef = db.collection("users").document(targetUserId)
                val currentUserRef = db.collection("users").document(currentUserId)
                db.runBatch { batch ->
                    batch.update(
                        targetUserRef,
                        "followers",
                        FieldValue.arrayRemove(currentUserId)
                    )
                    batch.update(targetUserRef, "followerCount", FieldValue.increment(-1))
                    batch.update(
                        currentUserRef,
                        "following",
                        FieldValue.arrayRemove(targetUserId)
                    )
                    batch.update(currentUserRef, "followingCount", FieldValue.increment(-1))
                }.await()
            } catch (e: Exception) {
                refreshUiStateForFollow(targetUserId, true)
            }
        }
    }

    private fun refreshUiStateForFollow(targetUserId: String, nowFollowing: Boolean) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            val user = currentState.user
            if (!currentState.isMyProfile) {
                val newFollowerCount =
                    if (nowFollowing) user.followerCount + 1 else (user.followerCount - 1).coerceAtLeast(
                        0
                    )
                _uiState.value = currentState.copy(
                    isFollowing = nowFollowing,
                    user = user.copy(followerCount = newFollowerCount)
                )
            } else {
                val newFollowingCount =
                    if (nowFollowing) user.followingCount + 1 else (user.followingCount - 1).coerceAtLeast(
                        0
                    )
                _uiState.value =
                    currentState.copy(user = user.copy(followingCount = newFollowingCount))
            }
        } else {
            fetchUserProfile(targetUserId)
        }
    }

    fun toggleBookSelection(bookId: String) {
        (_uiState.value as? ProfileUiState.Success)?.let { currentState ->
            val newSelectedIds = if (currentState.selectedBookIds.contains(bookId)) {
                currentState.selectedBookIds - bookId
            } else {
                currentState.selectedBookIds + bookId
            }
            _uiState.value = currentState.copy(selectedBookIds = newSelectedIds)
            if (newSelectedIds.isEmpty()) {
                _uiState.value =
                    currentState.copy(isSelectionMode = false, selectedBookIds = emptySet())
            }
        }
    }

    fun startSelectionMode(bookId: String) {
        (_uiState.value as? ProfileUiState.Success)?.let { currentState ->
            _uiState.value =
                currentState.copy(isSelectionMode = true, selectedBookIds = setOf(bookId))
        }
    }

    fun clearSelectionMode() {
        (_uiState.value as? ProfileUiState.Success)?.let { currentState ->
            _uiState.value =
                currentState.copy(isSelectionMode = false, selectedBookIds = emptySet())
        }
    }

    fun deleteSelectedFavoriteBooks() {
        val currentState = (_uiState.value as? ProfileUiState.Success) ?: return
        val bookIdsToDelete = currentState.selectedBookIds
        if (bookIdsToDelete.isEmpty()) return
        viewModelScope.launch {
            try {
                bookIdsToDelete.forEach { isbn ->
                    wishlistRepository.removeFromWishlist(isbn)
                }

                val updatedBooks =
                    currentState.favoriteBooks.filterNot { it.isbn in bookIdsToDelete }
                val updatedWishlistIsbns =
                    currentState.wishlist.filterNot { it in bookIdsToDelete }
                _uiState.value = currentState.copy(
                    favoriteBooks = updatedBooks,
                    wishlist = updatedWishlistIsbns,
                    isSelectionMode = false,
                    selectedBookIds = emptySet()
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to delete favorite books", e)
            }
        }
    }

    fun toggleLike(feedId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val feedRef = db.collection("feeds").document(feedId)
            try {
                val operation = if (isCurrentlyLiked) {
                    FieldValue.arrayRemove(currentUserId) to FieldValue.increment(-1)
                } else {
                    FieldValue.arrayUnion(currentUserId) to FieldValue.increment(1)
                }
                feedRef.update("likedBy", operation.first, "likeCount", operation.second).await()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error toggling like for feed $feedId", e)
            }
        }
    }

    fun toggleBookmark(feedId: String, isCurrentlyBookmarked: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        (_uiState.value as? ProfileUiState.Success)?.let { currentState ->
            val newSavedPosts = if (isCurrentlyBookmarked) {
                currentState.savedPosts.filterNot { it.id == feedId }
            } else {
                (currentState.myPosts + currentState.savedPosts).find { it.id == feedId }
                    ?.let { currentState.savedPosts + it } ?: currentState.savedPosts
            }
            val newBookmarkedIds = if (isCurrentlyBookmarked) {
                currentState.bookmarkedFeedIds - feedId
            } else {
                currentState.bookmarkedFeedIds + feedId
            }
            _uiState.value = currentState.copy(
                savedPosts = newSavedPosts.distinctBy { it.id },
                bookmarkedFeedIds = newBookmarkedIds
            )
        }
        viewModelScope.launch {
            try {
                val feedRef = db.collection("feeds").document(feedId)
                val operation = if (isCurrentlyBookmarked) FieldValue.arrayRemove(currentUserId)
                else FieldValue.arrayUnion(currentUserId)
                feedRef.update("bookmarkedBy", operation).await()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error toggling bookmark for feed $feedId", e)
            }
        }
    }

    fun deleteFeed(feedId: String) {
        viewModelScope.launch {
            try {
                db.collection("feeds").document(feedId).delete().await()
                val currentState = (_uiState.value as? ProfileUiState.Success) ?: return@launch
                val newMyPosts = currentState.myPosts.filterNot { it.id == feedId }
                val newSavedPosts = currentState.savedPosts.filterNot { it.id == feedId }
                _uiState.value = currentState.copy(myPosts = newMyPosts, savedPosts = newSavedPosts)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error deleting feed $feedId", e)
            }
        }
    }

    fun blockUser(userIdToBlock: String) {
        if (currentUserId == null) return
        viewModelScope.launch {
            try {
                db.collection("users").document(currentUserId)
                    .update("blockedUsers", FieldValue.arrayUnion(userIdToBlock)).await()
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    _uiState.value = currentState.copy(isBlocked = true)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error blocking user $userIdToBlock", e)
            }
        }
    }

    fun unblockUser(userIdToUnblock: String) {
        val currentUserId = this.currentUserId ?: return
        val originalBlockedUsers = _blockedUsers.value
        val originalUiState = _uiState.value
        _blockedUsers.value = _blockedUsers.value.filterNot { it.uid == userIdToUnblock }
        if (originalUiState is ProfileUiState.Success && originalUiState.user.uid == userIdToUnblock) {
            _uiState.value = originalUiState.copy(isBlocked = false)
        }
        viewModelScope.launch {
            try {
                db.collection("users").document(currentUserId)
                    .update("blockedUsers", FieldValue.arrayRemove(userIdToUnblock)).await()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error unblocking user $userIdToUnblock", e)
                _blockedUsers.value = originalBlockedUsers
                if (originalUiState is ProfileUiState.Success && originalUiState.user.uid == userIdToUnblock) {
                    _uiState.value = originalUiState
                }
                fetchBlockedUsers()
            }
        }
    }

    fun fetchBlockedUsers() {
        if (currentUserId == null) return
        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(currentUserId).get().await()
                val blockedUserIds =
                    userDoc.toObject(User::class.java)?.blockedUsers ?: emptyList()
                if (blockedUserIds.isNotEmpty()) {
                    val blockedUsersList =
                        db.collection("users").whereIn("uid", blockedUserIds).get().await()
                            .toObjects(User::class.java)
                    _blockedUsers.value = blockedUsersList
                } else {
                    _blockedUsers.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error fetching blocked users", e)
            }
        }
    }

    fun forceSyncCounts(targetUserId: String) {
        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(targetUserId).get().await()
                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    val correctFollowerCount = user.followers.filter { it != targetUserId }.size.toLong()
                    val correctFollowingCount = user.following.filter { it != targetUserId }.size.toLong()
                    db.collection("users").document(targetUserId).update(
                        mapOf(
                            "followerCount" to correctFollowerCount,
                            "followingCount" to correctFollowingCount
                        )
                    ).await()
                }
            } catch (e: Exception) {
                Log.d("ProfileViewModel", "forceSyncCounts failed for $targetUserId", e)
            }
        }
    }

    fun syncFollowRelationship(targetUserId: String) {
        if (currentUserId == null) return
        viewModelScope.launch {
            try {
                val currentUserDoc = db.collection("users").document(currentUserId).get().await()
                val targetUserDoc = db.collection("users").document(targetUserId).get().await()
                val currentUserFollowing =
                    currentUserDoc.get("following") as? List<String> ?: emptyList()
                val targetUserFollowers =
                    targetUserDoc.get("followers") as? List<String> ?: emptyList()
                val shouldBeFollowing = currentUserFollowing.contains(targetUserId)
                val isInTargetFollowers = targetUserFollowers.contains(currentUserId)
                if (shouldBeFollowing && !isInTargetFollowers) {
                    db.collection("users").document(targetUserId)
                        .update("followers", FieldValue.arrayUnion(currentUserId)).await()
                } else if (!shouldBeFollowing && isInTargetFollowers) {
                    db.collection("users").document(targetUserId)
                        .update("followers", FieldValue.arrayRemove(currentUserId)).await()
                }
                fetchUserProfile(targetUserId)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "동기화 실패", e)
            }
        }
    }

    fun toggleWishlist(book: Book, isInWishlist: Boolean) {
        viewModelScope.launch {
            val success = if (isInWishlist) wishlistRepository.removeFromWishlist(book.isbn)
            else wishlistRepository.addToWishlist(book)
            if (success) {
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    val updatedWishlistIsbns = if (isInWishlist) {
                        currentState.wishlist - book.isbn
                    } else {
                        currentState.wishlist + book.isbn
                    }

                    val updatedFavoriteBooks = if (isInWishlist) {
                        currentState.favoriteBooks.filter { it.isbn != book.isbn }
                    } else {
                        (currentState.favoriteBooks + book).distinctBy { it.isbn }
                    }

                    _uiState.value = currentState.copy(
                        wishlist = updatedWishlistIsbns,
                        favoriteBooks = updatedFavoriteBooks
                    )
                }
            }
        }
    }

    fun toggleMyLibrary(book: Book, isInMyLibrary: Boolean) {
        viewModelScope.launch {
            val totalPages = 0
            val myBook = MyBook(
                id = book.isbn, title = book.title, author = book.author, cover = book.cover,
                isbn = book.isbn, totalPages = totalPages, currentPage = 0,
                isCompleted = false, addedDate = System.currentTimeMillis(),
                lastReadDate = System.currentTimeMillis(), completedDate = null
            )
            val success =
                if (isInMyLibrary) myLibraryRepository.removeBookFromLibrary(book.isbn) else myLibraryRepository.addBookToLibrary(
                    myBook
                )
            if (success) {
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    val updatedMyLibrary =
                        if (isInMyLibrary) currentState.myLibrary - book.isbn else currentState.myLibrary + book.isbn
                    _uiState.value = currentState.copy(myLibrary = updatedMyLibrary)
                }
            }
        }
    }
}