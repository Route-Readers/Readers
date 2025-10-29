package com.route.readers.data.remote

import android.util.Log
import com.route.readers.data.model.Book
import com.route.readers.data.model.MyBook
import com.route.readers.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MyLibraryRepository {
    
    private val _myBooks = MutableStateFlow<List<MyBook>>(emptyList())
    val myBooks: StateFlow<List<MyBook>> = _myBooks
    
    private val firestoreRepository = FirestoreRepository()

    var onLibraryUpdate: (() -> Unit)? = null

    init {
        syncWithFirestore()
    }
    
    suspend fun addBookToLibrary(book: MyBook): Boolean {
        return try {
            val firestoreSuccess = firestoreRepository.addBookToLibrary(book)
            if (firestoreSuccess) {
                syncWithFirestore()
                WidgetUpdateHelper.updateAllWidgets()
                onLibraryUpdate?.invoke()
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
                syncWithFirestore()
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
            val success = firestoreRepository.removeBookFromLibrary(isbn)
            if (success) {
                syncWithFirestore()
                WidgetUpdateHelper.updateAllWidgets()
                onLibraryUpdate?.invoke()
            }
            success
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error removing book: ${e.message}", e)
            false
        }
    }
    
    suspend fun isBookInLibrary(isbn: String): Boolean {
        return try {
            _myBooks.value.any { it.isbn == isbn }
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error checking book existence: ${e.message}", e)
            false
        }
    }
    
    fun syncWithFirestore() {
        
    }

    suspend fun getMyBooks(): List<MyBook> {
        return try {
            val books = firestoreRepository.getMyBooks()
            _myBooks.value = books
            books
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error getting books: ${e.message}", e)
            emptyList()
        }
    }
}
