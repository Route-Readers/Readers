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
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ChallengeCard(
    challenge: Challenge,
    onJoinClick: () -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isJoined = challenge.participants.contains(currentUserId)

    val daysRemaining = challenge.endDate?.let {
        val diff = it.time - System.currentTimeMillis()
        TimeUnit.MILLISECONDS.toDays(diff).toInt()
    } ?: 0

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
                    text = "${daysRemaining}일 남음",
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
                val userProgress = challenge.progress[currentUserId] ?: 0
                val progressGoal = when (challenge.type) {
                    ChallengeType.DAILY_PAGES_READING -> challenge.goal
                    ChallengeType.CONSECUTIVE_READING, ChallengeType.CONSECUTIVE_READING_WITH_FRIEND -> 7 // Assuming these are weekly challenges
                    else -> challenge.goal // Default for other types
                }
                val progress = if (progressGoal > 0) userProgress.toFloat() / progressGoal.toFloat() else 0f

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 챌린지 타입에 따른 진행도 표시
                when (challenge.type) {
                    ChallengeType.DAILY_PAGES_READING -> {
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        Text(text = "${userProgress} / ${challenge.goal} 페이지 완료 (${(progress * 100).toInt()}%)")
                        Text(
                            text = "오늘 읽은 페이지: ${todayPages}/${challenge.goal}페이지",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (todayPages >= challenge.goal) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Text(text = "${userProgress} / ${progressGoal}일 완료 (${(progress * 100).toInt()}%)")
                    }
                }
            } else {
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