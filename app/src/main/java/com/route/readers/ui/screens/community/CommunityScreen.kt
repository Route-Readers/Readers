package com.route.readers.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.route.readers.data.model.BookClub
import com.route.readers.data.model.Challenge
import com.route.readers.data.model.ChatMessage
import com.route.readers.ui.community.used_trade.UsedBookTradeScreen
import com.route.readers.ui.components.BookClubCard
import com.route.readers.ui.theme.DarkRed
import com.route.readers.ui.screens.bookclub.BookClubChatScreen
import java.net.URLEncoder

import com.route.readers.data.model.User // Added User import

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToFriendsList: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToUsedBookDetail: (String) -> Unit = {},
    onNavigateToChatList: () -> Unit = {},
    isActive: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateBookClubDialog by remember { mutableStateOf(false) }
    var showChatScreen by remember { mutableStateOf<BookClub?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

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
        Spacer(modifier = Modifier.height(16.dp))

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
                        currentUserId = viewModel.currentUserId
                    )
                }
            }
            1 -> UsedBookTradeScreen(
                onNavigateToDetail = onNavigateToUsedBookDetail,
                onNavigateToChatList = onNavigateToChatList
            )
        }
    }



    if (showCreateBookClubDialog) {
        CreateBookClubDialog(
            onDismiss = { showCreateBookClubDialog = false },
            onCreateBookClub = { name, description, currentBook, author, meetingDate ->
                viewModel.createBookClub(name, description, currentBook, author, meetingDate)
                showCreateBookClubDialog = false
            }
        )
    }

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
    onRemoveFriend: (User) -> Unit, // Changed to User
    onShowCreateBookClubDialog: () -> Unit,
    onJoinBookClub: (BookClub) -> Unit,
    onToggleBookClubMembership: (String, Boolean) -> Unit,
    onSendNotification: () -> Unit,
    onJoinChallenge: (String) -> Unit = {},
    onResetChallenge: () -> Unit = {},
    currentUserId: String = ""
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SwipeableChallengeCard(
                userChallenge = uiState.userActiveChallenge,
                availableChallenges = uiState.availableChallenges,
                onChallengeSelected = { challenge -> onJoinChallenge(challenge.id) },
                onChallengeReset = onResetChallenge,
                currentUserId = currentUserId,
                isChallengesLoading = uiState.isChallengesLoading
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

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
                    Spacer(modifier = Modifier.width(8.dp))

                }
                if (uiState.hasMoreFriends) {
                    TextButton(onClick = onNavigateToFriendsList) {
                        Text("전체보기", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        items(uiState.displayedFriends) { friend ->
            FriendItemWithDelete(
                friend = friend,
                onDeleteClick = { onRemoveFriend(friend) }
            )
            Spacer(modifier = Modifier.height(16.dp))
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

        items(bookClubs) { bookClub ->
            BookClubCard(
                bookClub = bookClub,
                onJoinClick = { onToggleBookClubMembership(bookClub.id, bookClub.isJoined) },
                onChatClick = if (bookClub.isJoined) { { onJoinBookClub(bookClub) } } else null
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            TextButton(
                onClick = onShowCreateBookClubDialog,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("새 북클럽 만들기")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "나의 업적",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AchievementCard(
                    icon = Icons.Default.Book,
                    title = "첫 완독",
                    subtitle = "달성",
                    modifier = Modifier.weight(1f)
                )
                AchievementCard(
                    icon = Icons.Default.Notifications,
                    title = "일주일 연속 독서",
                    subtitle = "달성",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AchievementCard(
                    icon = Icons.Default.Group,
                    title = "친구 10명 만들기",
                    subtitle = "달성",
                    modifier = Modifier.weight(1f)
                )
                AchievementCard(
                    icon = Icons.Default.EmojiEvents,
                    title = "챌린지 우승",
                    subtitle = "달성",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "친구에게 독서 알림 보내기",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "함께 읽을 친구를 초대해보세요!",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    Button(
                        onClick = { onSendNotification() },
                        enabled = !uiState.isNotificationSending,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
fun BookClubItem(
    bookClub: BookClub,
    onJoinClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                bookClub.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "현재 도서: ${bookClub.currentBook}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "다음 모임: ${bookClub.nextMeetingDate}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TextButton(onClick = onJoinClick) {
            Text(
                "참여",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CreateBookClubDialog(
    onDismiss: () -> Unit,
    onCreateBookClub: (String, String, String, String, String) -> Unit
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
            kotlinx.coroutines.delay(500)
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
                                onCreateBookClub(name, description, selectedBook!!.title, selectedBook!!.author, "")
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
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                friend.nickname.first().toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                friend.nickname,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "친구 삭제",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}



@Composable
fun ChallengeCardInCommunity(
    challenge: com.route.readers.data.model.Challenge,
    onJoinClick: () -> Unit
) {
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isJoined = challenge.participants.contains(currentUserId)

    val daysRemaining = challenge.endDate?.let {
        val diff = it.time - System.currentTimeMillis()
        java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).toInt()
    } ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "주간 독서 챌린지",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    "${daysRemaining}일 남음",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                challenge.title,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (isJoined) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "내 진행률",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                val userProgress = challenge.progress[currentUserId] ?: 0
                val progress = if (challenge.goal > 0) userProgress.toFloat() / challenge.goal.toFloat() else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "${userProgress} / ${challenge.goal} (${(progress * 100).toInt()}%)",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${challenge.participants.size}명 참여 중",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                if (!isJoined) {
                    Button(
                        onClick = onJoinClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("챌린지 참여")
                    }
                }
            }
        }
    }
}
