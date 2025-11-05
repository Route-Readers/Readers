package com.route.readers.ui.community.used_trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.route.readers.data.model.User
import com.route.readers.data.remote.UserRepository

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val userRepository = UserRepository()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatters = MutableStateFlow<Map<String, User>>(emptyMap())
    val chatters: StateFlow<Map<String, User>> = _chatters.asStateFlow()
    
    fun loadMessages(userId: String, sellerId: String, bookId: String) {
        viewModelScope.launch {
            val user = userRepository.getUser(userId)
            val seller = userRepository.getUser(sellerId)
            val chattersMap = mutableMapOf<String, User>()
            user?.let { chattersMap[userId] = it }
            seller?.let { chattersMap[sellerId] = it }
            _chatters.value = chattersMap
        }

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
                
                db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(newMessage.id)
                    .set(newMessage)
                    .await()
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
