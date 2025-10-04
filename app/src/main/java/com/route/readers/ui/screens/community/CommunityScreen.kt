package com.route.readers.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.route.readers.ui.community.used_trade.UsedBookTradeScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateToFriendsList: () -> Unit = {},
    viewModel: CommunityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: 커뮤니티, 1: 중고책 거래
    
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
                IconButton(onClick = { /* 알림 */ }) {
                    Icon(Icons.Default.Notifications, contentDescription = "알림")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        // 탭 영역
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
        
        // 탭 내용
        when (selectedTab) {
            0 -> CommunityContent(
                uiState = uiState,
                onNavigateToFriendsList = onNavigateToFriendsList,
                onShowAddFriendDialog = { showAddFriendDialog = true },
                onRemoveFriend = { friendId -> viewModel.removeFriend(friendId) }
            )
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
    
    // 친구 추가 결과 메시지 표시
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
}

@Composable
fun CommunityContent(
    uiState: CommunityUiState,
    onNavigateToFriendsList: () -> Unit,
    onShowAddFriendDialog: () -> Unit,
    onRemoveFriend: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
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
                onDeleteClick = { onRemoveFriend(friend.id) }
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
            
            BookClubItem(
                title = "개발자 북클럽",
                author = "현재 도서: 클린 아키텍처",
                days = "24일",
                date = "다음 모임: 2024.01.20"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            BookClubItem(
                title = "자기계발 모임",
                author = "현재 도서: 7가지 습관",
                days = "18일",
                date = "다음 모임: 2024.01.22"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = { /* 새 북클럽 만들기 */ },
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
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
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
                        onClick = { /* 보내기 */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("보내기")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
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
fun BookClubItem(
    title: String,
    author: String,
    days: String,
    date: String
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
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                author,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                date,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        
        Text(
            "참여",
            fontSize = 14.sp,
            color = Color.Blue
        )
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
