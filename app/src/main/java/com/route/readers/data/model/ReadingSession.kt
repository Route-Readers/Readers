package com.route.readers.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ReadingSession(
    val sessionId: String = "",
    val bookId: String = "",
    @ServerTimestamp val startTime: Date? = null,
    @ServerTimestamp val endTime: Date? = null,
    val durationInSeconds: Int = 0,
    val pagesRead: Int = 0,
    val year: Int = 0,
    val month: Int = 0, // 1-12
    val dayOfMonth: Int = 0,
    val dayOfWeek: Int = 0, // 1 (월요일) to 7 (일요일)
    val weekOfYear: Int = 0
)
