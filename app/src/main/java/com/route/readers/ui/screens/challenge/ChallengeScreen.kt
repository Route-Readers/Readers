package com.route.readers.ui.screens.challenge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.route.readers.data.model.Challenge
import com.route.readers.ui.components.ChallengeCard
import com.route.readers.ui.screens.bookclub.BookClubScreen

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun ChallengeScreen(
    viewModel: ChallengeViewModel = viewModel(),
    isActive: Boolean = false,
    onNavigateToChat: (String, String) -> Unit = { _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isActive) {
        if (isActive) {
            viewModel.refreshChallenges()
        }
    }

    // 화면이 활성화될 때마다 데이터 새로고침
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000) // 5초마다 새로고침
            if (isActive) {
                viewModel.refreshChallenges()
            }
        }
    }

    Column {
        // 탭 바
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("챌린지") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("북클럽") }
            )
        }

        // 탭 내용
        when (selectedTab) {
            0 -> ChallengeContent(
                uiState = uiState,
                viewModel = viewModel
            )
            1 -> BookClubScreen(
                onNavigateToChat = onNavigateToChat
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeContent(
    uiState: ChallengeUiState,
    viewModel: ChallengeViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("주간 챌린지") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (uiState.userChallenge == null && !uiState.isLoading) {
                FloatingActionButton(
                    onClick = { viewModel.showCreateDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "챌린지 만들기")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.userChallenge != null -> {
                ChallengeProgress(
                    challenge = uiState.userChallenge,
                    userId = viewModel.currentUserId,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                ChallengeSelection(
                    challenges = uiState.availableChallenges,
                    onJoinChallenge = { viewModel.joinChallenge(it) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    if (uiState.showCreateDialog) {
        CreateChallengeDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { title, description, goal ->
                viewModel.createChallenge(title, description, goal)
            }
        )
    }
}

@Composable
fun ChallengeSelection(
    challenges: List<Challenge>,
    onJoinChallenge: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (challenges.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "참여 가능한 챌린지가 없습니다.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(challenges) { challenge ->
                ChallengeCard(
                    challenge = challenge,
                    onJoinClick = { onJoinChallenge(challenge.id) }
                )
            }
        }
    }
}

@Composable
fun ChallengeProgress(
    challenge: Challenge,
    userId: String,
    modifier: Modifier = Modifier
) {
    val progress = challenge.progress[userId] ?: 0
    val progressFraction = if (challenge.goal > 0) progress.toFloat() / challenge.goal.toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(challenge.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(challenge.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.size(200.dp),
                strokeWidth = 16.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            Text(
                text = "${(progressFraction * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("현재 진행도: $progress / ${challenge.goal}", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("보상: ${challenge.reward}", style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChallengeDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 챌린지 만들기") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("챌린지 제목") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it.filter { char -> char.isDigit() } },
                    label = { Text("목표 (페이지 수)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val goalValue = goal.toIntOrNull() ?: 0
                    if (goalValue > 0) {
                        onCreate(title, description, goalValue)
                    }
                },
                enabled = title.isNotBlank() && (goal.toIntOrNull() ?: 0) > 0
            ) {
                Text("만들기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
