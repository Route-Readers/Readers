package com.route.readers.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddFriendDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 상단 헤더
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
        
        // 전체 스크롤 가능한 컨텐츠
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item {
                // 탭 영역
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "커뮤니티",
                        fontSize = 16.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "중고책 거래",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 주간 독서 챌린지 카드
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
                        
                        // 진행률 바
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
                
                // 내 친구들 섹션 헤더
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
                            "내 친구들",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { showAddFriendDialog = true }
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
                    TextButton(onClick = { /* 전체보기 */ }) {
                        Text("전체보기", color = Color.Gray)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 친구 목록
            items(uiState.friends) { friend ->
                FriendItem(friend = friend)
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
                
                // 북클럽 아이템들
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
                
                // 새 북클럽 만들기 버튼
                TextButton(
                    onClick = { /* 새 북클럽 만들기 */ },
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
                
                // 업적 그리드
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
                
                // 친구에게 독서 알림 보내기 섹션
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
    
    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onAddFriend = { friendId ->
                viewModel.addFriend(friendId)
                showAddFriendDialog = false
            }
        )
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
fun FriendItem(friend: Friend) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 프로필 이미지 (임시)
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
