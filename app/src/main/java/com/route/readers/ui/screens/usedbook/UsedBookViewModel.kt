package com.route.readers.ui.screens.usedbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.UsedBook
import com.route.readers.data.remote.UsedBookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UsedBookViewModel : ViewModel() {
    private val repository = UsedBookRepository()
    
    private val _usedBooks = MutableStateFlow<List<UsedBook>>(emptyList())
    val usedBooks: StateFlow<List<UsedBook>> = _usedBooks
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadUsedBooks()
    }

    fun loadUsedBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            _usedBooks.value = repository.getUsedBooks()
            _isLoading.value = false
        }
    }
}
