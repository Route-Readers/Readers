package com.route.readers.data.model

data class AdminLog(
    val id: String = "",
    val adminId: String = "",
    val adminNickname: String = "",
    val action: String = "",
    val targetId: String = "",
    val targetType: String = "",
    val targetNickname: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object AdminAction {
    const val WARN_USER = "유저 경고"
    const val BAN_USER = "유저 정지"
    const val UNBAN_USER = "유저 정지 해제"
    const val DELETE_FEED = "피드 삭제"
    const val HIDE_CHAT = "채팅 숨김"
    const val KICK_FROM_CLUB = "북클럽 강퇴"
    const val RESOLVE_REPORT = "신고 처리"
    const val DISMISS_REPORT = "신고 기각"
    const val NEW_REPORT = "신고 접수"
}
