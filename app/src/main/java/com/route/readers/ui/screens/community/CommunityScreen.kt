package com.route.readers.ui.screens.community

import android.app.Application
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
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
import com.route.readers.ui.screens.bookclub.BookClubDetailScreen
import com.route.readers.ui.theme.PrimaryRed
import kotlinx.coroutines.delay

import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

// Premium Theme Colors Local Definitions
private val PremiumBackground = Color.White
private val PremiumText = Color(0xFF333333)
private val PremiumGold = Color(0xFFD4AF37)
private val PremiumBurgundy = PrimaryRed

@Composable
fun ChallengeSuccessDialog(
    challenge: Challenge,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "success_anim")
            
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            val rotate by infiniteTransition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "rotate"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(32.dp)
                    .scale(scale)
                    .rotate(rotate)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier.size(100.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "챌린지 달성!",
                    color = PremiumGold,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = challenge.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PremiumBurgundy),
                    border = BorderStroke(2.dp, PremiumGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+ ${challenge.reward}",
                            color = PremiumGold,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    text = "화면을 터치하여 계속하기",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

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
    var showDetailScreen by remember { mutableStateOf<BookClub?>(null) }
    val communityListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    // 시스템 뒤로가기 처리
    BackHandler(enabled = showChatScreen != null || showDetailScreen != null) {
        when {
            showChatScreen != null -> showChatScreen = null
            showDetailScreen != null -> showDetailScreen = null
        }
    }

    LaunchedEffect(showChatScreen, showDetailScreen) {
        onChatScreenChanged(showChatScreen != null || showDetailScreen != null)
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            viewModel.refreshChallenges()
        }
    }

    LaunchedEffect(uiState.addFriendMessage) {
        uiState.addFriendMessage?.let {
            Toast.makeText(localContext, it, Toast.LENGTH_SHORT).show()
            viewModel.clearAddFriendMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumBackground)
    ) {
        if (showChatScreen == null) {
            // Spacer(modifier = Modifier.height(16.dp)) // Removed top spacer for cleaner look

            if (uiState.isFriendsLoading || uiState.isBookClubsLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = PremiumBurgundy
                )
            }
        }

        when {
            showChatScreen != null -> {
                showChatScreen?.let { bookClub ->
                    BookClubChatScreen(
                        bookClubId = bookClub.id,
                        bookClubName = bookClub.name,
                        onBackClick = { showChatScreen = null },
                        onNavigateToProfile = onNavigateToUserProfile
                    )
                }
            }
            showDetailScreen != null -> {
                showDetailScreen?.let { bookClub ->
                    BookClubDetailScreen(
                        bookClub = bookClub,
                        ownerProfile = uiState.bookClubOwnerProfiles[bookClub.createdBy],
                        onBackClick = { showDetailScreen = null },
                        onJoinClick = {
                            viewModel.joinBookClub(bookClub.id)
                            showDetailScreen = null
                            // 참여 후 채팅방으로 이동
                            showChatScreen = bookClub.copy(isJoined = true)
                        },
                        onEnterChat = {
                            showDetailScreen = null
                            showChatScreen = bookClub
                        },
                        onOwnerProfileClick = { onNavigateToUserProfile(bookClub.createdBy) }
                    )
                }
            }
            else -> {
                CommunityContent(
                    uiState = uiState,
                    bookClubs = uiState.bookClubs,
                    onNavigateToFriendsList = onNavigateToFriendsList,
                    onRemoveFriend = { user -> viewModel.showDeleteConfirmation(user) },
                    onShowCreateBookClubDialog = { showCreateBookClubDialog = true },
                    onBookClubClick = { bookClub -> 
                        if (bookClub.isJoined) {
                            // 참여중이면 바로 채팅방으로
                            showChatScreen = bookClub
                        } else {
                            // 미참여면 상세 페이지로
                            viewModel.loadBookClubOwnerProfile(bookClub.createdBy)
                            showDetailScreen = bookClub
                        }
                    },
                    onJoinBookClub = { bookClub -> showChatScreen = bookClub },
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
    }

    // 챌린지 달성 팝업
    uiState.completedChallenge?.let { challenge ->
        ChallengeSuccessDialog(
            challenge = challenge,
            onDismiss = { viewModel.dismissCompletionPopup() }
        )
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
            title = { Text("친구 삭제", fontFamily = FontFamily.Serif) },
            text = { Text("${user.nickname}님을 친구에서 삭제하시겠습니까?") },
            containerColor = Color.White,
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteFriend() }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteFriend() }) {
                    Text("취소", color = PremiumText)
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
    onBookClubClick: (BookClub) -> Unit,
    onJoinBookClub: (BookClub) -> Unit,
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
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(24.dp)
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
        }

        // 친구 목록 섹션
        item {
            Column {
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
                            tint = PremiumGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "내 친구들",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = PremiumText
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "(${uiState.friends.size})",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Serif
                        )
                    }
                    if (uiState.hasMoreFriends) {
                        TextButton(onClick = onNavigateToFriendsList) {
                            Text("전체보기", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isFriendsLoading) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(3) {
                            FriendPlaceholderRow()
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.displayedFriends.forEach { friend ->
                            SwipeableFriendItem(
                                friend = friend,
                                onNotifyClick = { onSendNotificationToFriend(friend.uid) },
                                onDeleteClick = { onRemoveFriend(friend) },
                                onProfileClick = { onNavigateToUserProfile(friend.uid) },
                                hasReadToday = uiState.friendsReadingStatus[friend.uid] ?: false,
                                isNotifying = uiState.notifyingFriendId == friend.uid
                            )
                        }
                    }
                }
            }
        }

        // 북클럽 섹션
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = PremiumGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "북클럽",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = PremiumText
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isBookClubsLoading) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(2) {
                            BookClubPlaceholderCard()
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        bookClubs.forEach { bookClub ->
                            // Wrap BookClubCard with a custom styling since we can't easily modify the component
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE0E0E0))
                            ) {
                                BookClubCard(
                                    bookClub = bookClub,
                                    onClick = { onBookClubClick(bookClub) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onShowCreateBookClubDialog,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PremiumBurgundy),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PremiumBurgundy)
                ) {
                    Text("+ 새 북클럽 만들기", fontWeight = FontWeight.Medium)
                }
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
    hasReadToday: Boolean = false,
    isNotifying: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserProfileImage(user = friend, size = 48.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friend.nickname,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = PremiumText
                )
            }

            if (isNotifying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = PremiumBurgundy
                )
            } else if (hasReadToday) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = "오늘 독서 완료",
                    tint = PremiumBurgundy,
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
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "친구 삭제",
                    tint = Color.LightGray
                )
            }
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
                .background(Color(0xFFF0F0F0))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(Color(0xFFF0F0F0))
            )
        }
    }
}

@Composable
fun BookClubPlaceholderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(18.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(Color(0xFFF0F0F0))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(Color(0xFFF0F0F0))
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
        title = { Text("새 북클럽 만들기", fontFamily = FontFamily.Serif) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("북클럽 이름") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumBurgundy,
                        focusedLabelColor = PremiumBurgundy,
                        cursorColor = PremiumBurgundy
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumBurgundy,
                        focusedLabelColor = PremiumBurgundy,
                        cursorColor = PremiumBurgundy
                    )
                )
            }
        },
        containerColor = Color.White,
        confirmButton = {
            Button(
                onClick = {
                    onCreateBookClub(name, description, "", "", "", "", "", "")
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumBurgundy)
            ) {
                Text("만들기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = PremiumText)
            }
        }
    )
}
