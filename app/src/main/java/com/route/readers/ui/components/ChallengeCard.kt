package com.route.readers.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.Challenge
import com.route.readers.data.model.ChallengeType
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ChallengeCard(
    challenge: Challenge,
    onJoinClick: () -> Unit,
    onLeaveClick: () -> Unit // Added onLeaveClick parameter
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isJoined = challenge.participants.contains(currentUserId)

    val daysRemaining = if (isJoined) {
        challenge.joinDate[currentUserId]?.let { joinDate ->
            val joinLocalDate = joinDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val todayLocalDate = LocalDate.now(ZoneId.systemDefault())
            val elapsedDays = ChronoUnit.DAYS.between(joinLocalDate, todayLocalDate).toInt()
            (7 - elapsedDays).coerceAtLeast(0)
        } ?: 0 // 참여했지만 joinDate가 없는 경우 0일로 표시
    } else {
        // 아직 참여하지 않은 챌린지의 남은 기간 계산 (endDate 기준)
        challenge.endDate?.let {
            val diff = it.time - System.currentTimeMillis()
            if (diff > 0) TimeUnit.MILLISECONDS.toDays(diff).toInt() else 0
        } ?: 7 // endDate가 없으면 기본 7일로 표시
    }

    val daysText = if (daysRemaining <= 0) "종료됨" else "${daysRemaining}일 남음"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${challenge.participants.size}명 참여중",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isJoined) {
                val userProgress = (challenge.progress[currentUserId] as? Number)?.toInt() ?: 0 // This is total pages read for the challenge.

                val (currentProgressValue, totalGoalValue, progressUnit) = when (challenge.type) {
                    ChallengeType.DAILY_PAGES_READING -> {
                        // Calculate how many days the daily goal has been met
                        val dailyGoalMetDays = challenge.dailyProgress[currentUserId]?.values?.count { pages ->
                            (pages as? Number)?.toInt() ?: 0 >= challenge.goal
                        } ?: 0
                        Triple(dailyGoalMetDays, 7, "일") // X/7 일 완료
                    }
                    ChallengeType.CONSECUTIVE_READING, ChallengeType.CONSECUTIVE_READING_WITH_FRIEND -> {
                        Triple(userProgress, 7, "일") // X/7 일 완료 (consecutive days)
                    }
                    else -> {
                        Triple(userProgress, challenge.goal, "일") // Default for other types (e.g., total pages read for a book challenge)
                    }
                }
                val overallProgressFraction = if (totalGoalValue > 0) currentProgressValue.toFloat() / totalGoalValue.toFloat() else 0f

                LinearProgressIndicator(
                progress = { overallProgressFraction },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 챌린지 타입에 따른 진행도 표시
                val progressText = if (challenge.type == ChallengeType.DAILY_PAGES_READING) {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val todayPages = (challenge.dailyProgress[currentUserId]?.get(todayStr) as? Number)?.toInt() ?: 0
                    "${currentProgressValue}/${totalGoalValue}${progressUnit} 완료 (오늘: ${todayPages}/${challenge.goal}페이지) (${(overallProgressFraction * 100).toInt()}%)"
                } else {
                    "${currentProgressValue}/${totalGoalValue}${progressUnit} 완료 (${(overallProgressFraction * 100).toInt()}%)"
                }
                Text(text = progressText)
                Spacer(modifier = Modifier.height(8.dp)) // Add space before button
                Button(
                    onClick = onLeaveClick, // Call onLeaveClick when joined
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "챌린지 포기하기")
                }
            } else {
                // 참여하지 않은 경우에도 DAILY_PAGES_READING 타입이면 오늘 진행률을 보여줌
                if (challenge.type == ChallengeType.DAILY_PAGES_READING) {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val todayPages = (challenge.dailyProgress[currentUserId]?.get(todayStr) as? Number)?.toInt() ?: 0
                    val dailyGoal = challenge.goal
                    val dailyProgressFraction = if (dailyGoal > 0) todayPages.toFloat() / dailyGoal.toFloat() else 0f

                    LinearProgressIndicator(
                        progress = { dailyProgressFraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "오늘: ${todayPages}/${dailyGoal}페이지 (${(dailyProgressFraction * 100).toInt()}%)")
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = onJoinClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "참여하기")
                }
            }
        }
    }
}