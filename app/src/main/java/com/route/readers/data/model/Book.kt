package com.route.readers.data.model

import com.google.gson.annotations.SerializedName

data class Book(
    @SerializedName("title") val title: String = "",
    @SerializedName("author") val author: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("isbn13") val isbn: String = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("categoryName") val categoryName: String? = null,
    @SerializedName("publisher") val publisher: String? = null,
    @SerializedName("pubDate") val pubDate: String? = null,
    @SerializedName("priceSales") val priceSales: Int? = null,
    @SerializedName("priceStandard") val priceStandard: Int? = null,
    @SerializedName("subInfo") val subInfo: Map<String, Any>? = null,
    // 독서 진행 상황 관련 필드들 (로컬 데이터)
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val progress: Int = 0
) {
    // 페이지 수를 추출하는 함수
    fun getPageCount(): Int {
        // subInfo에서 itemPage 직접 확인
        subInfo?.get("itemPage")?.let { itemPage ->
            when (itemPage) {
                is Number -> return itemPage.toInt()
                is String -> itemPage.toIntOrNull()?.let { return it }
            }
        }
        
        // description에서 페이지 정보 추출 시도 (예: "320쪽" 형태)
        description.let { desc ->
            val pageRegex = "(\\d+)(?:쪽|페이지|p)".toRegex(RegexOption.IGNORE_CASE)
            pageRegex.find(desc)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        }
        
        return 0
    }
}
