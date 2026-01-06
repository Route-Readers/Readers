package com.route.readers.data.model

import com.google.firebase.Timestamp

data class Report(
    val id: String = "",
    val reporterId: String = "", // 신고한 사용자 UID
    val reporterNickname: String = "", // 신고한 사용자 닉네임
    val targetId: String = "", // 신고 대상 ID (댓글, 게시물 등)
    val targetType: String = "", // 신고 타입 (feed, chat, comment 등)
    val targetOwnerId: String = "", // 신고당한 사용자 UID
    val reason: String = "", // 신고 이유
    val reasonDetail: String? = null, // 상세 사유
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "pending", // pending, resolved, dismissed
    val resolvedBy: String? = null, // 처리한 관리자 ID
    val resolvedAt: Long? = null, // 처리 시간
    val actionTaken: String? = null // 취한 조치
)
