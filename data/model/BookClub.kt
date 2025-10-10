package com.route.readers.data.model

data class BookClub(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val currentBook: String = "",
    val currentBookAuthor: String = "",
    val memberCount: Int = 0,
    val isJoined: Boolean = false,
    val bookTitle: String = currentBook // 호환성을 위해 유지
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
