package com.route.readers.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.GsonBuilder
import com.route.readers.BuildConfig
import com.route.readers.data.model.Book
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
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

    suspend fun getBookSearch(query: String, page: Int = 1, maxResults: Int = 10): List<Book> {
        return try {
            if (TTBKEY.isBlank()) {
                return emptyList()
            }

            val response = bookService.getBookSearch(
                ttbKey = TTBKEY,
                query = query,
                queryType = "Keyword",
                start = page,
                maxResults = maxResults,
                output = OUTPUT,
                version = VERSION
            )

            if (response.isSuccessful) {
                val books = (response.body()?.books ?: emptyList()).filter { it.isbn.isNotBlank() }
                if (books.isEmpty()) {
                    return emptyList()
                }

                coroutineScope {
                    books.map { book ->
                        async {
                            if (book.isbn.isNotBlank()) {
                                val detailBook = getBookDetail(book.isbn)
                                detailBook?.copy(categoryName = book.categoryName) ?: book
                            } else {
                                book
                            }
                        }
                    }.map { it.await() }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getBookList(): List<Book> {
        return try {
            if (TTBKEY.isBlank()) {
                return emptyList()
            }

            val response = bookService.getBookList(
                ttbKey = TTBKEY,
                queryType = "ItemNewAll",
                searchTarget = "Book",
                output = OUTPUT,
                version = VERSION
            )

            if (response.isSuccessful) {
                val basicBookList = (response.body()?.books ?: emptyList()).filter { it.isbn.isNotBlank() }
                if (basicBookList.isEmpty()) {
                    return emptyList()
                }

                coroutineScope {
                    basicBookList.map { book ->
                        async {
                            if (book.isbn.isNotBlank()) {
                                val detailBook = getBookDetail(book.isbn)
                                detailBook?.copy(categoryName = book.categoryName) ?: book
                            } else {
                                book
                            }
                        }
                    }.map { it.await() }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getBookDetail(isbn: String): Book? {
        return try {
            if (TTBKEY.isBlank()) {
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
                response.body()?.books?.firstOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }


}

