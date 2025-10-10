package com.route.readers.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.FriendRequest
import com.route.readers.data.model.Notification
import com.route.readers.data.remote.FriendRequestRepository
import com.route.readers.data.remote.FriendsRepository
import com.route.readers.data.remote.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val notificationRepository = NotificationRepository()
    private val friendRequestRepository = FriendRequestRepository()
    private val friendsRepository = FriendsRepository()
    
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    init {
        loadNotifications()
        loadFriendRequests()
    }
    
    fun loadNotifications() {
        viewModelScope.launch {
            notificationRepository.loadNotifications()
            notificationRepository.notifications.collect { notifications ->
                android.util.Log.d("NotificationViewModel", "Loaded ${notifications.size} notifications")
                notifications.forEach { notification ->
                    android.util.Log.d("NotificationViewModel", "Notification: ${notification.title} - ${notification.message}")
                }
                _notifications.value = notifications
                updateUnreadCount()
            }
        }
    }
    
    fun loadFriendRequests() {
        viewModelScope.launch {
            friendRequestRepository.loadPendingRequests()
            friendRequestRepository.pendingRequests.collect { requests ->
                _friendRequests.value = requests
                updateUnreadCount()
            }
        }
    }
    
    private fun updateUnreadCount() {
        val unreadNotifications = _notifications.value.count { !it.isRead }
        val pendingRequests = _friendRequests.value.size
        _unreadCount.value = unreadNotifications + pendingRequests
    }
    
    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            val success = friendRequestRepository.acceptFriendRequest(requestId)
            if (success) {
                // 알림 메시지 변경
                updateNotificationMessage(requestId, "친구 요청이 수락되었습니다")
                // 친구 목록 새로고침
                friendsRepository.loadFriends()
                // 친구 요청 목록 새로고침
                loadFriendRequests()
                updateUnreadCount()
            }
        }
    }
    
    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            val success = friendRequestRepository.rejectFriendRequest(requestId)
            if (success) {
                // 알림 메시지 변경
                updateNotificationMessage(requestId, "친구 요청이 거절되었습니다")
                loadFriendRequests()
                updateUnreadCount()
            }
        }
    }
    
    private fun updateNotificationMessage(requestId: String, newMessage: String) {
        viewModelScope.launch {
            // 로컬 상태 업데이트
            val currentNotifications = _notifications.value
            val updatedNotifications = currentNotifications.map { notification ->
                if (notification.data["requestId"] == requestId) {
                    notification.copy(
                        message = newMessage,
                        isRead = true
                    )
                } else {
                    notification
                }
            }
            _notifications.value = updatedNotifications
            
            // Firestore 업데이트
            try {
                val notificationToUpdate = currentNotifications.find { 
                    it.data["requestId"] == requestId 
                }
                notificationToUpdate?.let { notification ->
                    notificationRepository.updateNotification(
                        notificationId = notification.id,
                        message = newMessage,
                        isRead = true
                    )
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
            updateUnreadCount()
        }
    }
    
    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
            updateUnreadCount()
        }
    }
}
