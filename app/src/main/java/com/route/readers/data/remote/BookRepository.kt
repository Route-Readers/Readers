package com.route.readers.data.remote

import android.util.Log
import com.google.gson.GsonBuilder
import com.route.readers.BuildConfig
import com.route.readers.data.model.Book
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BookRepository {

    companion object {
        private val TTBKEY = BuildConfig.ALADIN_TTB_KEY
        private const val QUERY_TYPE = "ItemNewAll"
        private const val SEARCH_TARGET = "Book"
        private const val ITEM_ID_TYPE = "ISBN"
        private const val OUTPUT = "js"
        private const val VERSION = "20131101"
    }

    private val bookService: BookService by lazy {
        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://www.aladin.co.kr/ttb/api/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        retrofit.create(BookService::class.java)
    }

    suspend fun getBookSearch(query: String, page: Int = 1, maxResults: Int = 10): List<Book> {
        return try {
            if (TTBKEY.isBlank()) {
                Log.e("BookRepository", "TTBKey is missing.")
                return emptyList()
            }

            val response = bookService.getBookSearch(
                ttbKey = TTBKEY,
                query = query,
                start = page,
                maxResults = maxResults,
                output = OUTPUT,
                version = VERSION
            )

            if (response.isSuccessful) {
                val books = response.body()?.books ?: emptyList()
                coroutineScope {
                    books.map { book ->
                        async {
                            if (book.isbn.isNotBlank()) {
                                getBookDetail(book.isbn) ?: book
                            } else {
                                book
                            }
                        }
                    }.map { it.await() }
                }
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
                output = OUTPUT,
                version = VERSION
            )

            if (response.isSuccessful) {
                val basicBookList = response.body()?.books ?: emptyList()
                Log.d("BookRepository", "신간 리스트에서 ${basicBookList.size}권의 책을 받았습니다.")
                coroutineScope {
                    basicBookList.map { book ->
                        async {
                            if (book.isbn.isNotBlank()) {
                                getBookDetail(book.isbn) ?: book
                            } else {
                                book
                            }
                        }
                    }.map { it.await() }
                }
            } else {
                Log.e("BookRepository", "List API Error: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Get list failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getBookDetail(isbn: String): Book? {
        return try {
            if (TTBKEY.isBlank()) {
                Log.e("BookRepository", "TTBKey is missing.")
                return null
            }
            val response = bookService.getBookDetail(
                ttbKey = TTBKEY,
                itemId = isbn,
                itemIdType = ITEM_ID_TYPE,
                output = OUTPUT,
                version = VERSION,
                optResult = "subInfo"
            )

            if (response.isSuccessful) {
                val book = response.body()?.books?.firstOrNull()
                Log.d("BookRepository", "상세 조회 [${book?.title}]: 페이지 정보 -> ${book?.subInfo?.itemPage}")
                book
            } else {
                Log.e("BookRepository", "Detail API Error: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Get detail failed: ${e.message}", e)
            null
        }
    }
}
