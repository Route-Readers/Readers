package com.route.readers.ui.screens.community

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.route.readers.data.model.Challenge
import kotlinx.coroutines.delay
import android.util.Log
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.route.readers.ui.theme.PrimaryRed
import kotlin.math.abs

// Premium Colors
private val PremiumGold = Color(0xFFD4AF37)
private val PremiumBurgundy = PrimaryRed // 0xFF800020
private val PremiumDarkBurgundy = Color(0xFF500014)
private val PremiumSurface = Color(0xFFF9F9F9)
private val PremiumText = Color(0xFF333333)

enum class ChallengeCardState {
    INITIAL,      // 초기 "참여하세요" 카드
    SELECTING,    // 챌린지 선택 화면
    ACTIVE        // 선택된 챌린지 진행 중
}

@Composable
fun SwipeableChallengeCard(
    userChallenges: List<Challenge>,
    availableChallenges: List<Challenge>,
    onChallengeSelected: (Challenge) -> Unit,
    onChallengeReset: (String) -> Unit,
    currentUserId: String,
    isChallengesLoading: Boolean,
    consecutiveReadingDays: Int,
    communityViewModel: CommunityViewModel? = null,
    challengeViewModel: com.route.readers.ui.screens.challenge.ChallengeViewModel? = null,
    startInSelectionMode: Boolean = false
) {
    // UI state for selection, using rememberSaveable to survive tab switching
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isChallengesLoading && userChallenges.isEmpty()) {
            CircularProgressIndicator(color = PremiumBurgundy)
        } else if (userChallenges.isNotEmpty()) {
            // User has active challenges - always show them
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(userChallenges, key = { it.id }) { challenge ->
                    ActiveChallengeCard(
                        challenge = challenge,
                        currentUserId = currentUserId,
                        consecutiveReadingDays = consecutiveReadingDays,
                        onReset = {
                            onChallengeReset(challenge.id)
                            isSelectionMode = false
                        }
                    )
                }
            }
        } else if (isSelectionMode || startInSelectionMode) {
            // No active challenges and in selection mode
            ChallengeSelectionCard(
                challenges = availableChallenges,
                onSelect = { challenge ->
                    onChallengeSelected(challenge)
                    isSelectionMode = false
                }
            )
        } else {
            // Initial invitation card
            InitialChallengeCard(
                onSwipe = {
                    isSelectionMode = true
                }
            )
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
                        if (abs(offsetX) > 50f) {
                            onSwipe()
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX += dragAmount
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            PremiumBurgundy,
                            PremiumDarkBurgundy
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
                    tint = PremiumGold,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "이번주 챌린지 도전하기",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "← 스와이프하여 멤버십 혜택 확인 →",
                    color = PremiumGold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                "챌린지 선택",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PremiumText,
                fontFamily = FontFamily.Serif
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(challenges, key = { it.id }) { challenge ->
                        ChallengeOption(
                            challenge = challenge,
                            onSelect = { onSelect(challenge) },
                            modifier = Modifier.width(160.dp) 
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
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = PremiumSurface
        ),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
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
                tint = PremiumBurgundy,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                challenge.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                maxLines = 2,
                color = PremiumText,
                fontFamily = FontFamily.Serif
            )
        }
    }
}

@Composable
fun ActiveChallengeCard(
    challenge: Challenge,
    currentUserId: String,
    consecutiveReadingDays: Int,
    onReset: () -> Unit
) {
    val (currentProgressValue, totalGoalValue, progressUnit) = when (challenge.type) {
        com.route.readers.data.model.ChallengeType.DAILY_PAGES_READING -> {
            // Calculate how many days the daily goal has been met
            val dailyGoalMetDays = challenge.dailyProgress[currentUserId]?.values?.count { pages ->
                (pages as? Number)?.toInt() ?: 0 >= challenge.goal
            } ?: 0
            Triple(dailyGoalMetDays, 7, "일") // X/7 일
        }
        com.route.readers.data.model.ChallengeType.CONSECUTIVE_READING, com.route.readers.data.model.ChallengeType.CONSECUTIVE_READING_WITH_FRIEND -> {
            // Use actual consecutive reading days from attendance
            Triple(consecutiveReadingDays, 7, "일")
        }
        else -> {
            Triple(challenge.progress[currentUserId] ?: 0, challenge.goal.takeIf { it > 0 } ?: 1, "일")
        }
    }
    val overallProgressFraction = if (totalGoalValue > 0) currentProgressValue.toFloat() / totalGoalValue.toFloat() else 0f

    val daysRemaining = challenge.joinDate[currentUserId]?.let { joinDate ->
        val joinLocalDate = joinDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val todayLocalDate = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
        val elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(joinLocalDate, todayLocalDate).toInt()
        (7 - elapsedDays).coerceAtLeast(0)
    } ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = PremiumBurgundy
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.5f))
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
                    color = PremiumGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${daysRemaining}일 남음",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    TextButton(
                        onClick = onReset,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = PremiumGold
                        ),
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("변경", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                challenge.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                 Text(
                    "내 진행률",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    "${(overallProgressFraction * 100).toInt()}%",
                    color = PremiumGold,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { overallProgressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PremiumGold,
                trackColor = Color.Black.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(8.dp))
             
             // Additional info if needed
        }
    }
}
