package com.route.readers.data.remote

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
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

            val currentUserDocRef = firestore.collection("users").document(currentId)
            val targetUserDocRef = firestore.collection("users").document(userId)

            // Firestore transaction to ensure atomicity
            firestore.runTransaction { transaction ->
                // === READ PHASE ===
                val currentUserSnapshot = transaction.get(currentUserDocRef)
                val targetUserSnapshot = transaction.get(targetUserDocRef)

                val initialCurrentFollowing = (currentUserSnapshot.get("following") as? List<String>)?.toMutableList() ?: mutableListOf()
                val initialCurrentUserFriends = (currentUserSnapshot.get("friends") as? List<String>)?.toMutableList() ?: mutableListOf()

                val initialTargetFollowers = (targetUserSnapshot.get("followers") as? List<String>)?.toMutableList() ?: mutableListOf()
                val initialTargetFollowing = (targetUserSnapshot.get("following") as? List<String>) ?: emptyList() // Not mutable, just for check
                val initialTargetUserFriends = (targetUserSnapshot.get("friends") as? List<String>)?.toMutableList() ?: mutableListOf()

                // === PROCESSING / MODIFICATION PHASE ===
                var shouldUpdateCurrentUserFollowing = false
                if (!initialCurrentFollowing.contains(userId)) {
                    initialCurrentFollowing.add(userId)
                    shouldUpdateCurrentUserFollowing = true
                }

                var shouldUpdateTargetFollowers = false
                if (!initialTargetFollowers.contains(currentId)) {
                    initialTargetFollowers.add(currentId)
                    shouldUpdateTargetFollowers = true
                }

                var shouldUpdateCurrentUserFriends = false
                var shouldUpdateTargetUserFriends = false

                // Check for mutual follow
                if (initialTargetFollowing.contains(currentId)) {
                    // They are now mutual followers, so they become friends
                    if (!initialCurrentUserFriends.contains(userId)) {
                        initialCurrentUserFriends.add(userId)
                        shouldUpdateCurrentUserFriends = true
                    }
                    if (!initialTargetUserFriends.contains(currentId)) {
                        initialTargetUserFriends.add(currentId)
                        shouldUpdateTargetUserFriends = true
                    }
                }

                // === WRITE PHASE ===
                if (shouldUpdateCurrentUserFollowing) {
                    transaction.update(currentUserDocRef, "following", initialCurrentFollowing)
                    transaction.update(
                        currentUserDocRef,
                        "followingCount",
                        initialCurrentFollowing.size.toLong()
                    )
                }

                if (shouldUpdateTargetFollowers) {
                    transaction.update(targetUserDocRef, "followers", initialTargetFollowers)
                    transaction.update(
                        targetUserDocRef,
                        "followerCount",
                        initialTargetFollowers.size.toLong()
                    )
                }

                if (shouldUpdateCurrentUserFriends) {
                    transaction.update(currentUserDocRef, "friends", initialCurrentUserFriends)
                }

                if (shouldUpdateTargetUserFriends) {
                    transaction.update(targetUserDocRef, "friends", initialTargetUserFriends)
                }
            }.await()

            // Send FCM notification after transaction is successful
            val currentUserNickname =
                firestore.collection("users").document(currentId).get().await()
                    .getString("nickname") ?: "알 수 없는 사용자"

            // Create a request in fcmRequests collection for the target user
            firestore.collection("fcmRequests").add(
                mapOf<String, Any>(
                    "targetUserId" to userId,
                    "title" to "새로운 팔로우 요청",
                    "message" to "${currentUserNickname}님이 회원님을 팔로우합니다.",
                    "notificationType" to "FOLLOW",
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun unfollowUser(userId: String) {
        currentUserId?.let { currentId ->
            if (currentId == userId) return@let

            val currentUserDocRef = firestore.collection("users").document(currentId)
            val targetUserDocRef = firestore.collection("users").document(userId)

            firestore.runTransaction { transaction ->
                // === READ PHASE ===
                val currentUserSnapshot = transaction.get(currentUserDocRef)
                val targetUserSnapshot = transaction.get(targetUserDocRef)

                val initialCurrentFollowing = (currentUserSnapshot.get("following") as? List<String>)?.toMutableList() ?: mutableListOf()
                val initialCurrentUserFriends = (currentUserSnapshot.get("friends") as? List<String>)?.toMutableList() ?: mutableListOf()

                val initialTargetFollowers = (targetUserSnapshot.get("followers") as? List<String>)?.toMutableList() ?: mutableListOf()
                val initialTargetUserFriends = (targetUserSnapshot.get("friends") as? List<String>)?.toMutableList() ?: mutableListOf()

                // === PROCESSING / MODIFICATION PHASE ===
                var shouldUpdateCurrentUserFollowing = false
                if (initialCurrentFollowing.remove(userId)) {
                    shouldUpdateCurrentUserFollowing = true
                }

                var shouldUpdateTargetFollowers = false
                if (initialTargetFollowers.remove(currentId)) {
                    shouldUpdateTargetFollowers = true
                }

                var shouldUpdateCurrentUserFriends = false
                if (initialCurrentUserFriends.remove(userId)) {
                    shouldUpdateCurrentUserFriends = true
                }

                var shouldUpdateTargetUserFriends = false
                if (initialTargetUserFriends.remove(currentId)) {
                    shouldUpdateTargetUserFriends = true
                }

                // === WRITE PHASE ===
                if (shouldUpdateCurrentUserFollowing) {
                    transaction.update(currentUserDocRef, "following", initialCurrentFollowing)
                    transaction.update(
                        currentUserDocRef,
                        "followingCount",
                        initialCurrentFollowing.size.toLong()
                    )
                }

                if (shouldUpdateTargetFollowers) {
                    transaction.update(targetUserDocRef, "followers", initialTargetFollowers)
                    transaction.update(
                        targetUserDocRef,
                        "followerCount",
                        initialTargetFollowers.size.toLong()
                    )
                }

                if (shouldUpdateCurrentUserFriends) {
                    transaction.update(currentUserDocRef, "friends", initialCurrentUserFriends)
                }

                if (shouldUpdateTargetUserFriends) {
                    transaction.update(targetUserDocRef, "friends", initialTargetUserFriends)
                }
            }.await()
        }
    }
}