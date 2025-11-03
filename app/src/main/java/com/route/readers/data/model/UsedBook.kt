package com.route.readers.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UsedBook(
    val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerProfileImage: String? = null,
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val bookCover: String? = null,
    val condition: String = "", // 상급, 중급, 하급
    val price: Int = 0, // 토큰 가격
    val description: String = "",
    val status: String = "판매중", // 판매중, 예약중, 판매완료
    @ServerTimestamp val createdAt: Date? = null,
    val buyerId: String? = null
)
