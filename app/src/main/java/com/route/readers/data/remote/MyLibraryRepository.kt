package com.route.readers.data.remote

import android.util.Log
import com.route.readers.data.model.Book
import com.route.readers.data.model.MyBook
import com.route.readers.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MyLibraryRepository {
    
    private val _myBooks = MutableStateFlow<List<Book>>(emptyList())
    val myBooks: StateFlow<List<Book>> = _myBooks
    
    private val firestoreRepository = FirestoreRepository()
    
    suspend fun addBookToLibrary(book: MyBook): Boolean {
        return try {
            val firestoreSuccess = firestoreRepository.addBookToLibrary(book)
            if (firestoreSuccess) {
                syncWithFirestore()
                WidgetUpdateHelper.updateAllWidgets()
            }
            firestoreSuccess
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error adding book: ${e.message}", e)
            false
        }
    }
    
    suspend fun updateReadingProgress(isbn: String, currentPage: Int): Boolean {
        return try {
            // Firestore에서 직접 업데이트
            val success = firestoreRepository.updateReadingProgress(isbn, currentPage)
            
            if (success) {
                // 로컬 상태도 업데이트
                val currentBooks = _myBooks.value.toMutableList()
                val bookIndex = currentBooks.indexOfFirst { it.isbn == isbn }
                
                if (bookIndex != -1) {
                    val book = currentBooks[bookIndex]
                    val progress = if (book.totalPages > 0) {
                        ((currentPage.toFloat() / book.totalPages) * 100).toInt()
                    } else 0
                    
                    currentBooks[bookIndex] = book.copy(
                        currentPage = currentPage,
                        progress = progress
                    )
                    _myBooks.value = currentBooks
                }
                
                // 위젯 업데이트
                WidgetUpdateHelper.updateAllWidgets()
            }
            
            success
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error updating progress: ${e.message}", e)
            false
        }
    }
    
    suspend fun removeBookFromLibrary(isbn: String): Boolean {
        return try {
            val currentBooks = _myBooks.value.toMutableList()
            currentBooks.removeAll { it.isbn == isbn }
            _myBooks.value = currentBooks
            
            // 위젯 업데이트
            WidgetUpdateHelper.updateAllWidgets()
            
            // Firestore에서도 삭제
            return firestoreRepository.removeBookFromLibrary(isbn)
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error removing book: ${e.message}", e)
            false
        }
    }
    
    suspend fun isBookInLibrary(isbn: String): Boolean {
        return try {
            // 메모리상 확인
            val inMemory = _myBooks.value.any { it.isbn == isbn }
            if (inMemory) return true
            
            // Firestore에서 확인
            val firestoreBooks = firestoreRepository.getMyBooks()
            return firestoreBooks.any { it.isbn == isbn }
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error checking book existence: ${e.message}", e)
            _myBooks.value.any { it.isbn == isbn }
        }
    }
    
    suspend fun syncWithFirestore() {
        try {
            val firestoreBooks = firestoreRepository.getMyBooks()
            val memoryBooks = firestoreBooks.map { myBook ->
                Book(
                    title = myBook.title,
                    author = myBook.author,
                    description = "",
                    isbn = myBook.isbn,
                    cover = myBook.cover,
                    categoryName = null,
                    itemPage = myBook.totalPages,
                    currentPage = myBook.currentPage,
                    totalPages = myBook.totalPages,
                    progress = myBook.progressPercentage
                )
            }
            _myBooks.value = memoryBooks
            Log.d("MyLibraryRepository", "Synced ${memoryBooks.size} books from Firestore")
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error syncing with Firestore: ${e.message}", e)
        }
    }

    suspend fun getMyBooks(): List<MyBook> {
        return try {
            firestoreRepository.getMyBooks()
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error getting books: ${e.message}", e)
            emptyList()
        }
    }
}
