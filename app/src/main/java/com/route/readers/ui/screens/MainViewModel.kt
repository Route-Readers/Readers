package com.route.readers.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.ReadingSession
import com.route.readers.data.model.User
import com.route.readers.data.model.Challenge
import com.route.readers.data.remote.MyLibraryRepository
import com.route.readers.data.remote.ChallengeRepository
import com.route.readers.data.remote.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid
    private val myLibraryRepository = MyLibraryRepository()
    private val firestoreRepository = FirestoreRepository()
    private val challengeRepository = ChallengeRepository(firestoreRepository)

    private val _consecutiveDays = MutableStateFlow(0)
    val consecutiveDays = _consecutiveDays.asStateFlow()

    private val _tokens = MutableStateFlow(0)
    val tokens = _tokens.asStateFlow()

    private val _userActiveChallenge = MutableStateFlow<Challenge?>(null)
    val userActiveChallenge: StateFlow<Challenge?> = _userActiveChallenge.asStateFlow()

    init {
        checkAndUpdateAttendance()
        loadUserTokens()
        // Initialize the active challenge from the repository's flow
        currentUserId?.let { userId ->
            challengeRepository.userActiveChallenges
                .mapNotNull { it[userId] } // Get the challenge for the current user
                .onEach { _userActiveChallenge.value = it } // Update MainViewModel's flow
                .launchIn(viewModelScope) // Collect within ViewModel's scope

            // Explicitly refresh the active challenge in the repository when MainViewModel starts
            challengeRepository.refreshUserActiveChallenge(userId)
        }
    }

    private fun checkAndUpdateAttendance() {
        if (currentUserId == null) {
            _consecutiveDays.value = 0
            return
        }

        viewModelScope.launch {
            try {
                val userRef = db.collection("users").document(currentUserId)
                val userDoc = userRef.get().await()
                val user = userDoc.toObject(User::class.java)

                if (user == null) {
                    _consecutiveDays.value = 0
                    return@launch
                }

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = sdf.format(Date())

                if (user.lastLoginDate == todayStr) {
                    _consecutiveDays.value = user.consecutiveDays
                    return@launch
                }

                var newConsecutiveDays = 1
                val lastLoginDateStr = user.lastLoginDate

                if (lastLoginDateStr.isNotEmpty()) {
                    try {
                        val lastLoginCalendar = Calendar.getInstance().apply {
                            time = sdf.parse(lastLoginDateStr) ?: Date()
                        }
                        val yesterdayCalendar = Calendar.getInstance().apply {
                            add(Calendar.DATE, -1)
                        }

                        val isYesterday = lastLoginCalendar.get(Calendar.YEAR) == yesterdayCalendar.get(Calendar.YEAR) &&
                                lastLoginCalendar.get(Calendar.DAY_OF_YEAR) == yesterdayCalendar.get(Calendar.DAY_OF_YEAR)

                        if (isYesterday) {
                            newConsecutiveDays = user.consecutiveDays + 1
                        }
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error parsing lastLoginDate: $lastLoginDateStr", e)
                        newConsecutiveDays = 1
                    }
                }

                userRef.update(
                    mapOf(
                        "lastLoginDate" to todayStr,
                        "consecutiveDays" to newConsecutiveDays
                    )
                ).await()

                _consecutiveDays.value = newConsecutiveDays

            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to check or update attendance", e)
                _consecutiveDays.value = 0
            }
        }
    }

    private fun loadUserTokens() {
        if (currentUserId == null) {
            _tokens.value = 0
            return
        }

        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(currentUserId).get().await()
                val user = userDoc.toObject(User::class.java)
                _tokens.value = user?.tokens ?: 0
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to load user tokens", e)
                _tokens.value = 0
            }
        }
    }

    fun addReadingTime(bookId: String, timeInSeconds: Int) {
        viewModelScope.launch {
            myLibraryRepository.addReadingTime(bookId, timeInSeconds)
        }
    }

    suspend fun saveReadingSession(
        book: com.route.readers.data.model.MyBook,
        newCurrentPage: Int,
        durationInSeconds: Int
    ): Pair<com.route.readers.data.model.MyBook, Int>? {
        val userId = auth.currentUser?.uid ?: return null
        val oldCurrentPage = book.currentPage
        val pagesReadThisSession = newCurrentPage - oldCurrentPage

        if (pagesReadThisSession < 0) {
            Log.e("MainViewModel", "Pages read this session cannot be negative.")
            return null
        }

        val isCompleted = newCurrentPage >= book.totalPages

        // 1. 책 진행률 업데이트 (페이지, 완료 여부)
        val updateSuccess = myLibraryRepository.updateReadingProgress(
            book.isbn,
            newCurrentPage,
            isCompleted
        )

        if (!updateSuccess) {
            Log.e("MainViewModel", "Failed to update reading progress for book: ${book.isbn}")
            return null
        }

        // 2. 독서 세션 저장
        val endTime = Date()
        val startTime = Date(endTime.time - (durationInSeconds * 1000L))

        val calendar = Calendar.getInstance().apply { time = startTime }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH는 0부터 시작
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1(일) ~ 7(토)
        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)

        val readingSession = ReadingSession(
            sessionId = db.collection("users").document(userId).collection("reading_sessions").document().id,
            bookId = book.isbn,
            startTime = startTime,
            endTime = endTime,
            durationInSeconds = durationInSeconds,
            pagesRead = pagesReadThisSession,
            year = year,
            month = month,
            dayOfMonth = dayOfMonth,
            dayOfWeek = dayOfWeek,
            weekOfYear = weekOfYear
        )

        val sessionSaveSuccess = myLibraryRepository.addReadingSession(readingSession)
        if (!sessionSaveSuccess) {
            Log.e("MainViewModel", "Failed to save reading session for book: ${book.isbn}")
            return null
        }

        // 3. 챌린지 진행도 업데이트
        if (pagesReadThisSession > 0) {
            updateChallengeProgress(userId, startTime)
        }

        // 4. 피드 게시 다이얼로그를 위한 정보 반환
        val updatedBook = book.copy(
            currentPage = newCurrentPage,
            isCompleted = isCompleted,
            lastReadDate = endTime.time
        )
        return Pair(updatedBook, pagesReadThisSession)
    }

    private fun updateChallengeProgress(userId: String, readingDate: Date) {
        GlobalScope.launch {
            try {
                Log.d("MainViewModel", "Updating challenge progress: userId=$userId")
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateStr = sdf.format(readingDate)
                
                // 활성화된 일일 페이지 챌린지들을 가져와서 업데이트
                val activeChallenges = challengeRepository.getActiveChallenges(userId)
                Log.d("MainViewModel", "Found ${activeChallenges.size} active challenges")
                
                activeChallenges.forEach { challenge: Challenge ->
                    Log.d("MainViewModel", "Challenge: ${challenge.title}, type: ${challenge.type}")
                    if (challenge.type == com.route.readers.data.model.ChallengeType.DAILY_PAGES_READING) {
                        Log.d("MainViewModel", "Updating daily progress for challenge: ${challenge.id}")
                        challengeRepository.updateDailyProgress(
                            challengeId = challenge.id,
                            userId = userId,
                            date = dateStr
                        )
                    }
                }
                // Removed: loadUserActiveChallenge() // Refresh active challenge after progress update
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to update challenge progress", e)
            }
        }
    }
}
