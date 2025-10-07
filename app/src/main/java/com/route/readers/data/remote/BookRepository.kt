package com.route.readers.data.remote

import android.util.Log
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
        private const val QUERY_TYPE = "ItemNewAll"
        private const val SEARCH_TARGET = "Book"
        private const val ITEM_ID_TYPE = "ISBN"
        private const val OUTPUT = "js"
        private const val VERSION = "20131101"
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId: String
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
                start = page,
                maxResults = maxResults,
                output = OUTPUT,
                version = VERSION
            )

            if (response.isSuccessful) {
                val books = response.body()?.books ?: emptyList()
                val detailedBooks = coroutineScope {
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
                applyFavoriteStatus(detailedBooks)
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
                queryType = QUERY_TYPE,
                searchTarget = SEARCH_TARGET,
                output = OUTPUT,
                version = VERSION
            )

            if (response.isSuccessful) {
                val basicBookList = response.body()?.books ?: emptyList()
                val detailedBooks = coroutineScope {
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
                applyFavoriteStatus(detailedBooks)
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

    suspend fun toggleFavoriteStatus(book: Book) {
        if (userId.isBlank() || book.isbn.isBlank()) return

        val favoriteRef = db.collection("users").document(userId)
            .collection("favorites").document(book.isbn)

        if (book.isFavorite) {
            val bookData = book.copy(isFavorite = false)
            favoriteRef.set(bookData).await()
        } else {
            favoriteRef.delete().await()
        }
    }

    suspend fun getFavoriteBooks(): List<Book> {
        if (userId.isBlank()) return emptyList()

        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("favorites").get().await()
            snapshot.documents.mapNotNull { document ->
                document.toObject(Book::class.java)?.copy(isFavorite = true)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun applyFavoriteStatus(books: List<Book>): List<Book> {
        if (userId.isBlank()) return books

        val favoriteIsbns = getFavoriteBooks().map { it.isbn }.toSet()
        return books.map { book ->
            if (favoriteIsbns.contains(book.isbn)) {
                book.copy(isFavorite = true)
            } else {
                book
            }
        }
    }

    suspend fun deleteFavoriteBooks(userId: String, bookIds: List<String>) {
        if (userId.isBlank() || bookIds.isEmpty()) return

        val favoriteCollectionRef = db.collection("users").document(userId).collection("favorites")
        val batch = db.batch()

        bookIds.forEach { bookId ->
            batch.delete(favoriteCollectionRef.document(bookId))
        }

        batch.commit().await()
    }
}
