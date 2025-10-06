package com.route.readers.data.remote

import com.route.readers.data.model.BookListDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BookService {

    @GET("ItemSearch.aspx")
    suspend fun getBookSearch(
        @Query("TTBKey") ttbKey: String, // 파라미터명 대문자 TTBKey로 수정
        @Query("Query") query: String,
        @Query("QueryType") queryType: String = "Keyword", // "Title" -> "Keyword"로 변경
        @Query("MaxResults") maxResults: Int = 20, // 기본값을 20으로 변경
        @Query("Start") start: Int = 1, // 파라미터명 대문자 Start로 수정
        @Query("SearchTarget") searchTarget: String = "Book",
        @Query("output") output: String = "js",
        @Query("Version") version: String = "20131101",
        @Query("OptResult") optResult: String = "ebookBlazing,usedList,reviewList,subInfo"
    ): Response<BookListDTO>

    /**
     * 알라딘 도서 목록 API (신간, 베스트셀러 등)
     */
    @GET("ItemList.aspx")
    suspend fun getBookList(
        @Query("ttbkey") ttbkey: String,
        @Query("QueryType") querytype: String,
        @Query("SearchTarget") searchtarget: String,
        @Query("output") output: String,
        @Query("Version") version: String = "20131101",
        @Query("OptResult") optResult: String = "ebookBlazing,usedList,reviewList,subInfo"
    ): Response<BookListDTO>

    /**
     * 알라딘 도서 상세 정보 API
     */
    @GET("ItemLookUp.aspx")
    suspend fun getBookDetail(
        @Query("ttbkey") ttbkey: String,
        @Query("ItemId") itemid: String,
        @Query("itemIdType") itemidtype: String,
        @Query("output") output: String,
        @Query("Version") version: String = "20131101",
        @Query("OptResult") optResult: String = "ebookBlazing,usedList,reviewList,subInfo"
    ): Response<BookListDTO>
}
