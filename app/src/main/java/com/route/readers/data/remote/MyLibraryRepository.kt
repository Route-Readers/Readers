package com.route.readers.data.remote

import android.util.Log
import com.route.readers.data.model.Book
import com.route.readers.data.model.MyBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MyLibraryRepository {
    
    private val _myBooks = MutableStateFlow<List<Book>>(emptyList())
    val myBooks: StateFlow<List<Book>> = _myBooks
    
    private val firestoreRepository = FirestoreRepository()
    
    suspend fun addBookToLibrary(book: Book): Boolean {
        return try {
            val currentBooks = _myBooks.value.toMutableList()
            
            // 이미 있는 책인지 확인 (ISBN으로 중복 체크)
            if (!currentBooks.any { it.isbn == book.isbn }) {
                // 페이지 정보 추출 (개선된 방식)
                val totalPages = book.extractPageCount().let { pages ->
                    if (pages > 0) pages else 0 // 페이지 정보가 없으면 0으로 설정
                }
                
                Log.d("MyLibraryRepository", "Book: ${book.title}, Pages: $totalPages (itemPage: ${book.itemPage})")
                
                val bookWithProgress = book.copy(
                    totalPages = totalPages,
                    currentPage = 0,
                    progress = 0
                )
                currentBooks.add(bookWithProgress)
                _myBooks.value = currentBooks
                
                // Firestore에도 저장
                val myBook = MyBook(
                    id = book.isbn,
                    title = book.title,
                    author = book.author,
                    cover = book.cover,
                    isbn = book.isbn,
                    totalPages = totalPages,
                    currentPage = 0,
                    addedDate = System.currentTimeMillis(),
                    lastReadDate = System.currentTimeMillis()
                )
                
                val firestoreSuccess = firestoreRepository.addBookToLibrary(myBook)
                Log.d("MyLibraryRepository", "Book added to Firestore: $firestoreSuccess")
                
                return firestoreSuccess
            }
            true
        } catch (e: Exception) {
            Log.e("MyLibraryRepository", "Error adding book: ${e.message}", e)
            false
        }
    }
    
    suspend fun updateReadingProgress(isbn: String, currentPage: Int): Boolean {
        return try {
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
                
                // Firestore에도 업데이트
                return firestoreRepository.updateReadingProgress(isbn, currentPage)
            }
            false
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
                    itemPage = myBook.totalPages.toString(),
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
}
