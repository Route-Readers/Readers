package com.route.readers.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("레벨 안내") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp), // 콘텐츠 주변에 패딩 추가
            contentAlignment = Alignment.TopCenter // 콘텐츠를 상단 중앙에 배치
        ) {
            LevelInfoCard(
                currentLevel = 2,
                currentPoints = 5200,
                pointsForNextLevel = 6000
            )
        }
    }
}

@Composable
fun LevelInfoCard(
    currentLevel: Int,
    currentPoints: Int,
    pointsForNextLevel: Int
) {
    val pointsToNext = pointsForNextLevel - currentPoints
    val progress = currentPoints.toFloat() / pointsForNextLevel.toFloat()
    val darkRed = Color(0xFFC62828) // 진한 빨간색 정의

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2C2C)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentLevel.toString(),
                        color = Color(0xFFF0E68C), // 밝은 노란색
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Level $currentLevel",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "$pointsToNext points to next level",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(contentAlignment = Alignment.Center) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = darkRed, // 진한 빨간색으로 변경
                    trackColor = darkRed.copy(alpha = 0.3f) // 반투명 진한 빨간색으로 변경
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LevelIndicator(level = currentLevel, isFilled = true)
                    LevelIndicator(level = currentLevel + 1, isFilled = false)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Points",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$currentPoints",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "/$pointsForNextLevel",
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun LevelIndicator(level: Int, isFilled: Boolean) {
    val darkRed = Color(0xFFC62828) // 진한 빨간색 정의
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (isFilled) darkRed else Color.Transparent), // 진한 빨간색으로 변경
        contentAlignment = Alignment.Center
    ) {
        if (!isFilled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = darkRed.copy(alpha = 0.5f), // 반투명 진한 빨간색으로 변경
                    radius = size.minDimension / 2.0f
                )
            }
        }
        Text(
            text = level.toString(),
            color = if (isFilled) Color.White else Color.Gray, // 채워진 원의 텍스트 색상을 흰색으로 변경
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}


@Preview(showBackground = true)
@Composable
fun LevelScreenPreview() {
    LevelScreen(onBack = {})
}

@Preview(showBackground = true)
@Composable
fun LevelInfoCardPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        LevelInfoCard(
            currentLevel = 2,
            currentPoints = 5200,
            pointsForNextLevel = 6000
        )
    }
}
