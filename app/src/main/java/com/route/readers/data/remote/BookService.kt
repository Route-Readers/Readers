package com.route.readers.data.remote

import com.route.readers.data.model.BookListDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BookService {

    // 알라딘 책 검색 api
    @GET("ItemSearch.aspx")
    suspend fun getBookSearch(
        @Query("ttbkey") ttbkey: String,
        @Query("Query") query: String,
        @Query("QueryType") queryType: String = "Title",
        @Query("MaxResults") maxResults: Int = 10,
        @Query("start") start: Int = 1,
        @Query("SearchTarget") searchTarget: String = "Book",
        @Query("output") output: String = "js",
        @Query("Version") version: String = "20131101"
    ): Response<BookListDTO>

    // 알라딘 책 목록 api
    @GET("ItemList.aspx")
    suspend fun getBookList(
        @Query("ttbkey") ttbkey: String,
        @Query("QueryType") querytype: String,
        @Query("SearchTarget") searchtarget: String,
        @Query("output") output: String,
        @Query("Version") version: String = "20131101"
    ): Response<BookListDTO>

    // 알라딘 책 상세 api
    @GET("ItemLookUp.aspx")
    suspend fun getBookDetail(
        @Query("ttbkey") ttbkey: String,
        @Query("ItemId") itemid: String,
        @Query("itemIdType") itemidtype: String,
        @Query("output") output: String,
        @Query("Version") version: String = "20131101"
    ): Response<BookListDTO>
}
