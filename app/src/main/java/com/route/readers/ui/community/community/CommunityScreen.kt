package com.route.readers.ui.community.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.Challenge
import com.route.readers.ui.community.used_trade.UsedBookTradeScreen
import com.route.readers.ui.theme.DarkRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    // onNavigateToNotifications: () -> Unit // TopAppBar를 수정하면서 이 콜백은 잠시 제거합니다.
    viewModel: CommunityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 상단 타이틀 및 탭 영역을 새로운 디자인으로 수정
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "커뮤니티",
                fontSize = 18.sp,
                color = if (selectedTab == 0) Color.Black else Color.Gray,
                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clickable { selectedTab = 0 }
            )
            Text(
                text = "중고책 거래",
                fontSize = 18.sp,
                color = if (selectedTab == 1) Color.Black else Color.Gray,
                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clickable { selectedTab = 1 }
            )
        }

        // 탭 내용
        when (selectedTab) {
            0 -> CommunityTabContent(
                uiState = uiState,
                onJoinChallenge = { challengeId -> viewModel.joinChallenge(challengeId) }
            )
            1 -> UsedBookTradeScreen()
        }
    }
}

@Composable
fun CommunityTabContent(
    uiState: CommunityUiState,
    onJoinChallenge: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 100.dp // 하단 네비게이션 바에 가려지지 않도록 패딩
        )
    ) {
        // ▼▼▼ "주간 독서 챌린지" 섹션을 동적으로 구현합니다. ▼▼▼
        item {
            Text("주간 독서 챌린지", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.isLoadingChallenges) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DarkRed)
                }
            }
        } else if (uiState.challenges.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("현재 진행 중인 챌린지가 없습니다.", color = Color.Gray)
                }
            }
        } else {
            items(uiState.challenges, key = { it.id }) { challenge ->
                ChallengeCard(
                    challenge = challenge,
                    onJoinClick = { onJoinChallenge(challenge.id) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        // ▲▲▲ 챌린지 섹션 수정 완료 ▲▲▲

        item { Spacer(modifier = Modifier.height(32.dp)) }

        // 친구들 헤더
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = "친구들", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("내 친구들", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { /* TODO: 전체보기 화면으로 이동 */ }) {
                    Text("전체보기", color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 친구 목록 (uiState에서 가져옴)
        if (uiState.friends.isEmpty()) {
            item {
                Text("친구 목록이 비어있습니다.", color = Color.Gray, modifier = Modifier.padding(vertical = 24.dp))
            }
        } else {
            items(uiState.friends, key = { it.name }) { friend ->
                FriendItem(friend = friend)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 북클럽 섹션 (하드코딩된 부분 유지)
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Book, contentDescription = "북클럽", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("북클럽", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // ... 이하 하드코딩된 북클럽, 업적 등은 일단 그대로 둡니다.
        // 추후 이 부분들도 ViewModel과 연동하여 동적으로 변경할 수 있습니다.
    }
}

@Composable
fun ChallengeCard(challenge: Challenge, onJoinClick: () -> Unit) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isParticipating = currentUserId in challenge.participants
    val myProgress = challenge.progress[currentUserId] ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkRed)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = challenge.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = challenge.description,
                color = Color.LightGray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 챌린지 참여자만 진행률 표시
            if (isParticipating && challenge.goal > 0) {
                Text("내 진행률: $myProgress / ${challenge.goal}", color = Color.LightGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (myProgress.toFloat() / challenge.goal.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Color.White,
                    trackColor = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${challenge.participants.size}명 참여 중",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
                Button(
                    onClick = onJoinClick,
                    enabled = !isParticipating, // 이미 참여 중이면 비활성화
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isParticipating) "참여 완료" else "챌린지 참여")
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
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(friend.name.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(friend.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(friend.currentBook, fontSize = 14.sp, color = Color.Gray)
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (friend.isOnline) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    friend.lastActive,
                    fontSize = 12.sp,
                    color = if (friend.isOnline) Color(0xFF4CAF50) else Color.Gray
                )
            }
        }
    }
}
