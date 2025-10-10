package com.route.readers.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.route.readers.data.model.FriendRequest
import com.route.readers.data.model.FriendRequestStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FriendRequestRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _pendingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequest>> = _pendingRequests.asStateFlow()
    
    private val currentUserId: String?
        get() = auth.currentUser?.uid
    
    suspend fun sendFriendRequest(receiverNickname: String): AddFriendResult {
        return currentUserId?.let { senderId ->
            try {
                val receiverQuery = firestore.collection("users")
                    .whereEqualTo("nickname", receiverNickname)
                    .get()
                    .await()
                
                if (receiverQuery.isEmpty) {
                    return@let AddFriendResult.UserNotFound
                }
                
                val receiverDoc = receiverQuery.documents.first()
                val receiverId = receiverDoc.id
                
                if (senderId == receiverId) {
                    return@let AddFriendResult.Error("자기 자신에게는 친구 요청을 보낼 수 없습니다")
                }
                
                // 이미 친구인지 확인
                val senderDoc = firestore.collection("users").document(senderId).get().await()
                val friends = senderDoc.get("friends") as? List<String> ?: emptyList()
                if (friends.contains(receiverId)) {
                    return@let AddFriendResult.AlreadyFriend
                }
                
                // 이미 요청이 있는지 확인
                val existingRequest = firestore.collection("friendRequests")
                    .whereEqualTo("senderId", senderId)
                    .whereEqualTo("receiverId", receiverId)
                    .whereEqualTo("status", FriendRequestStatus.PENDING.name)
                    .get()
                    .await()
                
                if (!existingRequest.isEmpty) {
                    return@let AddFriendResult.Error("이미 친구 요청을 보냈습니다")
                }
                
                val senderNickname = senderDoc.getString("nickname") ?: ""
                val senderProfileUrl = senderDoc.getString("profileImageUrl")
                
                val friendRequest = FriendRequest(
                    senderId = senderId,
                    receiverId = receiverId,
                    senderNickname = senderNickname,
                    senderProfileImageUrl = senderProfileUrl,
                    status = FriendRequestStatus.PENDING
                )
                
                val requestDoc = firestore.collection("friendRequests")
                    .add(friendRequest)
                    .await()
                
                // 알림 생성
                val notificationRepository = NotificationRepository()
                notificationRepository.createNotification(
                    userId = receiverId,
                    type = com.route.readers.data.model.NotificationType.FRIEND_REQUEST,
                    title = "새로운 친구 요청",
                    message = "${senderNickname}님이 친구 요청을 보냈습니다",
                    data = mapOf("requestId" to requestDoc.id, "senderId" to senderId)
                )
                
                AddFriendResult.Success
            } catch (e: Exception) {
                AddFriendResult.Error(e.message ?: "알 수 없는 오류")
            }
        } ?: AddFriendResult.Error("로그인이 필요합니다")
    }
    
    suspend fun loadPendingRequests() {
        currentUserId?.let { userId ->
            try {
                // 실시간 리스너 추가
                firestore.collection("friendRequests")
                    .whereEqualTo("receiverId", userId)
                    .whereEqualTo("status", FriendRequestStatus.PENDING.name)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _pendingRequests.value = emptyList()
                            return@addSnapshotListener
                        }
                        
                        val requestList = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                val request = doc.toObject(FriendRequest::class.java)?.copy(
                                    id = doc.id,
                                    timestamp = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                                )
                                request
                            } catch (e: Exception) {
                                null
                            }
                        } ?: emptyList()
                        
                        _pendingRequests.value = requestList
                    }
            } catch (e: Exception) {
                _pendingRequests.value = emptyList()
            }
        }
    }
    
    suspend fun acceptFriendRequest(requestId: String): Boolean {
        return try {
            val requestDoc = firestore.collection("friendRequests")
                .document(requestId)
                .get()
                .await()
            
            val request = requestDoc.toObject(FriendRequest::class.java) ?: return false
            
            // 양방향 친구 추가
            addFriendToUser(request.senderId, request.receiverId)
            addFriendToUser(request.receiverId, request.senderId)
            
            // 요청 상태 업데이트
            firestore.collection("friendRequests")
                .document(requestId)
                .update("status", FriendRequestStatus.ACCEPTED.name)
                .await()
            
            // 친구 수락 알림 생성
            val notificationRepository = NotificationRepository()
            val receiverDoc = firestore.collection("users").document(request.receiverId).get().await()
            val receiverNickname = receiverDoc.getString("nickname") ?: ""
            
            notificationRepository.createNotification(
                userId = request.senderId,
                type = com.route.readers.data.model.NotificationType.FRIEND_ACCEPTED,
                title = "친구 요청 수락",
                message = "${receiverNickname}님이 친구 요청을 수락했습니다",
                data = mapOf("friendId" to request.receiverId)
            )
            
            loadPendingRequests()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun rejectFriendRequest(requestId: String): Boolean {
        return try {
            firestore.collection("friendRequests")
                .document(requestId)
                .update("status", FriendRequestStatus.REJECTED.name)
                .await()
            
            loadPendingRequests()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun addFriendToUser(userId: String, friendId: String) {
        val userDoc = firestore.collection("users").document(userId).get().await()
        val currentFriends = userDoc.get("friends") as? MutableList<String> ?: mutableListOf()
        
        if (!currentFriends.contains(friendId)) {
            currentFriends.add(friendId)
            firestore.collection("users")
                .document(userId)
                .update("friends", currentFriends)
                .await()
        }
    }
}
