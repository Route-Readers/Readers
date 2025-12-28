package com.route.readers.data.remote

import android.util.Log
import com.route.readers.data.model.MyBook
import com.route.readers.data.model.ReadingSession
import com.route.readers.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.route.readers.data.remote.FirestoreRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    
    private fun getCurrentWeekNumber(): Int {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val week = calendar.get(Calendar.WEEK_OF_YEAR)
        return year * 100 + week
    }

    private suspend fun updateChallengeProgress() {
        try {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
            val challengeRepository = ChallengeRepository(firestoreRepository)
            val currentWeekNumber = getCurrentWeekNumber()
            val weeklyChallenges = challengeRepository.getChallengesForWeek(currentWeekNumber)

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            weeklyChallenges.filter { it.participants.contains(userId) && it.type == com.route.readers.data.model.ChallengeType.DAILY_PAGES_READING }
                .forEach { challenge ->
                    challengeRepository.updateDailyProgress(challenge.id, userId, today)
                }

        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error updating challenge progress: ${e.message}", e)
        }
    }
    
    private suspend fun checkIfReadToday(): Boolean {
        return calculatePagesReadToday() > 0
    }
    
    private suspend fun calculatePagesReadToday(): Int {
        return firestoreRepository.getPagesReadToday()
    }

    suspend fun addReadingTime(isbn: String, timeInSeconds: Int): Boolean {
        return try {
            val success = firestoreRepository.addReadingTime(isbn, timeInSeconds)
            if (success) {
                syncWithFirestore()
            }
            success
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error adding reading time: ${e.message}", e)
            false
        }
    }

    suspend fun addReadingSession(session: ReadingSession): Boolean {
        return try {
            val success = firestoreRepository.addReadingSession(session)
            if (success) {
                // 세션 추가 후 특별히 동기화할 필요는 없지만, 필요하다면 추가
            }
            success
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error adding reading session: ${e.message}", e)
            false
        }
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
