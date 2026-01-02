package com.route.readers.ui.screens.community

import android.app.Application
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.data.model.BookClub
import com.route.readers.data.model.Book
import com.route.readers.data.model.User
import com.route.readers.ui.screens.community.CommunityViewModel
import com.route.readers.ui.community.used_trade.UsedBookTradeScreen
import com.route.readers.ui.components.BookClubCard
import com.route.readers.ui.components.UserProfileImage
import com.route.readers.ui.screens.bookclub.BookClubChatScreen
import com.route.readers.ui.theme.PrimaryRed
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateToFriendsList: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToUsedBookDetail: (String) -> Unit = {},
    onNavigateToChatList: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    isActive: Boolean = false,
    onChatScreenChanged: (Boolean) -> Unit = {}
) {
    val localContext = LocalContext.current
    val application = localContext.applicationContext as Application
    val viewModel: CommunityViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return CommunityViewModel(application) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    var showCreateBookClubDialog by remember { mutableStateOf(false) }
    var showChatScreen by remember { mutableStateOf<BookClub?>(null) }
    val communityListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    LaunchedEffect(showChatScreen) {
        onChatScreenChanged(showChatScreen != null)
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            viewModel.refreshChallenges()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (showChatScreen == null) {
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isFriendsLoading || uiState.isBookClubsLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (showChatScreen != null) {
            showChatScreen?.let { bookClub ->
                BookClubChatScreen(
                    bookClubId = bookClub.id,
                    bookClubName = bookClub.name,
                    onBackClick = { showChatScreen = null }
                )
            }
        } else {
            CommunityContent(
                uiState = uiState,
                bookClubs = uiState.bookClubs,
                onNavigateToFriendsList = onNavigateToFriendsList,
                onRemoveFriend = { user -> viewModel.showDeleteConfirmation(user) },
                onShowCreateBookClubDialog = { showCreateBookClubDialog = true },
                onJoinBookClub = { bookClub -> showChatScreen = bookClub },
                onToggleBookClubMembership = { bookClubId, isJoined ->
                    if (isJoined) {
                        viewModel.leaveBookClub(bookClubId)
                    } else {
                        viewModel.joinBookClub(bookClubId)
                    }
                },
                onSendNotification = { viewModel.sendReadingNotification() },
                onSendNotificationToFriend = { friendId -> viewModel.sendReadingNotificationToFriend(friendId) },
                onJoinChallenge = { challengeId -> viewModel.joinChallenge(challengeId) },
                onResetChallenge = { challengeId -> viewModel.resetChallenge(challengeId) },
                onNavigateToUserProfile = onNavigateToUserProfile,
                currentUserId = viewModel.currentUserId,
                listState = communityListState,
                communityViewModel = viewModel
            )
        }
    }

    if (showCreateBookClubDialog) {
        CreateBookClubDialog(
            onDismiss = { showCreateBookClubDialog = false },
            onCreateBookClub = { name, description, currentBook, author, meetingDate, cover, genre, bookDesc ->
                viewModel.createBookClub(name, description, currentBook, author, meetingDate, cover, genre, bookDesc)
                showCreateBookClubDialog = false
            }
        )
    }

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


@Composable
fun CommunityContent(
    uiState: CommunityUiState,
    bookClubs: List<BookClub>,
    onNavigateToFriendsList: () -> Unit,
    onRemoveFriend: (User) -> Unit,
    onShowCreateBookClubDialog: () -> Unit,
    onJoinBookClub: (BookClub) -> Unit,
    onToggleBookClubMembership: (String, Boolean) -> Unit,
    onSendNotification: () -> Unit,
    onSendNotificationToFriend: (String) -> Unit,
    onJoinChallenge: (String) -> Unit = {},
    onResetChallenge: (String) -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit,
    currentUserId: String = "",
    listState: LazyListState,
    communityViewModel: CommunityViewModel? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        state = listState
    ) {
        // 챌린지 카드
        item {
            SwipeableChallengeCard(
                userChallenges = uiState.userActiveChallenges,
                availableChallenges = uiState.availableChallenges,
                onChallengeSelected = { challenge -> onJoinChallenge(challenge.id) },
                onChallengeReset = onResetChallenge,
                currentUserId = currentUserId,
                isChallengesLoading = uiState.isChallengesLoading,
                consecutiveReadingDays = uiState.consecutiveReadingDays,
                communityViewModel = communityViewModel
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 친구 목록 헤더
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "내 친구들 (${uiState.friends.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (uiState.hasMoreFriends) {
                    TextButton(onClick = onNavigateToFriendsList) {
                        Text("전체보기", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.isFriendsLoading) {
            items(3) {
                FriendPlaceholderRow()
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            items(uiState.displayedFriends) { friend ->
                SwipeableFriendItem(
                    friend = friend,
                    onNotifyClick = { onSendNotificationToFriend(friend.uid) },
                    onDeleteClick = { onRemoveFriend(friend) },
                    onProfileClick = { onNavigateToUserProfile(friend.uid) },
                    hasReadToday = uiState.friendsReadingStatus[friend.uid] ?: false
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Book,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "북클럽",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.isBookClubsLoading) {
            items(2) {
                BookClubPlaceholderCard()
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            items(bookClubs) { bookClub ->
                BookClubCard(
                    bookClub = bookClub,
                    onJoinClick = { onToggleBookClubMembership(bookClub.id, bookClub.isJoined) },
                    onChatClick = if (bookClub.isJoined) { { onJoinBookClub(bookClub) } } else null
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        item {
            TextButton(
                onClick = onShowCreateBookClubDialog,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ 새 북클럽 만들기")
            }
        }
    }
}

@Composable
fun SwipeableFriendItem(
    friend: User,
    onNotifyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onProfileClick: () -> Unit,
    hasReadToday: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserProfileImage(user = friend, size = 48.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                friend.nickname,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        if (hasReadToday) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = "오늘 독서 완료",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        } else {
            IconButton(
                onClick = onNotifyClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "독서 알림 보내기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "친구 삭제",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun FriendPlaceholderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
fun BookClubPlaceholderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(18.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
fun CreateBookClubDialog(
    onDismiss: () -> Unit,
    onCreateBookClub: (String, String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 북클럽 만들기") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("북클럽 이름") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreateBookClub(name, description, "", "", "", "", "", "")
                },
                enabled = name.isNotBlank()
            ) {
                Text("만들기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
