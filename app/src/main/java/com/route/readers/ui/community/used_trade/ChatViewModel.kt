package com.route.readers.ui.community.used_trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.route.readers.data.model.User
import com.route.readers.data.model.UsedBook
import com.route.readers.data.remote.UserRepository

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val userRepository = UserRepository()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatters = MutableStateFlow<Map<String, User>>(emptyMap())
    val chatters: StateFlow<Map<String, User>> = _chatters.asStateFlow()

    private val _bookInfo = MutableStateFlow<UsedBook?>(null)
    val bookInfo: StateFlow<UsedBook?> = _bookInfo.asStateFlow()
    
    fun loadMessages(userId: String, sellerId: String, bookId: String) {
        viewModelScope.launch {
            val user = userRepository.getUser(userId)
            val seller = userRepository.getUser(sellerId)
            val chattersMap = mutableMapOf<String, User>()
            user?.let { chattersMap[userId] = it }
            seller?.let { chattersMap[sellerId] = it }
            _chatters.value = chattersMap
        }

        loadBookInfo(bookId)

        val chatId = getChatId(userId, sellerId, bookId)
        
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                val messageList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                _messages.value = messageList
            }
    }

    private fun loadBookInfo(bookId: String) {
        viewModelScope.launch {
            try {
                val document = db.collection("usedBooks").document(bookId).get().await()
                if (document.exists()) {
                    val book = document.toObject(UsedBook::class.java)?.copy(id = document.id)
                    _bookInfo.value = book
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun sendMessage(senderId: String, receiverId: String, bookId: String, message: String) {
        viewModelScope.launch {
            try {
                val chatId = getChatId(senderId, receiverId, bookId)
                val newMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    senderId = senderId,
                    receiverId = receiverId,
                    message = message,
                    timestamp = java.util.Date(),
                    bookId = bookId
                )

                // 1. Add message to subcollection
                db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(newMessage.id)
                    .set(newMessage)
                    .await()

                // 2. Update parent document (Chat Room Info)
                val chatRoomInfo = mapOf(
                    "participants" to listOf(senderId, receiverId),
                    "bookId" to bookId,
                    "lastMessage" to message,
                    "lastMessageTime" to newMessage.timestamp
                )
                
                db.collection("chats")
                    .document(chatId)
                    .set(chatRoomInfo, SetOptions.merge())
                    .await()

                // 3. Create FCM request for chat notification
                val senderUser = userRepository.getUser(senderId)
                val senderNickname = senderUser?.nickname ?: "알 수 없는 사용자"

                val fcmRequestData = mapOf(
                    "targetUserId" to receiverId,
                    "title" to senderNickname,
                    "message" to message,
                    "notificationType" to "CHAT_MESSAGE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    // Include additional data for navigation if needed in the client
                    "chatId" to chatId,
                    "senderId" to senderId,
                    "bookId" to bookId
                )

                db.collection("fcmRequests")
                    .add(fcmRequestData)
                    .await()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun leaveChat(userId: String, otherUserId: String, bookId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val chatId = getChatId(userId, otherUserId, bookId)
                
                // 1. Delete all messages in subcollection
                val messagesSnapshot = db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .get()
                    .await()
                
                val batch = db.batch()
                for (doc in messagesSnapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                
                // 2. Delete the chat document itself
                db.collection("chats")
                    .document(chatId)
                    .delete()
                    .await()
                
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun blockUser(currentUserId: String, userToBlockId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val blockData = mapOf(
                    "blockedId" to userToBlockId,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                
                db.collection("users")
                    .document(currentUserId)
                    .collection("blockedUsers")
                    .document(userToBlockId)
                    .set(blockData)
                    .await()

                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reportUser(reporterId: String, reportedUserId: String, reason: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val reportData = mapOf(
                    "reporterId" to reporterId,
                    "reportedUserId" to reportedUserId,
                    "reason" to reason,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "status" to "PENDING"
                )
                
                db.collection("reports")
                    .add(reportData)
                    .await()

                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun getChatId(userId1: String, userId2: String, bookId: String): String {
        val sortedIds = listOf(userId1, userId2).sorted()
        return "${sortedIds[0]}_${sortedIds[1]}_$bookId"
    }
}
