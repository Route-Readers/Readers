package com.route.readers.data.remote

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.notification.FollowNotificationHelper
import com.route.readers.ui.screens.community.Friend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

sealed class AddFriendResult {
    object Success : AddFriendResult()
    object UserNotFound : AddFriendResult()
    object AlreadyFriend : AddFriendResult()
    data class Error(val message: String) : AddFriendResult()
}

class FriendsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()
    
    private var friendsListener: com.google.firebase.firestore.ListenerRegistration? = null
    
    private val currentUserId: String?
        get() = auth.currentUser?.uid
    
    init {
        startListeningToFriends()
    }
    
    private fun startListeningToFriends() {
        currentUserId?.let { userId ->
            friendsListener = firestore.collection("users")
                .document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        error.printStackTrace()
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        val friendIds = snapshot.get("friends") as? List<String> ?: emptyList()
                        loadFriendsData(friendIds)
                    }
                }
        }
    }
    
    private fun loadFriendsData(friendIds: List<String>) {
        if (friendIds.isEmpty()) {
            _friends.value = emptyList()
            return
        }
        
        val friendsList = mutableListOf<Friend>()
        var loadedCount = 0
        
        friendIds.forEach { friendId ->
            firestore.collection("users")
                .document(friendId)
                .get()
                .addOnSuccessListener { friendDoc ->
                    if (friendDoc.exists()) {
                        val friend = Friend(
                            id = friendId,
                            name = friendDoc.getString("nickname") ?: "알 수 없음",
                            currentBook = "최근 본 책: 독서 중",
                            isOnline = false,
                            lastActive = "최근 활동"
                        )
                        friendsList.add(friend)
                    }
                    
                    loadedCount++
                    if (loadedCount == friendIds.size) {
                        _friends.value = friendsList.sortedBy { it.name }
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    loadedCount++
                    if (loadedCount == friendIds.size) {
                        _friends.value = friendsList.sortedBy { it.name }
                    }
                }
        }
    }
    
    suspend fun loadFriends() {
        // 이제 실시간 리스너가 있으므로 이 메서드는 초기 로드용으로만 사용
        currentUserId?.let { userId ->
            try {
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()
                
                val friendIds = userDoc.get("friends") as? List<String> ?: emptyList()
                loadFriendsData(friendIds)
            } catch (e: Exception) {
                e.printStackTrace()
                _friends.value = emptyList()
            }
        }
    }
    
    fun stopListening() {
        friendsListener?.remove()
        friendsListener = null
    }
    
    suspend fun addFriend(friendNickname: String): AddFriendResult {
        // 이제 FriendRequestRepository를 사용하여 친구 요청을 보냄
        val friendRequestRepository = FriendRequestRepository()
        return friendRequestRepository.sendFriendRequest(friendNickname)
    }
    
    suspend fun removeFriend(friendId: String) {
        currentUserId?.let { userId ->
            try {
                val currentUserDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()
                
                val currentFriends = currentUserDoc.get("friends") as? MutableList<String> ?: mutableListOf()
                currentFriends.remove(friendId)
                
                firestore.collection("users")
                    .document(userId)
                    .update("friends", currentFriends)
                    .await()
                
                loadFriends()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    suspend fun followUser(userId: String, context: Context) {
        currentUserId?.let { currentId ->
            try {
                val currentUserDoc = firestore.collection("users").document(currentId)
                val currentUserData = currentUserDoc.get().await()
                val currentFollowing = currentUserData.get("following") as? MutableList<String> ?: mutableListOf()
                
                if (!currentFollowing.contains(userId)) {
                    currentFollowing.add(userId)
                    currentUserDoc.update("following", currentFollowing).await()
                    
                    val targetUserDoc = firestore.collection("users").document(userId)
                    val targetFollowers = targetUserDoc.get().await().get("followers") as? MutableList<String> ?: mutableListOf()
                    
                    if (!targetFollowers.contains(currentId)) {
                        targetFollowers.add(currentId)
                        targetUserDoc.update("followers", targetFollowers).await()
                        
                        val currentUserNickname = currentUserData.getString("nickname") ?: "알 수 없는 사용자"
                        FollowNotificationHelper.sendFollowNotification(context, currentUserNickname)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
