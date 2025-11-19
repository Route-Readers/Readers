package com.route.readers.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.Notification
import com.route.readers.data.remote.FriendsRepository
import com.route.readers.data.remote.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val notificationRepository = NotificationRepository()
    private val friendsRepository = FriendsRepository()
    
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    init {
        loadNotifications()
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
    

    
    private fun updateUnreadCount() {
        val unreadNotifications = _notifications.value.count { !it.isRead }
        _unreadCount.value = unreadNotifications
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
