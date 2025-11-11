package com.route.readers.ui.screens.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.MyBook
import com.route.readers.data.model.ReadingSession
import com.route.readers.data.remote.FirestoreRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.UUID

class ReadingViewModel : ViewModel() {

    private val firestoreRepository = FirestoreRepository()

    fun saveReadingSession(book: MyBook, durationInSeconds: Int) {
        // 10초 미만의 짧은 세션은 저장하지 않음
        if (durationInSeconds < 10) return

        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val session = ReadingSession(
                sessionId = UUID.randomUUID().toString(),
                bookId = book.isbn,
                startTime = Date(), // 실제 시작 시간은 아니지만, 세션이 끝난 시간을 기록
                endTime = Date(),
                durationInSeconds = durationInSeconds,
                pagesRead = 0, // 페이지 추적 기능은 추가 구현 필요
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1, // Calendar.MONTH는 0부터 시작
                dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
                weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            )
            firestoreRepository.addReadingSession(session)
        }
    }
}
