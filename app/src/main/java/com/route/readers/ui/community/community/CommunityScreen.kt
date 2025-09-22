package com.route.readers.ui.community.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.route.readers.ui.community.used_trade.UsedBookTradeScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 탭 내용
        when (selectedTab) {
            0 -> CommunityTabContent(uiState)
            1 -> UsedBookTradeScreen()
        }
    }
}

@Composable
fun CommunityTabContent(uiState: CommunityUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 100.dp
        )
    ) {
        // 챌린지 카드
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
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
                        Text("2명 남음", color = Color.Gray, fontSize = 14.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("이번 주에 책 3권 읽기", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("내 진행률", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = 0.67f,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color.White,
                        trackColor = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("156명 참여 중", color = Color.Gray, fontSize = 14.sp)
                        Button(
                            onClick = { },
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
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
        
        // 친구들 헤더
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("내 친구들", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { }) {
                    Text("전체보기", color = Color.Gray)
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        // 친구 목록
        items(uiState.friends.size) { index ->
            val friend = uiState.friends[index]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(Color.Gray, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(friend.name.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(friend.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(friend.currentBook, fontSize = 14.sp, color = Color.Gray)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Icon(
                        if (friend.isOnline) Icons.Default.Send else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (friend.isOnline) Color.Green else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(friend.lastActive, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 북클럽 섹션
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("북클럽", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(Color.Gray, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text("개발자 북클럽", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("현재 도서: 클린 아키텍처", fontSize = 14.sp, color = Color.Gray)
                    Text("24명 · 다음 모임: 2024.01.20", fontSize = 12.sp, color = Color.Gray)
                }
                
                Text("참여", fontSize = 14.sp, color = Color.Blue, fontWeight = FontWeight.Medium)
            }
        }
        
        item { Spacer(modifier = Modifier.height(12.dp)) }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(Color.Gray, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text("자기계발 모임", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("현재 도서: 7가지 습관", fontSize = 14.sp, color = Color.Gray)
                    Text("18명 · 다음 모임: 2024.01.22", fontSize = 12.sp, color = Color.Gray)
                }
                
                Text("참여", fontSize = 14.sp, color = Color.Blue, fontWeight = FontWeight.Medium)
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Gray),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("새 북클럽 만들기")
            }
        }
        
        // 업적 섹션
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("나의 업적", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("첫 완독", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("달성", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("일주일 연속 독서", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("달성", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(12.dp)) }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("친구 10명 만들기", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("달성", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("챌린지 우승", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("달성", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
        
        // 알림 카드
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text("친구에게 독서 알림 보내기", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("함께 읽을 친구를 초대해보세요!", color = Color.Gray, fontSize = 14.sp)
                    }
                    
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("보내기")
                    }
                }
            }
        }
    }
}
