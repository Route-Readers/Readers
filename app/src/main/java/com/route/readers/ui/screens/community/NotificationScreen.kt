package com.route.readers.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.route.readers.data.model.FriendRequest
import com.route.readers.data.model.Notification
import com.route.readers.data.model.NotificationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val friendRequests by viewModel.friendRequests.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
        viewModel.loadFriendRequests()
    }
    
    // 알림을 읽음/안읽음으로 분리하고 시간순 정렬
    val unreadNotifications = notifications
        .filter { !it.isRead }
        .sortedByDescending { it.timestamp }
    
    val readNotifications = notifications
        .filter { it.isRead }
        .sortedByDescending { it.timestamp }
    
    // 친구 요청도 시간순 정렬
    val sortedFriendRequests = friendRequests.sortedByDescending { it.timestamp }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("알림") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.markAllAsRead() }
                    ) {
                        Text("모두 읽음")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 탭 레이아웃
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("새 알림")
                            val newCount = sortedFriendRequests.size + unreadNotifications.size
                            if (newCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge {
                                    Text("$newCount")
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("읽은 알림") }
                )
            }
            
            // 탭 내용
            when (selectedTabIndex) {
                0 -> NewNotificationsTab(
                    friendRequests = sortedFriendRequests,
                    unreadNotifications = unreadNotifications,
                    viewModel = viewModel
                )
                1 -> ReadNotificationsTab(
                    readNotifications = readNotifications,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun NewNotificationsTab(
    friendRequests: List<FriendRequest>,
    unreadNotifications: List<Notification>,
    viewModel: NotificationViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 친구 요청 (최우선)
        if (friendRequests.isNotEmpty()) {
            item {
                Text(
                    text = "친구 요청 (${friendRequests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            items(friendRequests) { request ->
                FriendRequestItem(
                    request = request,
                    onAccept = { viewModel.acceptFriendRequest(request.id) },
                    onReject = { viewModel.rejectFriendRequest(request.id) }
                )
            }
        }
        
        // 읽지 않은 알림
        if (unreadNotifications.isNotEmpty()) {
            item {
                Text(
                    text = "새 알림 (${unreadNotifications.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            items(unreadNotifications) { notification ->
                NotificationItem(
                    notification = notification,
                    onClick = { viewModel.markAsRead(notification.id) },
                    isNew = true
                )
            }
        }
        
        if (friendRequests.isEmpty() && unreadNotifications.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "새로운 알림이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ReadNotificationsTab(
    readNotifications: List<Notification>,
    viewModel: NotificationViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (readNotifications.isNotEmpty()) {
            items(readNotifications) { notification ->
                NotificationItem(
                    notification = notification,
                    onClick = { /* 이미 읽음 */ },
                    isNew = false
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "읽은 알림이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${request.senderNickname}님이 친구 요청을 보냈습니다",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("수락")
                }
                
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("거절")
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    isNew: Boolean = false,
    viewModel: NotificationViewModel = viewModel()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isNew) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isNew) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isNew) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isNew) FontWeight.Bold else FontWeight.Normal,
                            color = if (isNew) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isNew) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = formatTimestamp(notification.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 친구 요청일 때 수락/거절 버튼 추가 (아직 처리되지 않은 경우만)
            if (notification.type == com.route.readers.data.model.NotificationType.FRIEND_REQUEST && 
                !notification.message.contains("수락되었습니다") && 
                !notification.message.contains("거절되었습니다")) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val requestId = notification.data["requestId"] as? String ?: ""
                            viewModel.acceptFriendRequest(requestId)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("수락")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val requestId = notification.data["requestId"] as? String ?: ""
                            viewModel.rejectFriendRequest(requestId)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("거절")
                    }
                }
            }
        }
    }
}

@Composable
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "방금 전"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}분 전"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}시간 전"
        else -> "${diff / (24 * 60 * 60 * 1000)}일 전"
    }
}
