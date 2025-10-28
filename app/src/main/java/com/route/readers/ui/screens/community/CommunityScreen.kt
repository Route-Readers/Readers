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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.route.readers.data.model.BookClub
import com.route.readers.data.model.ChatMessage
import com.route.readers.ui.community.used_trade.UsedBookTradeScreen
import com.route.readers.ui.components.NotificationIconWithBadge
import com.route.readers.ui.theme.DarkRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateToFriendsList: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: CommunityViewModel = remember { CommunityViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showCreateBookClubDialog by remember { mutableStateOf(false) }
    var showChatScreen by remember { mutableStateOf<BookClub?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var bookClubs by remember { mutableStateOf(listOf(
        BookClub(
            id = "1",
            name = "개발자 북클럽",
            description = "개발 관련 도서를 함께 읽어요",
            currentBook = "클린 아키텍처",
            currentBookAuthor = "로버트 C. 마틴",
            nextMeetingDate = "2024.01.20",
            memberCount = 12
        ),
        BookClub(
            id = "2", 
            name = "자기계발 모임",
            description = "자기계발서를 통해 성장해요",
            currentBook = "7가지 습관",
            currentBookAuthor = "스티븐 코비",
            nextMeetingDate = "2024.01.22",
            memberCount = 8
        )
    )) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TopAppBar(
            title = { 
                Text(
                    "커뮤니티",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ) 
            },
            actions = {
                NotificationIconWithBadge(
                    onClick = onNavigateToNotifications
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "커뮤니티",
                fontSize = 16.sp,
                color = if (selectedTab == 0) Color.Black else Color.Gray,
                fontWeight = if (selectedTab == 0) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.clickable { selectedTab = 0 }
            )
            Text(
                "중고책 거래",
                fontSize = 16.sp,
                color = if (selectedTab == 1) Color.Black else Color.Gray,
                fontWeight = if (selectedTab == 1) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.clickable { selectedTab = 1 }
            )
        }
        
        when (selectedTab) {
            0 -> {
                showChatScreen?.let { bookClub ->
                    BookClubChatScreen(
                        bookClub = bookClub,
                        onBack = { showChatScreen = null }
                    )
                } ?: CommunityContent(
                    uiState = uiState,
                    bookClubs = bookClubs,
                    onNavigateToFriendsList = onNavigateToFriendsList,
                    onShowAddFriendDialog = { showAddFriendDialog = true },
                    onRemoveFriend = { friend -> viewModel.showDeleteConfirmation(friend) },
                    onShowCreateBookClubDialog = { showCreateBookClubDialog = true },
                    onJoinBookClub = { bookClub -> showChatScreen = bookClub },
                    onSendNotification = { viewModel.sendReadingNotification() }
                )
            }
            1 -> UsedBookTradeScreen()
        }
    }
    
    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onAddFriend = { friendId ->
                viewModel.addFriend(friendId)
                showAddFriendDialog = false
            }
        )
    }
    
    if (showCreateBookClubDialog) {
        CreateBookClubDialog(
            onDismiss = { showCreateBookClubDialog = false },
            onCreateBookClub = { name, description, currentBook, author, meetingDate ->
                val newBookClub = BookClub(
                    id = (bookClubs.size + 1).toString(),
                    name = name,
                    description = description,
                    currentBook = currentBook,
                    currentBookAuthor = author,
                    nextMeetingDate = meetingDate,
                    memberCount = 1
                )
                bookClubs = bookClubs + newBookClub
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
    
    uiState.friendToDelete?.let { friend ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteFriend() },
            title = { Text("친구 삭제") },
            text = { Text("${friend.name}님을 친구에서 삭제하시겠습니까?") },
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
    onShowAddFriendDialog: () -> Unit,
    onRemoveFriend: (Friend) -> Unit,
    onShowCreateBookClubDialog: () -> Unit,
    onJoinBookClub: (BookClub) -> Unit,
    onSendNotification: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            
            // 주간 독서 챌린지
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkRed)
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
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "주간 독서 챌린지",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            "2명 남음",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "이번 주에 책 3권 읽기",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "내 진행률",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = 0.67f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color.White,
                        trackColor = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "156명 참여 중",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = { /* 챌린지 참여 */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("챌린지 참여")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 친구 섹션
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "내 친구들 (${uiState.friends.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onShowAddFriendDialog
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "친구 추가",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("친구 추가", fontSize = 14.sp)
                    }
                }
                if (uiState.hasMoreFriends) {
                    TextButton(onClick = onNavigateToFriendsList) {
                        Text("전체보기", color = Color.Gray)
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
            
            // 북클럽 섹션
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Book,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
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
            BookClubItem(
                bookClub = bookClub,
                onJoinClick = { onJoinBookClub(bookClub) }
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
            
            // 나의 업적 섹션
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
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
            
            // 친구에게 독서 알림 보내기
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkRed)
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
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "친구에게 독서 알림 보내기",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "함께 읽을 친구를 초대해보세요!",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                    Button(
                        onClick = { onSendNotification() },
                        enabled = !uiState.isNotificationSending,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (uiState.isNotificationSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
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
                .background(Color(0xFFFFE4B5), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Book,
                contentDescription = null,
                tint = Color(0xFFFF8C00)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                bookClub.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "현재 도서: ${bookClub.currentBook}",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                "다음 모임: ${bookClub.nextMeetingDate}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        
        TextButton(onClick = onJoinClick) {
            Text(
                "참여",
                fontSize = 14.sp,
                color = Color.Blue
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
                    modifier = Modifier.fillMaxWidth(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookClubChatScreen(
    bookClub: BookClub,
    onBack: () -> Unit
) {
    var messages by remember { mutableStateOf(listOf(
        ChatMessage(
            id = "1",
            bookClubId = bookClub.id,
            senderId = "user1",
            senderName = "김독서",
            message = "안녕하세요! 오늘 3장까지 읽었어요",
            timestamp = System.currentTimeMillis() - 3600000
        ),
        ChatMessage(
            id = "2", 
            bookClubId = bookClub.id,
            senderId = "user2",
            senderName = "이책벌레",
            message = "저도 방금 3장 끝냈습니다. 정말 흥미로운 내용이네요!",
            timestamp = System.currentTimeMillis() - 1800000
        )
    )) }
    var newMessage by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TopAppBar(
            title = { 
                Column {
                    Text(
                        bookClub.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${bookClub.currentBook} - ${bookClub.memberCount}명",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                ChatMessageItem(message = message)
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                placeholder = { Text("메시지를 입력하세요...") },
                modifier = Modifier.weight(1f),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newMessage.isNotBlank()) {
                        val message = ChatMessage(
                            id = (messages.size + 1).toString(),
                            bookClubId = bookClub.id,
                            senderId = "currentUser",
                            senderName = "나",
                            message = newMessage,
                            timestamp = System.currentTimeMillis()
                        )
                        messages = messages + message
                        newMessage = ""
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "전송")
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isMyMessage = message.senderId == "currentUser"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isMyMessage) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.Gray, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    message.senderName.first().toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isMyMessage) {
                Text(
                    message.senderName,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isMyMessage) Color(0xFF4285F4) else Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    message.message,
                    modifier = Modifier.padding(12.dp),
                    color = if (isMyMessage) Color.White else Color.Black
                )
            }
        }
    }
}

@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    onAddFriend: (String) -> Unit
) {
    var friendId by remember { mutableStateOf("") }
    
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
                    "친구 추가",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = friendId,
                    onValueChange = { friendId = it },
                    label = { Text("친구 아이디") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            if (friendId.isNotBlank()) {
                                onAddFriend(friendId)
                            }
                        }
                    ) {
                        Text("추가")
                    }
                }
            }
        }
    }
}

@Composable
fun FriendItemWithDelete(
    friend: Friend,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.Gray, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                friend.name.first().toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                friend.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                friend.currentBook,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (friend.isOnline) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "온라인",
                        tint = Color.Green,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "알림",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "친구 삭제",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                friend.lastActive,
                fontSize = 12.sp,
                color = Color.Gray
            )
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                subtitle,
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}
