package com.route.readers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.route.readers.data.model.Challenge
import com.route.readers.data.model.ChallengeType
import com.route.readers.ui.screens.challenge.ChallengeViewModel
import com.route.readers.ui.screens.community.CommunityViewModel
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun SharedChallengeCard(
    challenge: Challenge,
    currentUserId: String,
    consecutiveReadingDays: Int,
    communityViewModel: CommunityViewModel? = null,
    challengeViewModel: ChallengeViewModel? = null,
    onReset: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var actualDailyPages by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // Load actual daily reading data for DAILY_PAGES_READING challenges
    LaunchedEffect(challenge.type, currentUserId, communityViewModel) {
        if (challenge.type == ChallengeType.DAILY_PAGES_READING && communityViewModel != null) {
            val dailyPagesMap = mutableMapOf<String, Int>()
            // Get last 7 days of reading data
            for (i in 0..6) {
                val date = java.time.LocalDate.now().minusDays(i.toLong())
                val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                
                try {
                    val pagesRead = communityViewModel.getActualDailyPagesRead(dateStr)
                    dailyPagesMap[dateStr] = pagesRead
                } catch (e: Exception) {
                    // Fallback to challenge data
                    val fallbackPages = (challenge.dailyProgress[currentUserId]?.get(dateStr) as? Number)?.toInt() ?: 0
                    dailyPagesMap[dateStr] = fallbackPages
                }
            }
            actualDailyPages = dailyPagesMap
        } else if (challenge.type == ChallengeType.DAILY_PAGES_READING) {
            // Fallback when no communityViewModel
            val dailyPagesMap = mutableMapOf<String, Int>()
            for (i in 0..6) {
                val date = java.time.LocalDate.now().minusDays(i.toLong())
                val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val pagesRead = (challenge.dailyProgress[currentUserId]?.get(dateStr) as? Number)?.toInt() ?: 0
                dailyPagesMap[dateStr] = pagesRead
            }
            actualDailyPages = dailyPagesMap
        }
    }

    val (currentProgressValue, totalGoalValue) = when {
        communityViewModel != null -> {
            try {
                communityViewModel.getChallengeProgress(challenge)
            } catch (e: Exception) {
                getDefaultProgressWithActualData(challenge, currentUserId, consecutiveReadingDays, actualDailyPages)
            }
        }
        challengeViewModel != null -> {
            try {
                challengeViewModel.getChallengeProgress(challenge)
            } catch (e: Exception) {
                getDefaultProgressWithActualData(challenge, currentUserId, consecutiveReadingDays, actualDailyPages)
            }
        }
        else -> getDefaultProgressWithActualData(challenge, currentUserId, consecutiveReadingDays, actualDailyPages)
    }
    
    val progressUnit = "일"
    val overallProgressFraction = if (totalGoalValue > 0) currentProgressValue.toFloat() / totalGoalValue.toFloat() else 0f

    val daysRemaining = when {
        communityViewModel != null -> {
            try {
                communityViewModel.getDaysRemaining(challenge)
            } catch (e: Exception) {
                getDefaultDaysRemaining(challenge, currentUserId)
            }
        }
        challengeViewModel != null -> {
            try {
                challengeViewModel.getDaysRemaining(challenge)
            } catch (e: Exception) {
                getDefaultDaysRemaining(challenge, currentUserId)
            }
        }
        else -> getDefaultDaysRemaining(challenge, currentUserId)
    }

    Card(
        modifier = modifier
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
                    if (onReset != null) {
                        TextButton(
                            onClick = { onReset(challenge.id) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF00FF88)
                            ),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("변경", fontSize = 12.sp)
                        }
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
                progress = { overallProgressFraction },
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
                val progressText = if (challenge.type == ChallengeType.DAILY_PAGES_READING) {
                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val todayPages = actualDailyPages[todayStr] ?: 0
                    "${currentProgressValue}/${totalGoalValue}일 (오늘: ${todayPages}/${challenge.goal}페이지)"
                } else {
                    "${currentProgressValue}/${totalGoalValue}일"
                }

                Text(
                    text = progressText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${(overallProgressFraction * 100).toInt()}%",
                    color = Color(0xFF00FF88),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getDefaultProgressWithActualData(
    challenge: Challenge,
    currentUserId: String,
    consecutiveReadingDays: Int,
    actualDailyPages: Map<String, Int>
): Pair<Int, Int> {
    return when (challenge.type) {
        ChallengeType.DAILY_PAGES_READING -> {
            // Count how many days the user met the daily goal using actual reading data
            val dailyGoalMetDays = actualDailyPages.values.count { pagesRead ->
                pagesRead >= challenge.goal
            }
            Pair(dailyGoalMetDays, 7)
        }
        ChallengeType.CONSECUTIVE_READING, 
        ChallengeType.CONSECUTIVE_READING_WITH_FRIEND -> {
            Pair(consecutiveReadingDays, 7)
        }
        else -> {
            Pair(challenge.progress[currentUserId] ?: 0, challenge.goal.takeIf { it > 0 } ?: 1)
        }
    }
}

private fun getDefaultProgress(
    challenge: Challenge,
    currentUserId: String,
    consecutiveReadingDays: Int
): Pair<Int, Int> {
    return when (challenge.type) {
        ChallengeType.DAILY_PAGES_READING -> {
            val dailyGoalMetDays = challenge.dailyProgress[currentUserId]?.values?.count { pages ->
                (pages as? Number)?.toInt() ?: 0 >= challenge.goal
            } ?: 0
            Pair(dailyGoalMetDays, 7)
        }
        ChallengeType.CONSECUTIVE_READING, 
        ChallengeType.CONSECUTIVE_READING_WITH_FRIEND -> {
            Pair(consecutiveReadingDays, 7)
        }
        else -> {
            Pair(challenge.progress[currentUserId] ?: 0, challenge.goal.takeIf { it > 0 } ?: 1)
        }
    }
}

private fun getDefaultDaysRemaining(
    challenge: Challenge,
    currentUserId: String
): Int {
    return challenge.joinDate[currentUserId]?.let { joinDate ->
        val joinLocalDate = joinDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val todayLocalDate = LocalDate.now(ZoneId.systemDefault())
        val elapsedDays = ChronoUnit.DAYS.between(joinLocalDate, todayLocalDate).toInt()
        (7 - elapsedDays).coerceAtLeast(0)
    } ?: 0
}
