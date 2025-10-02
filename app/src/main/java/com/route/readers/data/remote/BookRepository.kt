package com.route.readers.data.remote

import android.util.Log
import com.google.gson.GsonBuilder
import com.route.readers.BuildConfig
import com.route.readers.data.model.Book
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class BookRepository {
    
    private val gson = GsonBuilder().setLenient().create()
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("http://www.aladin.co.kr/ttb/api/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val bookService = retrofit.create(BookService::class.java)

    suspend fun getBookSearch(query: String): List<Book> {
        return try {
            if (TTBKEY.isBlank()) {
                Log.e("BookRepository", "API Key is missing")
                return emptyList()
            }
            
            val response = bookService.getBookSearch(
                ttbkey = TTBKEY,
                query = query
            )
            Log.d("BookRepository", "Search response: ${response.body()}")
            
            if (response.isSuccessful) {
                response.body()?.books ?: emptyList()
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
