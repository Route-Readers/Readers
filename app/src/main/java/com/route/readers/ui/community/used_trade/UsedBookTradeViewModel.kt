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
    val books: List<UsedBook> = emptyList(),
    val isLoading: Boolean = true
)

class UsedBookTradeViewModel : ViewModel() {
    private val repository = UsedBookRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val _uiState = MutableStateFlow(UsedBookTradeUiState())
    val uiState: StateFlow<UsedBookTradeUiState> = _uiState.asStateFlow()
    
    init {
        loadBooksRealtime()
    }
    
    private fun loadBooksRealtime() {
        viewModelScope.launch {
            repository.getUsedBooksFlow().collect { books ->
                android.util.Log.d("UsedBookTrade", "Real-time update: ${books.size} books")
                _uiState.value = _uiState.value.copy(
                    books = books,
                    isLoading = false
                )
            }
        }
    }
    
    fun addBook(title: String, author: String, condition: String, price: Int, description: String, bookCover: String?) {
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
                    bookCover = bookCover,
                    condition = condition,
                    price = price,
                    description = description,
                    status = "판매중"
                )
                
                android.util.Log.d("UsedBookTrade", "Creating book: ${newBook.id}")
                repository.createUsedBook(newBook)
                android.util.Log.d("UsedBookTrade", "Book created successfully")
            } catch (e: Exception) {
                android.util.Log.e("UsedBookTrade", "Error adding book", e)
            }
        }
    }
    
    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            repository.deleteUsedBook(bookId)
        }
    }

    fun updateBook(bookId: String, title: String, author: String, condition: String, price: Int, description: String, bookCover: String?) {
        viewModelScope.launch {
            try {
                val book = repository.getUsedBook(bookId)
                if (book != null) {
                    val updatedBook = book.copy(
                        bookTitle = title,
                        bookAuthor = author,
                        condition = condition,
                        price = price,
                        description = description,
                        bookCover = bookCover
                    )
                    repository.updateUsedBook(updatedBook)
                }
            } catch (e: Exception) {
                android.util.Log.e("UsedBookTrade", "Error updating book", e)
            }
        }
    }
    
    private val _buyResult = MutableStateFlow<BuyResult?>(null)
    val buyResult: StateFlow<BuyResult?> = _buyResult.asStateFlow()

    fun clearBuyResult() {
        _buyResult.value = null
    }

    fun buyBook(bookId: String, buyerId: String) {
        viewModelScope.launch {
            try {
                val book = repository.getUsedBook(bookId)
                if (book == null) {
                    _buyResult.value = BuyResult.Error("게시글을 찾을 수 없습니다.")
                    return@launch
                }

                val buyerDoc = db.collection("users").document(buyerId).get().await()
                val buyerTokens = buyerDoc.getLong("tokens")?.toInt() ?: 0

                if (buyerTokens < book.price) {
                    _buyResult.value = BuyResult.InsufficientTokens(buyerTokens, book.price)
                    return@launch
                }

                // 트랜잭션으로 토큰 이동 + 상태 변경
                db.runTransaction { transaction ->
                    val buyerRef = db.collection("users").document(buyerId)
                    val sellerRef = db.collection("users").document(book.sellerId)
                    val bookRef = db.collection("usedBooks").document(bookId)

                    transaction.update(buyerRef, "tokens", buyerTokens - book.price)
                    
                    val sellerDoc = transaction.get(sellerRef)
                    val sellerTokens = sellerDoc.getLong("tokens")?.toInt() ?: 0
                    transaction.update(sellerRef, "tokens", sellerTokens + book.price)
                    
                    transaction.update(bookRef, mapOf("status" to "판매완료", "buyerId" to buyerId))
                }.await()

                _buyResult.value = BuyResult.Success
            } catch (e: Exception) {
                _buyResult.value = BuyResult.Error("구매 중 오류가 발생했습니다.")
            }
        }
    }
}

sealed class BuyResult {
    object Success : BuyResult()
    data class InsufficientTokens(val current: Int, val required: Int) : BuyResult()
    data class Error(val message: String) : BuyResult()
}
