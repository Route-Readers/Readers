package com.route.readers.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    
    private val currentUserId: String?
        get() = auth.currentUser?.uid
    
    suspend fun loadFriends() {
        currentUserId?.let { userId ->
            try {
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()
                
                val friendIds = userDoc.get("friends") as? List<String> ?: emptyList()
                
                if (friendIds.isNotEmpty()) {
                    val friendsList = mutableListOf<Friend>()
                    
                    for (friendId in friendIds) {
                        try {
                            val friendDoc = firestore.collection("users")
                                .document(friendId)
                                .get()
                                .await()
                            
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
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    _friends.value = friendsList
                } else {
                    _friends.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _friends.value = emptyList()
            }
        }
    }
    
    suspend fun addFriend(friendNickname: String): AddFriendResult {
        return currentUserId?.let { userId ->
            try {
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("nickname", friendNickname)
                    .get()
                    .await()
                
                if (querySnapshot.isEmpty) {
                    return@let AddFriendResult.UserNotFound
                }
                
                val friendDoc = querySnapshot.documents.first()
                val friendId = friendDoc.id
                
                val currentUserDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()
                
                val currentFriends = currentUserDoc.get("friends") as? MutableList<String> ?: mutableListOf()
                
                if (currentFriends.contains(friendId)) {
                    return@let AddFriendResult.AlreadyFriend
                }
                
                currentFriends.add(friendId)
                
                firestore.collection("users")
                    .document(userId)
                    .update("friends", currentFriends)
                    .await()
                
                loadFriends()
                AddFriendResult.Success
            } catch (e: Exception) {
                e.printStackTrace()
                AddFriendResult.Error(e.message ?: "알 수 없는 오류")
            }
        } ?: AddFriendResult.Error("로그인이 필요합니다")
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
}
