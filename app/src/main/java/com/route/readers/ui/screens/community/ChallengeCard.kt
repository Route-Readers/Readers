package com.route.readers.ui.screens.community

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.route.readers.data.model.Challenge

enum class ChallengeCardState {
    INITIAL,      // 초기 "참여하세요" 카드
    SELECTING,    // 챌린지 선택 화면
    ACTIVE        // 선택된 챌린지 진행 중
}

@Composable
fun SwipeableChallengeCard(
    userChallenge: Challenge?,
    availableChallenges: List<Challenge>,
    onChallengeSelected: (Challenge) -> Unit,
    onChallengeReset: () -> Unit,
    currentUserId: String
) {
    var cardState by remember { mutableStateOf(
        if (userChallenge != null) ChallengeCardState.ACTIVE else ChallengeCardState.INITIAL
    ) }
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = 50f

    // LaunchedEffect를 제거하고 상태 전환을 직접 관리

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        when (cardState) {
            ChallengeCardState.INITIAL -> {
                InitialChallengeCard(
                    offsetX = offsetX,
                    onSwipe = { offset ->
                        offsetX = offset
                        if (kotlin.math.abs(offsetX) > swipeThreshold) {
                            cardState = ChallengeCardState.SELECTING
                            offsetX = 0f
                        }
                    }
                )
            }
            ChallengeCardState.SELECTING -> {
                ChallengeSelectionCard(
                    challenges = availableChallenges,
                    onSelect = { challenge ->
                        onChallengeSelected(challenge)
                        cardState = ChallengeCardState.ACTIVE // 참여 시 즉시 ACTIVE 상태로 변경 (낙관적 업데이트)
                    }
                )
            }
            ChallengeCardState.ACTIVE -> {
                if (userChallenge != null) {
                    ActiveChallengeCard(
                        challenge = userChallenge,
                        currentUserId = currentUserId,
                        onReset = {
                            onChallengeReset()
                            cardState = ChallengeCardState.SELECTING // 챌린지 변경 시 SELECTING 상태로
                        }
                    )
                } else {
                    // userChallenge가 로드되기 전까지 로딩 상태 표시
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}


@Composable
fun InitialChallengeCard(
    offsetX: Float,
    onSwipe: (Float) -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (offsetX == 0f) 5f else offsetX / 30f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .rotate(rotation)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { onSwipe(0f) },
                    onHorizontalDrag = { _, dragAmount ->
                        onSwipe(offsetX + dragAmount)
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0A0E27),
                            Color(0xFF1A4D2E),
                            Color(0xFF00FF88)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFF00FF88),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "이번주 챌린지에 참여하세요!",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "← 스와이프하여 챌린지 선택 →",
                    color = Color(0xFF00FF88),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ChallengeSelectionCard(
    challenges: List<Challenge>,
    onSelect: (Challenge) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                "어떤 챌린지에 참여하시겠어요?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (challenges.isEmpty()) {
                Text(
                    "챌린지를 불러오는 중...",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    challenges.take(3).forEach { challenge ->
                        ChallengeOption(
                            challenge = challenge,
                            onSelect = { onSelect(challenge) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeOption(
    challenge: Challenge,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (challenge.type) {
        com.route.readers.data.model.ChallengeType.CONSECUTIVE_READING_WITH_FRIEND -> Icons.Default.LocalFireDepartment
        com.route.readers.data.model.ChallengeType.DAILY_PAGES_READING -> Icons.Default.AutoStories
        else -> Icons.Default.EmojiEvents
    }
    
    Card(
        onClick = onSelect,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF1A4D2E),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                challenge.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun ActiveChallengeCard(
    challenge: Challenge,
    currentUserId: String,
    onReset: () -> Unit
) {
    val userProgress = challenge.progress[currentUserId] ?: 0
    val goal = challenge.goal.takeIf { it > 0 } ?: 1 // 목표가 0일 경우 1로 처리하여 0으로 나누기 방지
    val progress = if (goal > 0) userProgress.toFloat() / goal.toFloat() else 0f
    val daysRemaining = challenge.endDate?.let {
        val diff = it.time - System.currentTimeMillis()
        java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).toInt()
    } ?: 0
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A4D2E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "진행 중인 챌린지",
                    color = Color(0xFF00FF88),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${daysRemaining}일 남음",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    TextButton(
                        onClick = onReset,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF00FF88)
                        ),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("변경", fontSize = 12.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                challenge.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "내 진행률",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = Color(0xFF00FF88),
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${userProgress} / ${goal}일",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    color = Color(0xFF00FF88),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}