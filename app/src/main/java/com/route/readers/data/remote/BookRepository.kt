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

    suspend fun getBookSearch(query: String, page: Int = 1, maxResults: Int = 10): List<Book> {
        return try {
            Log.d("BookRepository", "=== API 호출 시작 ===")
            Log.d("BookRepository", "Query: $query")
            Log.d("BookRepository", "API Key: ${if (TTBKEY.isBlank()) "없음" else "있음"}")
            
            if (TTBKEY.isBlank()) {
                Log.e("BookRepository", "TTBKey is missing.")
                return emptyList()
            }
            // API 명세에 맞는 파라미터 이름을 사용해야 합니다. (ttbKey -> ttbkey 등)
            // BookService.kt 파일에 정의된 함수를 호출합니다.
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
