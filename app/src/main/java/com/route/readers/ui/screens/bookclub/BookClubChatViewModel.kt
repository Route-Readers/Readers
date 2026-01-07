package com.route.readers.ui.screens.bookclub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.Book
import com.route.readers.data.model.BookClub
import com.route.readers.data.model.BookClubPermission
import com.route.readers.data.model.BookClubPermissions
import com.route.readers.data.model.BookClubRole
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
    
    val currentUserId: String?
        get() = auth.currentUser?.uid
    
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
    
    private fun updateUserRole() {
        val bookClub = _uiState.value.bookClub ?: return
        val userId = currentUserId ?: return
        val role = BookClubPermissions.getUserRole(bookClub, userId)
        _uiState.value = _uiState.value.copy(userRole = role)
    }
    
    fun hasPermission(permission: BookClubPermission): Boolean {
        val role = _uiState.value.userRole ?: return false
        return BookClubPermissions.hasPermission(role, permission)
    }
    
    fun getMemberRole(memberId: String): BookClubRole? {
        val bookClub = _uiState.value.bookClub ?: return null
        return BookClubPermissions.getUserRole(bookClub, memberId)
    }
    
    private var bookClubListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun listenForBookClubUpdates(bookClubId: String) {
        // Clean up previous listener before starting a new one
        bookClubListener?.remove()

        bookClubListener = firestore.collection("bookClubs").document(bookClubId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(error = "북클럽 정보를 불러오는데 실패했습니다: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    viewModelScope.launch {
                        val bookClub = snapshot.toObject(BookClub::class.java)?.copy(id = snapshot.id)

                        // Validate members and clean up if necessary
                        bookClub?.let { validateMembers(it) }

                        _uiState.value = _uiState.value.copy(bookClub = bookClub)
                        updateUserRole()
                        bookClub?.let { loadMemberProfiles(it) }
                    }
                } else {
                     _uiState.value = _uiState.value.copy(error = "북클럽 정보를 찾을 수 없습니다.")
                }
            }
    }
    
    private fun loadMemberProfiles(bookClub: BookClub) {
        viewModelScope.launch {
            val allMemberIds = (listOf(bookClub.createdBy) + bookClub.viceOwners + bookClub.members).distinct()
            val profiles = mutableMapOf<String, User>()
            
            allMemberIds.forEach { memberId ->
                try {
                    val user = firestoreRepository.getUserProfile(memberId)
                    if (user != null) profiles[memberId] = user
                } catch (e: Exception) { }
            }
            _uiState.value = _uiState.value.copy(memberProfiles = profiles)
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
        if (!hasPermission(BookClubPermission.CHANGE_BOOK)) {
            _uiState.value = _uiState.value.copy(error = "책 변경 권한이 없습니다.")
            return
        }
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
                // The listener will automatically update the UI after this write.
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "책 정보 업데이트에 실패했습니다.")
            }
        }
    }
    
    fun saveBookHistoryForDates(bookClubId: String, dates: List<String>, bookCover: String) {
        if (!hasPermission(BookClubPermission.EDIT_CLUB_INFO)) {
            _uiState.value = _uiState.value.copy(error = "일정 수정 권한이 없습니다.")
            return
        }
        viewModelScope.launch {
            try {
                val currentHistory = _uiState.value.bookClub?.bookHistory?.toMutableMap() ?: mutableMapOf()
                dates.forEach { date -> currentHistory[date] = bookCover }
                firestore.collection("bookClubs").document(bookClubId).update("bookHistory", currentHistory).await()
                // The listener will automatically update the UI after this write.
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "책 기록 저장에 실패했습니다.")
            }
        }
    }
    
    fun deleteBookHistoryForDates(bookClubId: String, dates: List<String>) {
        if (!hasPermission(BookClubPermission.EDIT_CLUB_INFO)) {
            _uiState.value = _uiState.value.copy(error = "일정 삭제 권한이 없습니다.")
            return
        }
        viewModelScope.launch {
            try {
                val currentHistory = _uiState.value.bookClub?.bookHistory?.toMutableMap() ?: mutableMapOf()
                dates.forEach { date -> currentHistory.remove(date) }
                firestore.collection("bookClubs").document(bookClubId).update("bookHistory", currentHistory).await()
                // The listener will automatically update the UI after this write.
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "책 기록 삭제에 실패했습니다.")
            }
        }
    }
    
    fun deleteBookClub(bookClubId: String, onSuccess: () -> Unit) {
        if (!hasPermission(BookClubPermission.DELETE_CLUB)) {
            _uiState.value = _uiState.value.copy(error = "북클럽 삭제 권한이 없습니다.")
            return
        }
        viewModelScope.launch {
            try {
                firestore.collection("bookClubs").document(bookClubId).delete().await()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "북클럽 삭제에 실패했습니다.")
            }
        }
    }
    
    fun leaveBookClub(bookClubId: String, onSuccess: () -> Unit) {
        if (!hasPermission(BookClubPermission.LEAVE_CLUB)) {
            _uiState.value = _uiState.value.copy(error = "방장은 북클럽을 나갈 수 없습니다.")
            return
        }
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                val bookClub = _uiState.value.bookClub ?: return@launch
                
                // 나가기 시스템 메시지 전송
                sendSystemMessage(bookClubId, "${currentUserInfo?.nickname ?: "알 수 없음"}님이 북클럽을 나갔습니다.")
                
                val updatedMembers = bookClub.members - userId
                val updatedViceOwners = bookClub.viceOwners - userId
                firestore.collection("bookClubs").document(bookClubId).update(
                    mapOf(
                        "members" to updatedMembers,
                        "viceOwners" to updatedViceOwners,
                        "memberCount" to updatedMembers.size
                    )
                ).await()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "북클럽 나가기에 실패했습니다.")
            }
        }
    }
    
    private suspend fun sendSystemMessage(bookClubId: String, message: String) {
        try {
            val systemMessage = ChatMessage(
                bookClubId = bookClubId,
                senderId = "system",
                senderName = "시스템",
                senderProfileImage = "",
                message = message,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("bookClubs")
                .document(bookClubId)
                .collection("messages")
                .add(systemMessage)
                .await()
        } catch (e: Exception) { }
    }
    
    fun kickMember(bookClubId: String, memberId: String) {
        if (!hasPermission(BookClubPermission.KICK_MEMBER)) {
            _uiState.value = _uiState.value.copy(error = "멤버 강퇴 권한이 없습니다.")
            return
        }
        val bookClub = _uiState.value.bookClub ?: return
        // 방장은 강퇴 불가
        if (bookClub.isOwner(memberId)) {
            _uiState.value = _uiState.value.copy(error = "방장은 강퇴할 수 없습니다.")
            return
        }
        // 부방장이 다른 부방장 강퇴 불가
        if (_uiState.value.userRole == BookClubRole.VICE_OWNER && bookClub.isViceOwner(memberId)) {
            _uiState.value = _uiState.value.copy(error = "부방장은 다른 부방장을 강퇴할 수 없습니다.")
            return
        }
        viewModelScope.launch {
            try {
                val updatedMembers = bookClub.members - memberId
                val updatedViceOwners = bookClub.viceOwners - memberId
                firestore.collection("bookClubs").document(bookClubId).update(
                    mapOf(
                        "members" to updatedMembers,
                        "viceOwners" to updatedViceOwners,
                        "memberCount" to updatedMembers.size
                    )
                ).await()
                // The listener will automatically update the UI after this write.
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "멤버 강퇴에 실패했습니다.")
            }
        }
    }
    
    fun setViceOwner(bookClubId: String, memberId: String, isViceOwner: Boolean) {
        if (!hasPermission(BookClubPermission.MANAGE_VICE_OWNER)) {
            _uiState.value = _uiState.value.copy(error = "부방장 관리 권한이 없습니다.")
            return
        }
        viewModelScope.launch {
            try {
                val bookClub = _uiState.value.bookClub ?: return@launch
                val updatedViceOwners = if (isViceOwner) {
                    bookClub.viceOwners + memberId
                } else {
                    bookClub.viceOwners - memberId
                }
                firestore.collection("bookClubs").document(bookClubId).update("viceOwners", updatedViceOwners).await()
                // The listener will automatically update the UI after this write.
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "부방장 설정에 실패했습니다.")
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
                    _uiState.value = _uiState.value.copy(messages = messages, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "메시지를 불러오는데 실패했습니다.")
            }
        }
    }
    
    fun sendMessage(bookClubId: String, messageText: String) {
        if (messageText.isBlank()) return
        if (bookClubId.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "잘못된 북클럽 ID입니다.")
            return
        }
        val currentUser = auth.currentUser ?: run {
            _uiState.value = _uiState.value.copy(error = "로그인이 필요합니다.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            try {
                if (currentUserInfo == null) loadCurrentUserInfo()
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
                    onSuccess = { _uiState.value = _uiState.value.copy(isSending = false, error = null) },
                    onFailure = { error -> _uiState.value = _uiState.value.copy(isSending = false, error = error.message ?: "메시지 전송에 실패했습니다.") }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSending = false, error = e.message ?: "메시지 전송 중 오류가 발생했습니다.")
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun validateMembers(bookClub: BookClub) {
        viewModelScope.launch {
            val membersToRemove = mutableListOf<String>()

            // Check each member's existence
            for (memberId in bookClub.members) {
                try {
                    val memberDoc = firestore.collection("users").document(memberId).get().await()
                    if (!memberDoc.exists()) {
                        membersToRemove.add(memberId)
                    }
                } catch (e: Exception) {
                    // Handle exceptions, e.g., network errors
                    android.util.Log.e("BookClubChatVM", "Error checking member existence for $memberId", e)
                }
            }

            // If ghost members are found, update the book club document
            if (membersToRemove.isNotEmpty()) {
                android.util.Log.d("BookClubChatVM", "Found ghost members to remove: $membersToRemove")
                try {
                    firestore.collection("bookClubs").document(bookClub.id)
                        .update("members", com.google.firebase.firestore.FieldValue.arrayRemove(*membersToRemove.toTypedArray()))
                        .await()
                    android.util.Log.d("BookClubChatVM", "Successfully removed ghost members from Firestore.")
                    // The listener will automatically pick up this change and refresh the UI.
                } catch (e: Exception) {
                    android.util.Log.e("BookClubChatVM", "Error removing ghost members from Firestore", e)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bookClubListener?.remove()
    }
    
    fun retryLoadMessages(bookClubId: String) {
        clearError()
        loadMessages(bookClubId)
    }
}

data class BookClubChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val bookClub: BookClub? = null,
    val userRole: BookClubRole? = null,
    val memberProfiles: Map<String, User> = emptyMap(),
    val searchResults: List<Book> = emptyList(),
    val isSearchingBooks: Boolean = false,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)
