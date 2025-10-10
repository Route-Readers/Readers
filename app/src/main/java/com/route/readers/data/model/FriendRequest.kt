package com.route.readers.data.model

import com.google.firebase.Timestamp

data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderNickname: String = "",
    val senderProfileImageUrl: String? = null,
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAt: Timestamp = Timestamp.now(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
