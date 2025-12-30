package com.route.readers.data.remote

import com.route.readers.data.model.BookListDTO
import retrofit2.http.GET
import retrofit2.http.Query

interface BookService {

    @GET("ItemSearch.aspx")    suspend fun getBookSearch(
        @Query("TTBKey") ttbKey: String,
        @Query("Query") query: String,
        @Query("QueryType") queryType: String = "Keyword",
        @Query("MaxResults") maxResults: Int, // 기본값 제거
        @Query("Start") start: Int,          // 기본값 제거
        @Query("SearchTarget") searchTarget: String = "Book",
        @Query("Sort") sort: String = "SalesPoint",
        @Query("output") output: String = "js",
        @Query("Version") version: String = "20131101",
        @Query("Cover") cover: String = "Big",
        @Query("CategoryId") categoryId: String? = null, // Add this line
        @Query("OptResult") optResult: String = "ebookBlazing,usedList,reviewList,subInfo"
    ): BookListDTO // Response<T> -> T 로 변경

    @GET("ItemList.aspx")
    suspend fun getBookList(
        @Query("TTBKey") ttbKey: String,
        @Query("QueryType") queryType: String,
        @Query("SearchTarget") searchTarget: String,
        @Query("output") output: String,
        @Query("Version") version: String = "20131101",
        @Query("Cover") cover: String = "Big",
        @Query("OptResult") optResult: String = "ebookBlazing,usedList,reviewList,subInfo"
    ): BookListDTO // Response<T> -> T 로 변경

    @GET("ItemLookUp.aspx")
    suspend fun getBookDetail(
        @Query("TTBKey") ttbKey: String,
        @Query("ItemId") itemId: String,
        @Query("itemIdType") itemIdType: String,
        @Query("output") output: String,
        @Query("Version") version: String = "20131101",
        @Query("Cover") cover: String = "Big",
        @Query("OptResult") optResult: String = "ebookBlazing,usedList,reviewList,subInfo"
    ): BookListDTO // Response<T> -> T 로 변경
}
