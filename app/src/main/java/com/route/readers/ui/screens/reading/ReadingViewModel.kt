package com.route.readers.ui.screens.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.MyBook
import com.route.readers.data.model.ReadingSession
import com.route.readers.data.remote.ChallengeRepository
import com.route.readers.data.remote.FirestoreRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.UUID

class ReadingViewModel : ViewModel() {

    private val firestoreRepository = FirestoreRepository()
    private val challengeRepository = ChallengeRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    fun saveReadingSession(book: MyBook, durationInSeconds: Int, pagesRead: Int) {
        // 10초 미만의 짧은 세션은 저장하지 않음
        if (durationInSeconds < 10) return

        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val session = ReadingSession(
                sessionId = UUID.randomUUID().toString(),
                bookId = book.isbn,
                startTime = Date(),
                endTime = Date(),
                durationInSeconds = durationInSeconds,
                pagesRead = pagesRead,
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
                weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            )
            firestoreRepository.addReadingSession(session)
            android.util.Log.d("ReadingVM", "Reading session added for book: ${book.isbn}, duration: $durationInSeconds, pagesRead: $pagesRead")


            currentUserId?.let { userId ->
                android.util.Log.d("ReadingVM", "Attempting to update challenge for userId: $userId, pagesRead: $pagesRead")
                challengeRepository.updatePagesReadChallengeProgress(userId, pagesRead)
                challengeRepository.refreshUserActiveChallenge(userId)
                android.util.Log.d("ReadingVM", "Challenge update and refresh triggered for userId: $userId")
            } ?: run {
                android.util.Log.e("ReadingVM", "currentUserId is null, cannot update challenge.")
            }
        }
    }
}
