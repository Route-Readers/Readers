package com.route.readers.ui.community.used_trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.UsedBook
import com.route.readers.data.remote.UsedBookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UsedBookTradeUiState(
    val books: List<UsedBook> = emptyList()
)

class UsedBookTradeViewModel : ViewModel() {
    private val repository = UsedBookRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val _uiState = MutableStateFlow(UsedBookTradeUiState())
    val uiState: StateFlow<UsedBookTradeUiState> = _uiState.asStateFlow()
    
    init {
        loadBooks()
    }
    
    private fun loadBooks() {
        viewModelScope.launch {
            val books = repository.getUsedBooks()
            _uiState.value = _uiState.value.copy(books = books)
        }
    }
    
    fun addBook(title: String, author: String, condition: String, price: Int, description: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("UsedBookTrade", "addBook started")
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    android.util.Log.e("UsedBookTrade", "User not logged in")
                    return@launch
                }
                
                val userDoc = db.collection("users").document(currentUser.uid).get().await()
                val userName = userDoc.getString("nickname") ?: "익명"
                val userProfileImage = userDoc.getString("profileImageUrl")
                
                android.util.Log.d("UsedBookTrade", "User: $userName")
                
                val newBook = UsedBook(
                    id = System.currentTimeMillis().toString(),
                    sellerId = currentUser.uid,
                    sellerName = userName,
                    sellerProfileImage = userProfileImage,
                    bookTitle = title,
                    bookAuthor = author,
                    condition = condition,
                    price = price,
                    description = description,
                    status = "판매중"
                )
                
                android.util.Log.d("UsedBookTrade", "Creating book: ${newBook.id}")
                repository.createUsedBook(newBook)
                android.util.Log.d("UsedBookTrade", "Book created, reloading...")
                loadBooks()
            } catch (e: Exception) {
                android.util.Log.e("UsedBookTrade", "Error adding book", e)
            }
        }
    }
    
    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            repository.deleteUsedBook(bookId)
            loadBooks()
        }
    }
    
    fun buyBook(bookId: String, buyerId: String) {
        viewModelScope.launch {
            repository.updateBookStatus(bookId, "판매완료", buyerId)
            loadBooks()
        }
    }
}
