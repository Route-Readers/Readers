package com.route.readers.data.model

data class BookClub(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val currentBook: String = "",
    val currentBookAuthor: String = "",
    val nextMeetingDate: String = "",
    val memberCount: Int = 0,
    val members: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isJoined: Boolean = false,
    val bookTitle: String = currentBook
)

data class ChatMessage(
    val id: String = "",
    val bookClubId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderProfileImage: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
