package com.route.readers.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude // Exclude 어노테이션을 import 합니다.

/**
 * Firestore의 'users' 컬렉션 문서 구조와 매핑되는 통합 데이터 모델.
 */
data class User(
    // ✨ @DocumentId를 위한 필드를 새로 추가하고, 문서에 저장되지 않도록 @Exclude 처리합니다.
    @DocumentId @get:Exclude
    val documentId: String = "",

    // Firestore 문서 내부에 이미 존재하는 'uid' 필드는 그대로 둡니다.
    val uid: String = "",

    val nickname: String = "",
    val email: String? = null,
    val profileImageUrl: String? = null,
    val readingGenres: List<String> = emptyList(),
    val readingStyles: List<String> = emptyList(),
    val level: Int = 1,

    // 팔로우/팔로잉 정보 필드
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val followerCount: Long = 0,
    val followingCount: Long = 0,

    // 추가 정보 필드
    val readBookCount: Int = 0,
    val isCurrentlyReading: Boolean = false
)
