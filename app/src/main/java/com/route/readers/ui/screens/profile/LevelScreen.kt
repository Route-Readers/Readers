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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth

// 레벨 정보를 담는 데이터 클래스
data class LevelInfo(
    val currentLevel: Int,
    val currentPoints: Int,
    val pointsForNextLevel: Int,
    val totalPointsForCurrentLevel: Int // 현재 레벨이 되기 위해 필요한 총 경험치
)

// 총 경험치를 기반으로 레벨 정보를 계산하는 함수
fun calculateLevelInfo(totalPoints: Int): LevelInfo {
    val xpPerLevel = 100
    var currentLevel = 1
    var pointsNeededForNext = xpPerLevel
    var accumulatedPoints = 0

    while (totalPoints >= accumulatedPoints + pointsNeededForNext) {
        accumulatedPoints += pointsNeededForNext
        currentLevel++
        pointsNeededForNext += xpPerLevel
        if (currentLevel >= 10) break // 최대 레벨 10 제한
    }

    if (currentLevel >= 10) {
        val maxLevelPoints = accumulatedPoints + pointsNeededForNext
        return LevelInfo(
            currentLevel = 10,
            currentPoints = totalPoints.coerceAtMost(maxLevelPoints),
            pointsForNextLevel = maxLevelPoints,
            totalPointsForCurrentLevel = accumulatedPoints
        )
    }

    return LevelInfo(
        currentLevel = currentLevel,
        currentPoints = totalPoints - accumulatedPoints,
        pointsForNextLevel = pointsNeededForNext,
        totalPointsForCurrentLevel = accumulatedPoints
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(key1 = currentUserId) {
        if (currentUserId != null) {
            viewModel.fetchUserProfile(currentUserId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("레벨 및 업적") },
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            when (val state = uiState) {
                is ProfileUiState.Success -> {
                    // 사용자 데이터로부터 레벨 정보 계산
                    val levelInfo = calculateLevelInfo(state.user.totalPoints)
                    LevelInfoCard(
                        currentLevel = levelInfo.currentLevel,
                        currentPoints = levelInfo.currentPoints,
                        pointsForNextLevel = levelInfo.pointsForNextLevel
                    )
                    AchievementsSection(readBookCount = state.readBooks.size)
                }
                is ProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ProfileUiState.Error -> {
                    Text(text = "사용자 정보를 불러오는 데 실패했습니다: ${state.message}")
                }
            }
        }
    }
}

@Composable
fun LevelInfoCard(
    currentLevel: Int,
    currentPoints: Int,
    pointsForNextLevel: Int
) {
    val pointsToNext = (pointsForNextLevel - currentPoints).coerceAtLeast(0)
    val progress = if (pointsForNextLevel > 0) currentPoints.toFloat() / pointsForNextLevel.toFloat() else 0f
    val darkRed = Color(0xFFC62828)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        color = Color(0xFFF0E68C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (currentLevel >= 10) "Level $currentLevel (MAX)" else "Level $currentLevel",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    if (currentLevel < 10) {
                        Text(
                            text = "$pointsToNext points to next level",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            Box(contentAlignment = Alignment.Center) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = darkRed,
                    trackColor = darkRed.copy(alpha = 0.3f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LevelIndicator(level = currentLevel, isFilled = true)
                    if (currentLevel < 10) {
                        LevelIndicator(level = currentLevel + 1, isFilled = false)
                    }
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
fun AchievementsSection(readBookCount: Int) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("진행 중", "완료")
    val darkRed = Color(0xFFC62828)

    val allAchievements = listOf(
        Achievement("read_1", "책 1권 읽기", "1권의 책을 완독하세요", readBookCount, 1),
        Achievement("read_10", "책 10권 읽기", "10권의 책을 완독하세요", readBookCount, 10),
        Achievement("read_50", "책 50권 읽기", "50권의 책을 완독하세요", readBookCount, 50),
        Achievement("read_100", "책 100권 읽기", "100권의 책을 완독하세요", readBookCount, 100)
    )

    val inProgress = allAchievements.filter { !it.isCompleted }
    val completed = allAchievements.filter { it.isCompleted }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "업적",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = darkRed,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = darkRed
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTabIndex == index) darkRed else Color.Gray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val achievementsToShow = if (selectedTabIndex == 0) inProgress else completed
            if (achievementsToShow.isEmpty()) {
                Text(
                    text = if(selectedTabIndex == 0) "진행 중인 업적이 없습니다." else "완료된 업적이 없습니다.",
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 20.dp),
                    color = Color.Gray
                )
            } else {
                achievementsToShow.forEach { achievement ->
                    AchievementItem(achievement = achievement)
                }
            }
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            val currentProgressClamped = achievement.currentProgress.coerceAtMost(achievement.targetProgress)
            val progress = currentProgressClamped.toFloat() / achievement.targetProgress.toFloat()
            val progressPercentage = (progress * 100).toInt()

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                    color = Color.Black,
                    trackColor = Color.Black.copy(alpha = 0.1f)
                )
                Text(
                    text = "$progressPercentage%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "$currentProgressClamped / ${achievement.targetProgress}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun LevelIndicator(level: Int, isFilled: Boolean) {
    val darkRed = Color(0xFFC62828)
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (isFilled) darkRed else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (!isFilled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = darkRed.copy(alpha = 0.5f),
                    radius = size.minDimension / 2.0f
                )
            }
        }
        Text(
            text = level.toString(),
            color = if (isFilled) Color.White else Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun LevelScreenPreview() {
    // Preview에서는 ViewModel이 없으므로 직접 상태를 전달합니다.
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 예시: 550 경험치를 가진 사용자의 레벨 정보
        val levelInfo = calculateLevelInfo(550)
        LevelInfoCard(
            currentLevel = levelInfo.currentLevel,
            currentPoints = levelInfo.currentPoints,
            pointsForNextLevel = levelInfo.pointsForNextLevel
        )
        // 예시: 25권의 책을 읽은 사용자의 업적
        AchievementsSection(readBookCount = 25)
    }
}
