package com.route.readers.data.model

data class BookClub(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val currentBook: String = "",
    val currentBookAuthor: String = "",
    val nextMeetingDate: String = "",
    val memberCount: Int = 0,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = "",
    val bookClubId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
