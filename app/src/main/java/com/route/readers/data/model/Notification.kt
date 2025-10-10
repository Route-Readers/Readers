package com.route.readers.data.model

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.FRIEND_REQUEST,
    val title: String = "",
    val message: String = "",
    val data: Map<String, Any> = emptyMap(),
    val isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class NotificationType {
    FRIEND_REQUEST,
    FRIEND_ACCEPTED,
    READING_REMINDER,
    CHALLENGE_INVITE,
    BOOK_RECOMMENDATION
}
