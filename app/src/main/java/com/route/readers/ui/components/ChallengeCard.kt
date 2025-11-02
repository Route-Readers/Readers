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
                val progress = if (challenge.goal > 0) userProgress.toFloat() / challenge.goal.toFloat() else 0f

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "${userProgress} / ${challenge.goal} (${(progress * 100).toInt()}%)")
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
