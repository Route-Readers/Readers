package com.route.readers.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/** * Firestore의 'users' 컬렉션 문서 구조와 매핑되는 통합 데이터 모델.
 */
data class User(
    @DocumentId @get:Exclude
    val documentId: String = "",

    val uid: String = "",

    val nickname: String = "",
    val email: String? = null,
    val profileImageUrl: String? = null,
    val bio: String? = null, // 이 라인을 추가했습니다.
    val readingGenres: List<String> = emptyList(),
    val readingStyles: List<String> = emptyList(),
    val level: Int = 1,

    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val followerCount: Long = 0,
    val followingCount: Long = 0,

    val readBookCount: Int = 0,
    val isCurrentlyReading: Boolean = false
)
