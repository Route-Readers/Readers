package com.route.readers.data.remote

import android.util.Log
import com.google.gson.GsonBuilder
import com.route.readers.BuildConfig
import com.route.readers.data.model.Book
import com.route.readers.data.model.BookListDTO
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BookRepository {

    // Retrofit과 BookService 인스턴스를 lazy 초기화를 통해 생성합니다.
    private val bookService: BookService by lazy {
        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://www.aladin.co.kr/ttb/api/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        // 별도의 파일에 정의된 BookService 인터페이스를 사용합니다.
        retrofit.create(BookService::class.java)
    }

    suspend fun getBookSearch(query: String, maxResults: Int = 20): List<Book> {
        return try {
            if (TTBKEY.isBlank()) {
                Log.e("BookRepository", "TTBKey is missing.")
                return emptyList()
            }
            // API 명세에 맞는 파라미터 이름을 사용해야 합니다. (ttbKey -> ttbkey 등)
            // BookService.kt 파일에 정의된 함수를 호출합니다.
            val response = bookService.getBookSearch(
                ttbKey = TTBKEY,
                query = query,
                maxResults = maxResults
            )
            if (response.isSuccessful) {
                response.body()?.books ?: emptyList()
            } else {
                Log.e("BookRepository", "Search API Error: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Search failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getBookList(): List<Book> {
        return try {
            if (TTBKEY.isBlank()) {
                Log.e("BookRepository", "TTBKey is missing.")
                return emptyList()
            }
            val response = bookService.getBookList(
                ttbKey = TTBKEY,
                queryType = QUERY_TYPE,
                searchTarget = SEARCH_TARGET,
                output = OUTPUT
            )
            if (response.isSuccessful) {
                response.body()?.books ?: emptyList()
            } else {
                Log.e("BookRepository", "List API Error: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Get list failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getBookDetail(itemid: String): Book? {
        return try {
            if (TTBKEY.isBlank()) {
                Log.e("BookRepository", "TTBKey is missing.")
                return null
            }
            val response = bookService.getBookDetail(
                ttbKey = TTBKEY,
                itemId = itemid,
                itemIdType = ITEM_ID_TYPE,
                output = OUTPUT
            )
            if (response.isSuccessful) {
                response.body()?.books?.firstOrNull()
            } else {
                Log.e("BookRepository", "Detail API Error: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Get detail failed: ${e.message}", e)
            null
        }
    }

    // companion object는 API 키와 같은 상수들을 보관합니다.
    private companion object {
        private val TTBKEY = BuildConfig.ALADIN_TTB_KEY
        private const val QUERY_TYPE = "ItemNewSpecial"
        private const val SEARCH_TARGET = "Book"
        private const val ITEM_ID_TYPE = "ISBN13"
        private const val OUTPUT = "js"
    }
}
