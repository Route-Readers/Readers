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
import com.route.readers.data.model.Challenge

@Composable
fun ChallengeCard(
    challenge: Challenge,
    onJoinClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = challenge.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 예시: 첫 번째 참여자의 진행률 표시
            val currentUser = challenge.participants.firstOrNull()
            val userProgress = challenge.progress[currentUser] ?: 0
            val progress = if (challenge.goal > 0) userProgress.toFloat() / challenge.goal.toFloat() else 0f

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "${userProgress} / ${challenge.goal} (${(progress * 100).toInt()}%)")

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onJoinClick) {
                Text(text = "참여하기")
            }
        }
    }
}
