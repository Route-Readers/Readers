package com.route.readers.data.remote

import android.util.Log
import com.route.readers.data.model.MyBook
import com.route.readers.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyLibraryRepository {

    private val _myBooks = MutableStateFlow<List<MyBook>>(emptyList())
    val myBooks: StateFlow<List<MyBook>> = _myBooks

    private val firestoreRepository = FirestoreRepository()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    var onLibraryUpdate: (() -> Unit)? = null

    init {
        repositoryScope.launch {
            syncWithFirestore()
        }
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

    suspend fun updateReadingProgress(isbn: String, currentPage: Int, isCompleted: Boolean): Boolean {
        return try {
            val success = firestoreRepository.updateReadingProgress(isbn, currentPage, isCompleted)

            if (success) {
                syncWithFirestore()
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

    suspend fun syncWithFirestore() {
        try {
            val firestoreBooks = firestoreRepository.getMyBooks()
            _myBooks.value = firestoreBooks
            Log.d("MyLibraryRepository", "Synced ${firestoreBooks.size} books from Firestore")
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error syncing with Firestore: ${e.message}", e)
        }
    }

    suspend fun getMyBooks(): List<MyBook> {
        syncWithFirestore()
        return _myBooks.value
    }
}
