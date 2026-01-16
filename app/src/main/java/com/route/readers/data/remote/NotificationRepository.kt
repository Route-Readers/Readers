package com.route.readers.data.remote

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.route.readers.data.model.Notification
import com.route.readers.data.model.NotificationType
import com.route.readers.notification.ReadingNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository(private val context: Context? = null) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationManager = context?.let { ReadingNotificationManager(it) }

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun loadNotifications() {
        currentUserId?.let { userId ->
            try {
                android.util.Log.d(
                    "NotificationRepository",
                    "Loading notifications for user: $userId"
                )

                // orderBy 제거하고 단순하게 쿼리
                firestore.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e(
                                "NotificationRepository",
                                "Error loading notifications",
                                error
                            )
                            _notifications.value = emptyList()
                            _unreadCount.value = 0
                            return@addSnapshotListener
                        }

                        android.util.Log.d(
                            "NotificationRepository",
                            "Snapshot received with ${snapshot?.documents?.size} documents"
                        )

                        val notificationList = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                android.util.Log.d(
                                    "NotificationRepository",
                                    "Processing document: ${doc.id}"
                                )
                                val notification = Notification(
                                    id = doc.id,
                                    userId = doc.getString("userId") ?: "",
                                    type = NotificationType.valueOf(
                                        doc.getString("type") ?: "FRIEND_REQUEST"
                                    ),
                                    title = doc.getString("title") ?: "",
                                    message = doc.getString("message") ?: "",
                                    data = doc.get("data") as? Map<String, Any> ?: emptyMap(),
                                    isRead = doc.getBoolean("isRead") ?: false,
                                    createdAt = doc.getTimestamp("createdAt")
                                        ?: com.google.firebase.Timestamp.now(),
                                    timestamp = doc.getTimestamp("createdAt")?.toDate()?.time
                                        ?: System.currentTimeMillis()
                                )
                                android.util.Log.d(
                                    "NotificationRepository",
                                    "Created notification: ${notification.title}"
                                )
                                notification
                            } catch (e: Exception) {
                                android.util.Log.e(
                                    "NotificationRepository",
                                    "Error parsing notification",
                                    e
                                )
                                null
                            }
                        } ?: emptyList()

                        android.util.Log.d(
                            "NotificationRepository",
                            "Final notification list size: ${notificationList.size}"
                        )
                        _notifications.value = notificationList
                        _unreadCount.value = notificationList.count { !it.isRead }
                    }
            } catch (e: Exception) {
                android.util.Log.e("NotificationRepository", "Error setting up listener", e)
                _notifications.value = emptyList()
                _unreadCount.value = 0
            }
        }
    }

    suspend fun markAsRead(notificationId: String) {
        try {
            firestore.collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .await()

            loadNotifications()
        } catch (e: Exception) {
            // Handle error
        }
    }

    suspend fun markAllAsRead() {
        currentUserId?.let { userId ->
            try {
                // 로컬 상태 즉시 업데이트
                val updatedNotifications = _notifications.value.map { it.copy(isRead = true) }
                _notifications.value = updatedNotifications
                _unreadCount.value = 0

                // Firestore 업데이트
                val unreadNotifications = firestore.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()

                val batch = firestore.batch()
                unreadNotifications.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    suspend fun updateNotification(
        notificationId: String,
        message: String,
        isRead: Boolean
    ) {
        try {
            firestore.collection("notifications")
                .document(notificationId)
                .update(
                    mapOf(
                        "message" to message,
                        "isRead" to isRead
                    )
                )
                .await()
        } catch (e: Exception) {
            // Handle error
        }
    }

    suspend fun createNotification(
        userId: String,
        type: NotificationType,
        title: String,
        message: String,
        data: Map<String, Any> = emptyMap()
    ) {
        try {
            val notification = hashMapOf(
                "userId" to userId,
                "type" to type.name,
                "title" to title,
                "message" to message,
                "data" to data,
                "isRead" to false,
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("notifications")
                .add(notification)
                .await()

            // FCM 푸시 알림 전송용 문서를 추가하여 Cloud Function을 트리거합니다.
            val fcmRequest = hashMapOf(
                "targetUserId" to userId,
                "title" to title,
                "message" to message,
                "type" to type.name, // 어떤 종류의 알림인지 명시
                "fromUserId" to (data["fromUserId"] ?: ""),
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("fcmRequests")
                .add(fcmRequest)
                .await()

            android.util.Log.d(
                "NotificationRepository",
                "FCM request created in firestore for user: $userId"
            )
        } catch (e: Exception) {
            android.util.Log.e(
                "NotificationRepository",
                "Failed to create notification or FCM request",
                e
            )
        }
    }

    suspend fun sendReadingNotificationToFriends() {
        currentUserId?.let { userId ->
            try {
                // 현재 사용자 정보 가져오기
                val userDoc = firestore.collection("users").document(userId).get().await()
                val userName = userDoc.getString("nickname")
                    ?: userDoc.getString("name")
                    ?: userDoc.getString("displayName")
                    ?: "독서친구"

                // 맞팔 관계 확인 (following과 followers 교집합)
                val following = userDoc.get("following") as? List<String> ?: emptyList()
                val followers = userDoc.get("followers") as? List<String> ?: emptyList()
                val mutualFollowers = following.intersect(followers.toSet()).toList()

                android.util.Log.d(
                    "NotificationRepository",
                    "User: $userName, Following: ${following.size}, Followers: ${followers.size}, Mutual: ${mutualFollowers.size}"
                )

                val title = "함께 독서해요! 📚"
                val message = "${userName}님이 지금 책을 읽고 있어요. 함께 독서하시겠어요?"

                // 각 맞팔 친구에게 알림 보내기
                mutualFollowers.forEach { friendId ->
                    android.util.Log.d(
                        "NotificationRepository",
                        "Sending notification to mutual follower: $friendId"
                    )

                    // createNotification을 호출하여 Firestore 저장 및 FCM 트리거 수행
                    createNotification(
                        userId = friendId,
                        type = NotificationType.READING_INVITATION,
                        title = title,
                        message = message,
                        data = mapOf("fromUserId" to userId, "fromUserName" to userName)
                    )
                }

                android.util.Log.d(
                    "NotificationRepository",
                    "Successfully sent ${mutualFollowers.size} notifications"
                )

            } catch (e: Exception) {
                android.util.Log.e(
                    "NotificationRepository",
                    "Error sending reading notifications",
                    e
                )
            }
        }
    }
}