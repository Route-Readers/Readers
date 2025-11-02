package com.route.readers.data.remote

import android.util.Log
import com.route.readers.data.model.MyBook
import com.route.readers.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyLibraryRepository {

    private val _myBooks = MutableStateFlow<List<MyBook>>(emptyList())
    val myBooks: StateFlow<List<MyBook>> = _myBooks

    private val firestoreRepository = FirestoreRepository()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    var onLibraryUpdate: (() -> Unit)? = null

    init {
        repositoryScope.launch {
            syncWithFirestore()
        }
    }

    suspend fun addBookToLibrary(book: MyBook): Boolean {
        return try {
            val firestoreSuccess = firestoreRepository.addBookToLibrary(book)
            if (firestoreSuccess) {
                syncWithFirestore()
                WidgetUpdateHelper.updateAllWidgets()
                onLibraryUpdate?.invoke()
            }
            firestoreSuccess
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error adding book: ${e.message}", e)
            false
        }
    }

    suspend fun updateReadingProgress(isbn: String, currentPage: Int, isCompleted: Boolean): Boolean {
        return try {
            val success = firestoreRepository.updateReadingProgress(isbn, currentPage, isCompleted)

            if (success) {
                syncWithFirestore()
                WidgetUpdateHelper.updateAllWidgets()
                
                // 챌린지 진행률 업데이트
                updateChallengeProgress()
            }

            success
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error updating progress: ${e.message}", e)
            false
        }
    }
    
    private suspend fun updateChallengeProgress() {
        try {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
            val challengeRepository = ChallengeRepository()
            val challenges = challengeRepository.getChallenges()
            
            val userChallenge = challenges.find { it.participants.contains(userId) } ?: return
            
            when (userChallenge.id) {
                "challenge_7days_reading" -> {
                    // 7일 연속 독서: 오늘 읽었으면 +1일
                    val currentProgress = userChallenge.progress[userId] ?: 0
                    if (checkIfReadToday() && currentProgress < 7) {
                        challengeRepository.updateProgress(userChallenge.id, userId, currentProgress + 1)
                    }
                }
                "challenge_30pages_daily" -> {
                    // 하루 30페이지 7일: 30페이지 읽으면 1일 달성
                    val pagesReadToday = calculatePagesReadToday()
                    val daysCompleted = if (pagesReadToday >= 30) {
                        val currentProgress = userChallenge.progress[userId] ?: 0
                        (currentProgress + 1).coerceAtMost(7)
                    } else {
                        userChallenge.progress[userId] ?: 0
                    }
                    challengeRepository.updateProgress(userChallenge.id, userId, daysCompleted)
                }
                "challenge_1book_weekly" -> {
                    // 1주일에 한 권: 책 완독하면 1권 달성
                    val completedBooks = _myBooks.value.count { it.isCompleted }
                    val booksThisWeek = if (completedBooks > 0) 1 else 0
                    challengeRepository.updateProgress(userChallenge.id, userId, booksThisWeek)
                }
            }
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error updating challenge progress: ${e.message}", e)
        }
    }
    
    private fun checkIfReadToday(): Boolean {
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        return _myBooks.value.any { book ->
            book.lastReadDate?.let { it >= today } ?: false
        }
    }
    
    private fun calculatePagesReadToday(): Int {
        // 오늘 읽은 페이지 수 = 현재 진행 중인 책들의 currentPage 합계
        return _myBooks.value
            .filter { !it.isCompleted }
            .sumOf { it.currentPage }
    }

    suspend fun removeBookFromLibrary(isbn: String): Boolean {
        return try {
            val success = firestoreRepository.removeBookFromLibrary(isbn)
            if (success) {
                syncWithFirestore()
                WidgetUpdateHelper.updateAllWidgets()
                onLibraryUpdate?.invoke()
            }
            success
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error removing book: ${e.message}", e)
            false
        }
    }

    suspend fun isBookInLibrary(isbn: String): Boolean {
        return try {
            _myBooks.value.any { it.isbn == isbn }
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error checking book existence: ${e.message}", e)
            false
        }
    }

    suspend fun syncWithFirestore() {
        try {
            val firestoreBooks = firestoreRepository.getMyBooks()
            _myBooks.value = firestoreBooks
            Log.d("MyLibraryRepository", "Synced ${firestoreBooks.size} books from Firestore")
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error syncing with Firestore: ${e.message}", e)
        }
    }

    suspend fun getMyBooks(): List<MyBook> {
        syncWithFirestore()
        return _myBooks.value
    }
}
