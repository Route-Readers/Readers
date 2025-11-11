package com.route.readers.ui.screens.bookclub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.ChatMessage
import com.route.readers.data.model.User
import com.route.readers.data.remote.ChatRepository
import com.route.readers.data.remote.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookClubChatViewModel : ViewModel() {
    
    private val chatRepository = ChatRepository()
    private val firestoreRepository = FirestoreRepository()
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
                // 사용자 정보를 불러올 수 없는 경우 기본값 사용
                currentUserInfo = User(
                    uid = currentUser.uid,
                    nickname = currentUser.displayName ?: "익명",
                    profileImageUrl = currentUser.photoUrl?.toString()
                )
            }
        }
    }
    
    fun loadMessages(bookClubId: String) {
        if (bookClubId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "잘못된 북클럽 ID입니다."
            )
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
                // 캐시된 사용자 정보가 없으면 다시 로드
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
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            error = null
                        )
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
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)
