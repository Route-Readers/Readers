package com.route.readers.data.remote

import android.util.Log
import com.google.gson.GsonBuilder
import com.route.readers.BuildConfig
import com.route.readers.data.model.Book
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BookRepository {
    
    private val gson = GsonBuilder().setLenient().create()
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("http://www.aladin.co.kr/ttb/api/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val bookService = retrofit.create(BookService::class.java)

    suspend fun getBookSearch(query: String, page: Int = 1, maxResults: Int = 10): List<Book> {
        return try {
            Log.d("BookRepository", "=== API 호출 시작 ===")
            Log.d("BookRepository", "Query: $query")
            Log.d("BookRepository", "API Key: ${if (TTBKEY.isBlank()) "없음" else "있음"}")
            
            if (TTBKEY.isBlank()) {
                Log.e("BookRepository", "API Key is missing")
                return emptyList()
            }
            
            val response = bookService.getBookSearch(
                ttbkey = TTBKEY,
                query = query,
                start = page,
                maxResults = maxResults
            )
            
            Log.d("BookRepository", "API 응답 코드: ${response.code()}")
            
            if (response.isSuccessful) {
                val books = response.body()?.books ?: emptyList()
                Log.d("BookRepository", "받은 책 개수: ${books.size}")
                
                // 각 책의 상세 정보를 가져와서 페이지 정보 보완
                val booksWithPages = books.map { book ->
                    try {
                        if (book.isbn.isNotBlank()) {
                            val detailBook = getBookDetail(book.isbn)
                            if (detailBook != null && detailBook.subInfo?.itemPage != null) {
                                Log.d("BookRepository", "${book.title}: 상세조회로 페이지 정보 획득 - ${detailBook.subInfo.itemPage}페이지")
                                book.copy(subInfo = detailBook.subInfo)
                            } else {
                                Log.d("BookRepository", "${book.title}: 페이지 정보 없음")
                                book
                            }
                        } else {
                            book
                        }
                    } catch (e: Exception) {
                        Log.w("BookRepository", "${book.title}: 상세조회 실패 - ${e.message}")
                        book
                    }
                }
                
                return booksWithPages
            } else {
                Log.e("BookRepository", "API Error: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Search error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getBookList(): List<Book> {
        return try {
            if (TTBKEY.isBlank()) {
                Log.e("BookRepository", "API Key is missing")
                return emptyList()
            }
            
            val response = bookService.getBookList(
                ttbkey = TTBKEY,
                querytype = QUERY_TYPE,
                searchtarget = SEARCH_TARGET,
                output = OUTPUT
            )
            Log.d("BookRepository", "List response: ${response.body()}")
            
            if (response.isSuccessful) {
                response.body()?.books ?: emptyList()
            } else {
                Log.e("BookRepository", "API Error: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "List error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getBookDetail(itemid: String): Book? {
        return try {
            val response = bookService.getBookDetail(
                ttbkey = TTBKEY,
                itemid = itemid,
                itemidtype = ITEM_ID_TYPE,
                output = OUTPUT
            )
            response.body()?.books?.firstOrNull()
        } catch (e: Exception) {
            Log.e("BookRepository", "Detail error: ${e.message}")
            null
        }
    }

    private companion object {
        private val TTBKEY = BuildConfig.ALADIN_TTB_KEY
        private const val QUERY_TYPE = "ItemNewSpecial"
        private const val SEARCH_TARGET = "Book"
        private const val ITEM_ID_TYPE = "ISBN13"
        private const val OUTPUT = "js"
    }
}
