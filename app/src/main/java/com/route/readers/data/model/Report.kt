package com.route.readers.data.model

data class Report(
    val id: String = "",
    val reporterId: String = "",
    val reporterNickname: String = "",
    val targetId: String = "",
    val targetType: String = "", // "user", "feed", "chat", "bookclub_chat"
    val targetOwnerId: String = "", // 신고 대상의 소유자 (피드 작성자, 채팅 발신자 등)
    val reason: String = "",
    val reasonDetail: String? = null,
    val status: String = "pending", // "pending", "resolved", "dismissed"
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val actionTaken: String? = null // "warning", "ban", "delete", "none"
)

object ReportReason {
    const val SPAM = "스팸/광고"
    const val HATE = "욕설/혐오 표현"
    const val SEXUAL = "성적 콘텐츠"
    const val FRAUD = "사기/사칭"
    const val OTHER = "기타"
    
    val all = listOf(SPAM, HATE, SEXUAL, FRAUD, OTHER)
}
