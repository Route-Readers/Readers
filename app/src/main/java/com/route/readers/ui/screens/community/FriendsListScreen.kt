package com.route.readers.ui.screens.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(
    onBackClick: () -> Unit,
    onUserClick: (String) -> Unit = {},
    viewModel: CommunityViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "내 친구들 (${uiState.friends.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.friends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    "친구가 없습니다.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.friends) { friend ->
                    SwipeableFriendItem(
                        friend = friend,
                        onNotifyClick = { viewModel.sendReadingNotificationToFriend(friend.uid) },
                        onDeleteClick = { viewModel.showDeleteConfirmation(friend) },
                        onProfileClick = { onUserClick(friend.uid) },
                        hasReadToday = uiState.friendsReadingStatus[friend.uid] ?: false
                    )
                }
            }
        }

        // 친구 삭제 확인 다이얼로그
        uiState.friendToDelete?.let { user ->
            AlertDialog(
                onDismissRequest = { viewModel.cancelDeleteFriend() },
                title = { Text("친구 삭제") },
                text = { Text("${user.nickname}님을 친구에서 삭제하시겠습니까?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDeleteFriend() }) {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDeleteFriend() }) {
                        Text("취소")
                    }
                }
            )
        }
    }
}
