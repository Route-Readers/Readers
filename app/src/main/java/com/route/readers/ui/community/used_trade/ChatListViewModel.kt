package com.route.readers.ui.community.used_trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.route.readers.data.model.User
import com.route.readers.data.remote.UserRepository
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
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid ?: return@launch

            db.collection("chats")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }

                    snapshots?.let { querySnapshot ->
                        val chatItems = mutableListOf<ChatListItem>()
                        for (document in querySnapshot.documents) {
                            val chatId = document.id
                            val parts = chatId.split("_")
                            if (parts.size >= 3) {
                                val userId1 = parts[0]
                                val userId2 = parts[1]
                                val bookId = parts[2]

                                if (userId1 == currentUserId || userId2 == currentUserId) {
                                    val otherUserId = if (userId1 == currentUserId) userId2 else userId1

                                    // Fetch last message
                                    db.collection("chats").document(chatId).collection("messages")
                                        .orderBy("timestamp", Query.Direction.DESCENDING)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener { messageSnapshot ->
                                            val lastMessageDoc = messageSnapshot.documents.firstOrNull()
                                            val lastMessage = lastMessageDoc?.toObject(ChatMessage::class.java)

                                            viewModelScope.launch {
                                                val otherUser = userRepository.getUser(otherUserId)
                                                val book = bookRepository.getBookDetail(bookId)
                                                val bookTitle = book?.title

                                                if (lastMessage != null && otherUser != null) {
                                                    chatItems.add(
                                                        ChatListItem(
                                                            chatId = chatId,
                                                            otherUserId = otherUserId,
                                                            otherUserName = otherUser.nickname ?: "알 수 없음",
                                                            otherUserProfileImageUrl = otherUser.profileImageUrl,
                                                            lastMessage = lastMessage.message,
                                                            timestamp = lastMessage.timestamp?.time ?: 0L,
                                                            bookId = bookId,
                                                            bookTitle = bookTitle
                                                        )
                                                    )
                                                    _chatList.value = chatItems.sortedByDescending { it.timestamp }
                                                }
                                            }
                                        }
                                }
                            }
                        }
                    }
                }
        }
    }
}
