package com.route.readers.data.remote

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.notification.FollowNotificationHelper
import com.route.readers.data.model.User // Corrected import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

// Friend management is now handled by followUser and unfollowUser.
// Mutual followers automatically become friends.

class FriendsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _friends = MutableStateFlow<List<User>>(emptyList()) // Changed to List<User>
    val friends: StateFlow<List<User>> = _friends.asStateFlow() // Changed to List<User>
    
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
        
        val friendsList = mutableListOf<User>() // Changed to List<User>
        var loadedCount = 0
        
        friendIds.forEach { friendId ->
            firestore.collection("users")
                .document(friendId)
                .get()
                .addOnSuccessListener { friendDoc ->
                    if (friendDoc.exists()) {
                        val user = friendDoc.toObject(User::class.java) // Deserialize to User object
                        user?.let { friendsList.add(it) }
                    }
                    
                    loadedCount++
                    if (loadedCount == friendIds.size) {
                        _friends.value = friendsList.sortedBy { it.nickname } // Sorted by nickname
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    loadedCount++
                    if (loadedCount == friendIds.size) {
                        _friends.value = friendsList.sortedBy { it.nickname }
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
    
    suspend fun followUser(userId: String, context: Context) {
        currentUserId?.let { currentId ->
            if (currentId == userId) return@let

            try {
                val currentUserDocRef = firestore.collection("users").document(currentId)
                val targetUserDocRef = firestore.collection("users").document(userId)

                // Firestore transaction to ensure atomicity
                firestore.runTransaction { transaction ->
                    val currentUserSnapshot = transaction.get(currentUserDocRef)
                    val targetUserSnapshot = transaction.get(targetUserDocRef)

                    val currentFollowing = currentUserSnapshot.get("following") as? MutableList<String>
                        ?: mutableListOf()
                    if (!currentFollowing.contains(userId)) {
                        currentFollowing.add(userId)
                        transaction.update(currentUserDocRef, "following", currentFollowing)
                        transaction.update(
                            currentUserDocRef,
                            "followingCount",
                            currentFollowing.size.toLong()
                        )
                    }

                    val targetFollowers =
                        targetUserSnapshot.get("followers") as? MutableList<String> ?: mutableListOf()
                    if (!targetFollowers.contains(currentId)) {
                        targetFollowers.add(currentId)
                        transaction.update(targetUserDocRef, "followers", targetFollowers)
                        transaction.update(
                            targetUserDocRef,
                            "followerCount",
                            targetFollowers.size.toLong()
                        )
                    }

                    // Check for mutual follow
                    val targetFollowing =
                        targetUserSnapshot.get("following") as? List<String> ?: emptyList()
                    if (targetFollowing.contains(currentId)) {
                        // They are now mutual followers, so they become friends
                        val currentUserFriends =
                            currentUserSnapshot.get("friends") as? MutableList<String>
                                ?: mutableListOf()
                        if (!currentUserFriends.contains(userId)) {
                            currentUserFriends.add(userId)
                            transaction.update(currentUserDocRef, "friends", currentUserFriends)
                        }

                        val targetUserFriends =
                            targetUserSnapshot.get("friends") as? MutableList<String>
                                ?: mutableListOf()
                        if (!targetUserFriends.contains(currentId)) {
                            targetUserFriends.add(currentId)
                            transaction.update(targetUserDocRef, "friends", targetUserFriends)
                        }
                    }
                }.await()

                // Send notification after transaction is successful
                val currentUserNickname =
                    firestore.collection("users").document(currentId).get().await()
                        .getString("nickname") ?: "알 수 없는 사용자"
                FollowNotificationHelper.sendFollowNotification(context, currentUserNickname)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun unfollowUser(userId: String) {
        currentUserId?.let { currentId ->
            if (currentId == userId) return@let

            try {
                val currentUserDocRef = firestore.collection("users").document(currentId)
                val targetUserDocRef = firestore.collection("users").document(userId)

                firestore.runTransaction { transaction ->
                    // Remove from current user's following list
                    val currentUserSnapshot = transaction.get(currentUserDocRef)
                    val currentFollowing =
                        currentUserSnapshot.get("following") as? MutableList<String>
                            ?: mutableListOf()
                    if (currentFollowing.remove(userId)) {
                        transaction.update(currentUserDocRef, "following", currentFollowing)
                        transaction.update(
                            currentUserDocRef,
                            "followingCount",
                            currentFollowing.size.toLong()
                        )
                    }

                    // Remove from target user's followers list
                    val targetUserSnapshot = transaction.get(targetUserDocRef)
                    val targetFollowers =
                        targetUserSnapshot.get("followers") as? MutableList<String>
                            ?: mutableListOf()
                    if (targetFollowers.remove(currentId)) {
                        transaction.update(targetUserDocRef, "followers", targetFollowers)
                        transaction.update(
                            targetUserDocRef,
                            "followerCount",
                            targetFollowers.size.toLong()
                        )
                    }

                    // Remove friendship
                    val currentUserFriends =
                        currentUserSnapshot.get("friends") as? MutableList<String>
                            ?: mutableListOf()
                    if (currentUserFriends.remove(userId)) {
                        transaction.update(currentUserDocRef, "friends", currentUserFriends)
                    }

                    val targetUserFriends =
                        targetUserSnapshot.get("friends") as? MutableList<String>
                            ?: mutableListOf()
                    if (targetUserFriends.remove(currentId)) {
                        transaction.update(targetUserDocRef, "friends", targetUserFriends)
                    }
                }.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}