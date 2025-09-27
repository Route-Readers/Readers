package com.route.readers.ui.screens.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.Book
import com.route.readers.data.remote.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BookViewModel : ViewModel() {
    
    private val bookRepository = BookRepository()
    
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    fun searchBooks(query: String) {
        if (query.isBlank()) {
            Log.w("BookViewModel", "Empty query provided")
            return
        }
        
        Log.d("BookViewModel", "검색 시작: $query")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                Log.d("BookViewModel", "API 호출 중...")
                val result = bookRepository.getBookSearch(query.trim())
                Log.d("BookViewModel", "검색 결과: ${result.size}개")
                _books.value = result
                if (result.isEmpty()) {
                    _errorMessage.value = "검색 결과가 없습니다"
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "검색 에러: ${e.message}", e)
                _errorMessage.value = "검색 중 오류가 발생했습니다: ${e.message}"
                _books.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun getNewBooks() {
        Log.d("BookViewModel", "신간 도서 불러오기 시작")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                Log.d("BookViewModel", "신간 API 호출 중...")
                val result = bookRepository.getBookList()
                Log.d("BookViewModel", "신간 결과: ${result.size}개")
                _books.value = result
                if (result.isEmpty()) {
                    _errorMessage.value = "신간 도서를 불러올 수 없습니다"
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "신간 에러: ${e.message}", e)
                _errorMessage.value = "신간 도서 불러오기 중 오류: ${e.message}"
                _books.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
