package com.route.readers.data.model

import com.google.gson.annotations.SerializedName

data class SubInfo(
    @SerializedName("itemPage") val itemPage: Int? = null
)

data class Book(
    @SerializedName("title") val title: String = "",
    @SerializedName("author") val author: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("isbn13") val isbn: String = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("categoryName") val categoryName: String? = null,
    @SerializedName("itemPage") val itemPage: String? = null,
    @SerializedName("subInfo") val subInfo: SubInfo? = null,
    @SerializedName("publisher") val publisher: String? = null,
    @SerializedName("pubDate") val pubDate: String? = null,
    // 독서 진행 상황 관련 필드들 (로컬 데이터)
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val progress: Int = 0
) {
    // 페이지 정보 추출 - subInfo.itemPage가 가장 정확함
    fun extractPageCount(): Int {
        // subInfo의 itemPage를 우선 사용 (가장 정확한 정보)
        return subInfo?.itemPage ?: 0
    }
    
    // 페이지 정보가 유효한지 확인
    fun hasValidPageInfo(): Boolean {
        return extractPageCount() > 0
    }
}
