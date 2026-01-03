package com.route.readers.data.model

data class BookClub(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val currentBook: String = "",
    val currentBookAuthor: String = "",
    val currentBookCover: String = "",
    val currentBookGenre: String = "",
    val currentBookDescription: String = "",
    val nextMeetingDate: String = "",
    val memberCount: Int = 0,
    val members: List<String> = emptyList(),
    val viceOwners: List<String> = emptyList(), // 부방장 목록
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isJoined: Boolean = false,
    val bookTitle: String = currentBook,
    val bookHistory: Map<String, String> = emptyMap()
) {
    fun isOwner(userId: String): Boolean = createdBy == userId
    fun isViceOwner(userId: String): Boolean = viceOwners.contains(userId)
    fun isMember(userId: String): Boolean = members.contains(userId)
}

enum class BookClubRole {
    OWNER,       // 방장
    VICE_OWNER,  // 부방장
    MEMBER       // 멤버
}

enum class BookClubPermission {
    DELETE_CLUB,
    EDIT_CLUB_INFO,
    CHANGE_BOOK,
    KICK_MEMBER,
    MANAGE_VICE_OWNER, // 부방장 임명/해제
    SEND_MESSAGE,
    LEAVE_CLUB,
    VIEW_CONTENT
}

object BookClubPermissions {
    private val ownerPermissions = setOf(
        BookClubPermission.DELETE_CLUB,
        BookClubPermission.EDIT_CLUB_INFO,
        BookClubPermission.CHANGE_BOOK,
        BookClubPermission.KICK_MEMBER,
        BookClubPermission.MANAGE_VICE_OWNER,
        BookClubPermission.SEND_MESSAGE,
        BookClubPermission.VIEW_CONTENT
    )
    
    private val viceOwnerPermissions = setOf(
        BookClubPermission.EDIT_CLUB_INFO,
        BookClubPermission.CHANGE_BOOK,
        BookClubPermission.KICK_MEMBER,
        BookClubPermission.SEND_MESSAGE,
        BookClubPermission.LEAVE_CLUB,
        BookClubPermission.VIEW_CONTENT
    )
    
    private val memberPermissions = setOf(
        BookClubPermission.SEND_MESSAGE,
        BookClubPermission.LEAVE_CLUB,
        BookClubPermission.VIEW_CONTENT
    )
    
    fun hasPermission(role: BookClubRole, permission: BookClubPermission): Boolean {
        return when (role) {
            BookClubRole.OWNER -> ownerPermissions.contains(permission)
            BookClubRole.VICE_OWNER -> viceOwnerPermissions.contains(permission)
            BookClubRole.MEMBER -> memberPermissions.contains(permission)
        }
    }
    
    fun getUserRole(bookClub: BookClub, userId: String): BookClubRole? {
        return when {
            bookClub.isOwner(userId) -> BookClubRole.OWNER
            bookClub.isViceOwner(userId) -> BookClubRole.VICE_OWNER
            bookClub.isMember(userId) -> BookClubRole.MEMBER
            else -> null
        }
    }
}

data class ChatMessage(
    val id: String = "",
    val bookClubId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderProfileImage: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
