package com.route.readers.data.remote

import android.util.Log
import com.google.gson.GsonBuilder
import com.route.readers.BuildConfig
import com.route.readers.data.model.Book
import com.route.readers.data.model.BookListDTO
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BookRepository {
    
    private val gson = GsonBuilder().setLenient().create()
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("http://www.aladin.co.kr/ttb/api/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val bookService: BookService by lazy {
        retrofit.create(BookService::class.java)
    }

    suspend fun getBookSearch(query: String, page: Int = 1, maxResults: Int = 10): List<Book> {
        return try {
            Log.d("BookRepository", "=== API 호출 시작 ===")
            Log.d("BookRepository", "Query: $query")
            
            if (TTBKEY.isBlank()) {
                Log.e("BookRepository", "TTBKey is missing.")
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
                            Log.d("BookRepository", "${book.title}: 상세조회 시작")
                            val detailBook = getBookDetail(book.isbn)
                            
                            if (detailBook != null && detailBook.subInfo?.itemPage != null) {
                                Log.d("BookRepository", "${book.title}: 상세조회로 페이지 정보 획득 - ${detailBook.subInfo.itemPage}페이지")
                                val updatedBook = book.copy(subInfo = detailBook.subInfo)
                                Log.d("BookRepository", "${book.title}: 최종 extractPageCount = ${updatedBook.extractPageCount()}")
                                updatedBook
                            } else {
                                Log.d("BookRepository", "${book.title}: 페이지 정보 없음 (detailBook=$detailBook, subInfo=${detailBook?.subInfo})")
                                book
                            }
                        } else {
                            Log.d("BookRepository", "${book.title}: ISBN 없음")
                            book
                        }
                    } catch (e: Exception) {
                        Log.w("BookRepository", "${book.title}: 상세조회 실패 - ${e.message}")
                        book
                    }
                }
                
                return booksWithPages
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
                ttbkey = TTBKEY,
                querytype = QUERY_TYPE,
                searchtarget = SEARCH_TARGET,
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
            Log.d("BookRepository", "=== getBookDetail 호출: $itemid ===")
            if (TTBKEY.isBlank()) {
                Log.e("BookRepository", "TTBKey is missing.")
                return null
            }
            
            val response = bookService.getBookDetail(
                ttbkey = TTBKEY,
                itemid = itemid,
                itemidtype = ITEM_ID_TYPE,
                output = OUTPUT
            )
            
            Log.d("BookRepository", "Detail API 응답 코드: ${response.code()}")
            
            if (response.isSuccessful) {
                val book = response.body()?.books?.firstOrNull()
                Log.d("BookRepository", "Detail API 결과: title=${book?.title}")
                Log.d("BookRepository", "Detail API subInfo: ${book?.subInfo}")
                Log.d("BookRepository", "Detail API itemPage: ${book?.subInfo?.itemPage}")
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

    private companion object {
        private val TTBKEY = BuildConfig.ALADIN_TTB_KEY
        private const val QUERY_TYPE = "ItemNewSpecial"
        private const val SEARCH_TARGET = "Book"
        private const val ITEM_ID_TYPE = "ISBN13"
        private const val OUTPUT = "js"
    }
}
