package com.route.readers.ui.screens.mylibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.MyBook
import com.route.readers.data.remote.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyLibraryViewModel : ViewModel() {
    private val firestoreRepository = FirestoreRepository()
    
    private val _books = MutableStateFlow<List<MyBook>>(emptyList())
    val books: StateFlow<List<MyBook>> = _books
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    init {
        loadBooks()
    }
    
    fun loadBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            _books.value = firestoreRepository.getMyBooks()
            _isLoading.value = false
        }
    }
    
    fun updateReadingProgress(isbn: String, currentPage: Int) {
        viewModelScope.launch {
            firestoreRepository.updateReadingProgress(isbn, currentPage)
            loadBooks() // 업데이트 후 다시 로드
        }
    }
}
