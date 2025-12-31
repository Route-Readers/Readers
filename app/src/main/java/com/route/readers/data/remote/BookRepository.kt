package com.route.readers.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.GsonBuilder
import com.route.readers.BuildConfig
import com.route.readers.data.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BookRepository {

    companion object {
        private val TTBKEY = BuildConfig.ALADIN_TTB_KEY
        private const val ITEM_ID_TYPE = "ISBN"
        private const val OUTPUT = "js"
        private const val VERSION = "20131101"
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    private val bookService: BookService by lazy {
        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://www.aladin.co.kr/ttb/api/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        retrofit.create(BookService::class.java)
    }

    suspend fun getBookSearch(
        query: String,
        page: Int = 1,
        maxResults: Int = 10,
        categoryId: String? = null, // Add this parameter
        sort: String = "Accuracy" // 정렬 파라미터 추가
    ): List<Book> = withContext(Dispatchers.IO) {
        try {
            if (TTBKEY.isBlank()) {
                Log.w("BookRepository", "알라딘 TTBKEY가 비어있습니다.")
                return@withContext emptyList()
            }

            Log.d("BookRepository", "API 호출 시작: query='$query', page=$page, maxResults=$maxResults, categoryId='$categoryId', sort='$sort'")

            val response = bookService.getBookSearch(
                ttbKey = TTBKEY,
                query = query,
                maxResults = maxResults,
                start = page,
                categoryId = categoryId, // Pass the categoryId
                sort = sort // Pass the sort parameter
            )

            val foundBooksCount = response.books?.size ?: 0
            Log.i("BookRepository", "API 응답 성공. 찾은 책 개수: $foundBooksCount")

            return@withContext (response.books ?: emptyList()).filter { it.isbn.isNotBlank() }

        } catch (e: Exception) {
            Log.e("BookRepository", "API 호출 중 심각한 오류 발생", e)
            return@withContext emptyList()
        }
    }

    suspend fun getBookList(
        queryType: String = "ItemNewAll", // 기본값을 "ItemNewAll"로 설정
        page: Int = 1,
        maxResults: Int = 10
    ): List<Book> = withContext(Dispatchers.IO) {
        try {
            if (TTBKEY.isBlank()) {
                Log.w("BookRepository", "알라딘 TTBKEY가 비어있습니다.")
                return@withContext emptyList()
            }

            Log.d("BookRepository", "API 호출 시작: queryType='$queryType', page=$page, maxResults=$maxResults")

            val response = bookService.getBookList(
                ttbKey = TTBKEY,
                queryType = queryType,
                maxResults = maxResults,
                start = page,
                searchTarget = "Book",
                output = OUTPUT
            )

            val foundBooksCount = response.books?.size ?: 0
            Log.i("BookRepository", "API 응답 성공. 찾은 책 개수: $foundBooksCount")

            return@withContext (response.books ?: emptyList()).filter { it.isbn.isNotBlank() }
        } catch (e: Exception) {
            Log.e("BookRepository", "API 호출 중 심각한 오류 발생", e)
            return@withContext emptyList()
        }
    }

    suspend fun getBestsellerList(
        page: Int = 1,
        maxResults: Int = 10
    ): List<Book> = withContext(Dispatchers.IO) {
        return@withContext getBookList(queryType = "Bestseller", page = page, maxResults = maxResults)
    }

    suspend fun getBookDetail(isbn: String): Book? = withContext(Dispatchers.IO) {
        try {
            if (TTBKEY.isBlank()) {
                Log.w("BookRepository", "알라딘 TTBKEY가 비어있습니다.")
                return@withContext null
            }

            Log.d("BookRepository", "도서 상세 정보 API 호출 시작: isbn='$isbn'")

            val response = bookService.getBookDetail(
                ttbKey = TTBKEY,
                itemId = isbn,
                itemIdType = ITEM_ID_TYPE,
                output = OUTPUT
            )

            Log.i("BookRepository", "도서 상세 정보 API 응답 성공. ISBN: $isbn")
            return@withContext response.books?.firstOrNull()
        } catch (e: Exception) {
            Log.e("BookRepository", "도서 상세 정보 API 호출 중 오류 발생. ISBN: $isbn", e)
            return@withContext null
        }
    }
}