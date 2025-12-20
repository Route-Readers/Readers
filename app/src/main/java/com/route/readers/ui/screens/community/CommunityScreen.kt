package com.route.readers.ui.screens.community

import android.app.Application
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.data.model.BookClub
import com.route.readers.data.model.User
import com.route.readers.ui.screens.community.CommunityViewModel
import com.route.readers.ui.community.used_trade.UsedBookTradeScreen
import com.route.readers.ui.components.BookClubCard
import com.route.readers.ui.components.UserProfileImage
import com.route.readers.ui.screens.bookclub.BookClubChatScreen
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
    // CommunityScreen이 자체적으로 ViewModel을 생성합니다.
    // Application Context가 필요한 ViewModel이므로 Factory를 사용하여 생성합니다.
    val context = LocalContext.current
    val application = context.applicationContext as Application
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
    var selectedTab by remember { mutableStateOf(0) }
    val communityListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val usedTradeListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    // 채팅방 상태 변경 시 콜백 호출
    LaunchedEffect(showChatScreen) {
        onChatScreenChanged(showChatScreen != null)
    }

    // 화면이 활성화될 때 데이터를 새로고침합니다.
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
        // 채팅방이 아닐 때만 상단 여백과 로딩 표시
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

            // "커뮤니티" / "중고책 거래" 탭
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "커뮤니티",
                    fontSize = 18.sp,
                    color = if (selectedTab == 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { selectedTab = 0 }
                )
                Text(
                    "중고책 거래",
                    fontSize = 18.sp,
                    color = if (selectedTab == 1) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { selectedTab = 1 }
                )
            }
        }

        // 선택된 탭에 따라 다른 화면을 보여줍니다.
        when (selectedTab) {
            0 -> {
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
                        onJoinChallenge = { challengeId -> viewModel.joinChallenge(challengeId) },
                        onResetChallenge = { viewModel.resetChallenge() },
                        onNavigateToUserProfile = onNavigateToUserProfile,
                        currentUserId = viewModel.currentUserId,
                        listState = communityListState,
                        communityViewModel = viewModel
                    )
                }
            }
            1 -> UsedBookTradeScreen(
                onNavigateToDetail = onNavigateToUsedBookDetail,
                onNavigateToChatList = onNavigateToChatList,
                listState = usedTradeListState
            )
        }
    }

    // 새 북클럽 만들기 다이얼로그
    if (showCreateBookClubDialog) {
        CreateBookClubDialog(
            onDismiss = { showCreateBookClubDialog = false },
            onCreateBookClub = { name, description, currentBook, author, meetingDate, cover, genre, bookDesc ->
                viewModel.createBookClub(name, description, currentBook, author, meetingDate, cover, genre, bookDesc)
                showCreateBookClubDialog = false
            }
        )
    }

    // 알림 다이얼로그 (친구 추가 메시지 등)
    uiState.addFriendMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearAddFriendMessage() },
            title = { Text("알림") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAddFriendMessage() }) {
                    Text("확인")
                }
            }
        )
    }

    // 친구 삭제 확인 다이얼로그
    uiState.friendToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteFriend() },
            title = { Text("친구 삭제") },
            text = { Text("${user.nickname}님을 친구에서 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteFriend() }) {
                    Text("삭제")
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
    onJoinChallenge: (String) -> Unit = {},
    onResetChallenge: () -> Unit = {},
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
        item {
            SwipeableChallengeCard(
                userChallenge = uiState.userActiveChallenge,
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
                FriendItemWithDelete(
                    friend = friend,
                    onDeleteClick = { onRemoveFriend(friend) },
                    onProfileClick = { onNavigateToUserProfile(friend.uid) }
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
            items(3) {
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
                Text("새 북클럽 만들기")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 친구에게 독서 알림 보내기 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "친구에게 독서 알림 보내기",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "함께 읽을 친구를 초대해보세요!",
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    Button(
                        onClick = onSendNotification,
                        enabled = !uiState.isNotificationSending,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (uiState.isNotificationSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("보내기")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
    var currentBook by remember { mutableStateOf("") }
    var selectedBook by remember { mutableStateOf<com.route.readers.data.model.Book?>(null) }
    var searchResults by remember { mutableStateOf<List<com.route.readers.data.model.Book>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }

    val bookRepository = remember { com.route.readers.data.remote.BookRepository() }

    LaunchedEffect(currentBook) {
        if (currentBook.length >= 2) {
            isSearching = true
            delay(500) // 사용자 타이핑 대기
            try {
                val results = bookRepository.getBookSearch(currentBook, maxResults = 5)
                searchResults = results
                showDropdown = results.isNotEmpty()
            } catch (e: Exception) {
                searchResults = emptyList()
                showDropdown = false
            }
            isSearching = false
        } else {
            searchResults = emptyList()
            showDropdown = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "새 북클럽 만들기",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("북클럽 이름") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 책 검색 UI
                Column {
                    OutlinedTextField(
                        value = currentBook,
                        onValueChange = {
                            currentBook = it
                            if (it != selectedBook?.title) {
                                selectedBook = null
                            }
                        },
                        label = { Text("현재 읽을 책") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    )

                    if (showDropdown && searchResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            LazyColumn {
                                items(searchResults) { book ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedBook = book
                                                currentBook = book.title
                                                showDropdown = false
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = book.cover,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .width(30.dp)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = book.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = book.author,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                selectedBook?.let { book ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = book.cover,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(45.dp)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = book.author,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && selectedBook != null) {
                                onCreateBookClub(
                                    name, 
                                    description, 
                                    selectedBook!!.title, 
                                    selectedBook!!.author, 
                                    "",
                                    selectedBook!!.cover,
                                    selectedBook!!.categoryName ?: "",
                                    selectedBook!!.description
                                )
                            }
                        },
                        enabled = name.isNotBlank() && selectedBook != null
                    ) {
                        Text("만들기")
                    }
                }
            }
        }
    }
}

@Composable
fun FriendItemWithDelete(
    friend: User,
    onDeleteClick: () -> Unit,
    onProfileClick: () -> Unit
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
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun BookClubPlaceholderCard() {
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
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .height(10.dp)
                            .weight(1f)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

