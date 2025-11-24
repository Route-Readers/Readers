package com.route.readers.ui.screens.community

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.delay
import android.util.Log

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
    currentUserId: String,
    isChallengesLoading: Boolean
) {
    var cardState by remember { mutableStateOf(ChallengeCardState.INITIAL) }
    var optimisticChallenge by remember { mutableStateOf<Challenge?>(null) }

    // This effect synchronizes the card's state with data from the ViewModel.
    LaunchedEffect(userChallenge, isChallengesLoading, optimisticChallenge) {
        Log.d("SwipeableChallengeCard", "LaunchedEffect triggered. userChallenge: $userChallenge, isChallengesLoading: $isChallengesLoading, optimisticChallenge: $optimisticChallenge")

        if (userChallenge != null || optimisticChallenge != null) {
            // If there's an active challenge (real or optimistic), show it.
            cardState = ChallengeCardState.ACTIVE
            if (userChallenge != null && optimisticChallenge != null) {
                // If real data has arrived, clear optimistic update.
                optimisticChallenge = null
            }
        } else if (!isChallengesLoading) {
            // If no active challenge (real or optimistic) and not loading, go to initial.
            cardState = ChallengeCardState.INITIAL
        }
        // If loading and no challenge, remain in current state (e.g., showing progress indicator).
    }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // While loading, if we don't have a challenge to show, display a progress indicator.
        if (isChallengesLoading && userChallenge == null && optimisticChallenge == null) {
            CircularProgressIndicator()
        } else {
            when (cardState) {
                ChallengeCardState.INITIAL -> {
                    InitialChallengeCard(
                        onSwipe = {
                            cardState = ChallengeCardState.SELECTING
                        }
                    )
                }
                ChallengeCardState.SELECTING -> {
                    ChallengeSelectionCard(
                        challenges = availableChallenges,
                        onSelect = { challenge ->
                            onChallengeSelected(challenge)
                            optimisticChallenge = challenge // Optimistically update the UI.
                            cardState = ChallengeCardState.ACTIVE
                        }
                    )
                }
                ChallengeCardState.ACTIVE -> {
                    val challengeToShow = userChallenge ?: optimisticChallenge

                    if (challengeToShow != null) {
                        ActiveChallengeCard(
                            challenge = challengeToShow,
                            currentUserId = currentUserId,
                            onReset = {
                                onChallengeReset()
                                optimisticChallenge = null
                                cardState = ChallengeCardState.SELECTING
                            }
                        )
                    } else {
                        // If for some reason we end up here with no challenge, go back to initial.
                        // This can happen if the last challenge is left/reset.
                        LaunchedEffect(Unit) {
                            cardState = ChallengeCardState.INITIAL
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun InitialChallengeCard(
    onSwipe: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val rotation by animateFloatAsState(targetValue = if (offsetX == 0f) 5f else offsetX / 30f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .rotate(rotation)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(offsetX) > 50f) {
                            onSwipe()
                        }
                        offsetX = 0f
                     },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX += dragAmount
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
                    "참여 가능한 챌린지를 불러오는 중...",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(challenges) { challenge ->
                        ChallengeOption(
                            challenge = challenge,
                            onSelect = { onSelect(challenge) },
                            modifier = Modifier.width(150.dp) // Give each card a fixed width for scrolling
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
        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).toInt()
        days.coerceAtLeast(0)
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
                    "${userProgress} / ${goal}${if (challenge.type == com.route.readers.data.model.ChallengeType.DAILY_PAGES_READING) "페이지" else "일"}",
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