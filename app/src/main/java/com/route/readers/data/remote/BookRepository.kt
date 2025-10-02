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

    suspend fun getBookSearch(query: String): List<Book> {
        return try {
            if (TTBKEY.isBlank()) {
                Log.e("BookRepository", "API Key is missing")
                return emptyList()
            }
            
            Log.d("BookRepository", "API Key: ${TTBKEY.take(10)}...")
            Log.d("BookRepository", "Search query: $query")
            
            val response = bookService.getBookSearch(
                ttbkey = TTBKEY,
                query = query
            )
            
            Log.d("BookRepository", "Response code: ${response.code()}")
            Log.d("BookRepository", "Response message: ${response.message()}")
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d("BookRepository", "Response body: $responseBody")
                
                val books = responseBody?.books ?: emptyList()
                Log.d("BookRepository", "Books count: ${books.size}")
                
                books.forEach { book ->
                    Log.d("BookRepository", "Book: ${book.title} by ${book.author}")
                }
                
                books
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("BookRepository", "API Error: ${response.code()} - ${response.message()}")
                Log.e("BookRepository", "Error body: $errorBody")
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
            
            Log.d("BookRepository", "Getting new books...")
            
            val response = bookService.getBookList(
                ttbkey = TTBKEY,
                querytype = QUERY_TYPE,
                searchtarget = SEARCH_TARGET,
                output = OUTPUT
            )
            
            Log.d("BookRepository", "New books response code: ${response.code()}")
            
            if (response.isSuccessful) {
                val books = response.body()?.books ?: emptyList()
                Log.d("BookRepository", "New books count: ${books.size}")
                books
            } else {
                Log.e("BookRepository", "New books API Error: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "New books error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getPageInfo(isbn: String): Int? {
        return try {
            Log.d("BookRepository", "Getting page info for ISBN: $isbn")
            
            val response = bookService.getBookDetail(
                ttbkey = TTBKEY,
                itemid = isbn,
                itemidtype = ITEM_ID_TYPE,
                output = OUTPUT
            )
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d("BookRepository", "Page info response: $responseBody")
                
                val book = responseBody?.books?.firstOrNull()
                val subInfo = book?.subInfo
                val itemPage = subInfo?.get("itemPage")
                
                Log.d("BookRepository", "SubInfo: $subInfo")
                Log.d("BookRepository", "ItemPage: $itemPage")
                
                when (itemPage) {
                    is Number -> itemPage.toInt()
                    is String -> itemPage.toIntOrNull()
                    else -> null
                }
            } else {
                Log.e("BookRepository", "Page info API Error: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Page info error: ${e.message}", e)
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
