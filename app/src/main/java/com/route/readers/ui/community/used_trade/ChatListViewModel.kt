package com.route.readers.ui.community.used_trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.route.readers.data.model.User
import com.route.readers.data.remote.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.route.readers.data.remote.BookRepository

data class ChatListItem(
    val chatId: String,
    val otherUserId: String,
    val otherUserName: String,
    val otherUserProfileImageUrl: String?,
    val lastMessage: String,
    val timestamp: Long,
    val bookId: String,
    val bookTitle: String? // Will be fetched later
)


class ChatListViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    private val bookRepository = BookRepository()

    private val _chatList = MutableStateFlow<List<ChatListItem>>(emptyList())
    val chatList: StateFlow<List<ChatListItem>> = _chatList.asStateFlow()

    init {
        loadChatList()
    }

    private fun loadChatList() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                viewModelScope.launch {
                    val deferredItems = snapshots?.documents?.map { doc ->
                        async {
                            val data = doc.data ?: return@async null
                            val participants = data["participants"] as? List<String> ?: return@async null
                            val bookId = data["bookId"] as? String ?: return@async null
                            val lastMessage = data["lastMessage"] as? String ?: ""
                            
                            // Handle Timestamp or Date (it might be saved as either depending on how it was set)
                            val timestampObj = data["lastMessageTime"]
                            val timestamp = when (timestampObj) {
                                is com.google.firebase.Timestamp -> timestampObj.toDate().time
                                is java.util.Date -> timestampObj.time
                                else -> 0L
                            }

                            val otherUserId = participants.firstOrNull { it != currentUserId } ?: return@async null

                            val otherUser = userRepository.getUser(otherUserId)
                            val book = bookRepository.getBookDetail(bookId)

                            if (otherUser != null) {
                                ChatListItem(
                                    chatId = doc.id,
                                    otherUserId = otherUserId,
                                    otherUserName = otherUser.nickname ?: "알 수 없음",
                                    otherUserProfileImageUrl = otherUser.profileImageUrl,
                                    lastMessage = lastMessage,
                                    timestamp = timestamp,
                                    bookId = bookId,
                                    bookTitle = book?.title
                                )
                            } else {
                                null
                            }
                        }
                    }

                    val items = deferredItems?.awaitAll()?.filterNotNull() ?: emptyList()
                    _chatList.value = items.sortedByDescending { it.timestamp }
                }
            }
    }
}
