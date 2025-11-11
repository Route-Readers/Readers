package com.route.readers.ui.screens.search

import com.route.readers.data.remote.BookAvailabilityResponse
import com.route.readers.data.remote.LibrarySearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

data class BookSearchDetailResponse(val response: BookDetailItem)
data class BookDetailItem(val detail: List<BookDetail>)
data class BookDetail(
    val book: BookInfo
)
data class BookInfo(
    val bookname: String,
    val authors: String,
    val publisher: String,
    val publication_year: String,
    val isbn13: String,
    val description: String,
    val bookImageURL: String
)

interface LibraryApiService {

    @GET("api/libSrchByBook")
    suspend fun searchLibrariesWithBook(
        @Query("authKey") authKey: String,
        @Query("isbn") isbn: String,
        @Query("region") region: String,
        @Query("format") format: String = "json"
    ): LibrarySearchResponse

    @GET("api/bookExist")
    suspend fun getBookAvailability(
        @Query("authKey") authKey: String,
        @Query("libCode") libCode: String,
        @Query("isbn13") isbn13: String,
        @Query("format") format: String = "json"
    ): BookAvailabilityResponse

    @GET("api/libSrch")
    suspend fun searchLibrariesByArea(
        @Query("authKey") authKey: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 30,
        @Query("format") format: String = "json",
        @Query("radius") radius: Int = 50
    ): LibrarySearchResponse

    @GET("api/srchDtlList")
    suspend fun searchBookDetail(
        @Query("authKey") authKey: String,
        @Query("isbn13") isbn13: String,
        @Query("format") format: String = "json"
    ): BookSearchDetailResponse
}
