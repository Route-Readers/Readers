package com.route.readers.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
        
        // 내 친구들 섹션
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
            }
            TextButton(onClick = { /* 전체보기 */ }) {
                Text("전체보기", color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 친구 목록
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(uiState.friends) { friend ->
                FriendItem(friend = friend)
                Spacer(modifier = Modifier.height(16.dp))
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
