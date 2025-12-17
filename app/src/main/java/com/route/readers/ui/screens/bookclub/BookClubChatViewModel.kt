package com.route.readers.ui.screens.bookclub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.Book
import com.route.readers.data.model.BookClub
import com.route.readers.data.model.ChatMessage
import com.route.readers.data.model.User
import com.route.readers.data.remote.BookRepository
import com.route.readers.data.remote.ChatRepository
import com.route.readers.data.remote.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BookClubChatViewModel : ViewModel() {
    
    private val chatRepository = ChatRepository()
    private val firestoreRepository = FirestoreRepository()
    private val bookRepository = BookRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow(BookClubChatUiState())
    val uiState: StateFlow<BookClubChatUiState> = _uiState.asStateFlow()
    
    private var currentUserInfo: User? = null
    
    init {
        loadCurrentUserInfo()
    }
    
    private fun loadCurrentUserInfo() {
        viewModelScope.launch {
            val currentUser = auth.currentUser ?: return@launch
            try {
                currentUserInfo = firestoreRepository.getUserProfile(currentUser.uid)
            } catch (e: Exception) {
                currentUserInfo = User(
                    uid = currentUser.uid,
                    nickname = currentUser.displayName ?: "익명",
                    profileImageUrl = currentUser.photoUrl?.toString()
                )
            }
        }
    }
    
    fun loadBookClubInfo(bookClubId: String) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("bookClubs").document(bookClubId).get().await()
                val bookClub = doc.toObject(BookClub::class.java)?.copy(id = doc.id)
                _uiState.value = _uiState.value.copy(bookClub = bookClub)
            } catch (e: Exception) {
                // 무시
            }
        }
    }
    
    fun searchBooks(query: String) {
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingBooks = true)
            try {
                val results = bookRepository.getBookSearch(query, maxResults = 5)
                _uiState.value = _uiState.value.copy(searchResults = results, isSearchingBooks = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearchingBooks = false)
            }
        }
    }
    
    fun updateBookClubBook(bookClubId: String, book: Book) {
        viewModelScope.launch {
            try {
                firestore.collection("bookClubs").document(bookClubId).update(
                    mapOf(
                        "currentBook" to book.title,
                        "currentBookAuthor" to book.author,
                        "currentBookCover" to book.cover,
                        "currentBookGenre" to (book.categoryName ?: ""),
                        "currentBookDescription" to book.description
                    )
                ).await()
                loadBookClubInfo(bookClubId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "책 정보 업데이트에 실패했습니다.")
            }
        }
    }
    
    fun saveBookHistoryForDates(bookClubId: String, dates: List<String>, bookCover: String) {
        viewModelScope.launch {
            try {
                val currentHistory = _uiState.value.bookClub?.bookHistory?.toMutableMap() ?: mutableMapOf()
                dates.forEach { date ->
                    currentHistory[date] = bookCover
                }
                firestore.collection("bookClubs").document(bookClubId).update(
                    "bookHistory", currentHistory
                ).await()
                loadBookClubInfo(bookClubId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "책 기록 저장에 실패했습니다.")
            }
        }
    }
    
    fun deleteBookHistoryForDates(bookClubId: String, dates: List<String>) {
        viewModelScope.launch {
            try {
                val currentHistory = _uiState.value.bookClub?.bookHistory?.toMutableMap() ?: mutableMapOf()
                dates.forEach { date ->
                    currentHistory.remove(date)
                }
                firestore.collection("bookClubs").document(bookClubId).update(
                    "bookHistory", currentHistory
                ).await()
                loadBookClubInfo(bookClubId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "책 기록 삭제에 실패했습니다.")
            }
        }
    }
    
    fun clearSearchResults() {
        _uiState.value = _uiState.value.copy(searchResults = emptyList())
    }
    
    fun loadMessages(bookClubId: String) {
        if (bookClubId.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "잘못된 북클럽 ID입니다.")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                chatRepository.getChatMessages(bookClubId).collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "메시지를 불러오는데 실패했습니다."
                )
            }
        }
    }
    
    fun sendMessage(bookClubId: String, messageText: String) {
        if (messageText.isBlank()) return
        if (bookClubId.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "잘못된 북클럽 ID입니다.")
            return
        }
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(error = "로그인이 필요합니다.")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            
            try {
                if (currentUserInfo == null) {
                    loadCurrentUserInfo()
                }
                
                val userInfo = currentUserInfo ?: User(
                    uid = currentUser.uid,
                    nickname = currentUser.displayName ?: "익명",
                    profileImageUrl = currentUser.photoUrl?.toString()
                )
                
                val message = ChatMessage(
                    bookClubId = bookClubId,
                    senderId = currentUser.uid,
                    senderName = userInfo.nickname,
                    senderProfileImage = userInfo.profileImageUrl ?: "",
                    message = messageText.trim(),
                    timestamp = System.currentTimeMillis()
                )
                
                chatRepository.sendMessage(bookClubId, message).fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isSending = false, error = null)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            error = error.message ?: "메시지 전송에 실패했습니다."
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = e.message ?: "메시지 전송 중 오류가 발생했습니다."
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun retryLoadMessages(bookClubId: String) {
        clearError()
        loadMessages(bookClubId)
    }
}

data class BookClubChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val bookClub: BookClub? = null,
    val searchResults: List<Book> = emptyList(),
    val isSearchingBooks: Boolean = false,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)
