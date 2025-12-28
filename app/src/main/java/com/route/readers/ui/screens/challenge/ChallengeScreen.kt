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
import com.route.readers.ui.components.SharedChallengeCard
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
            viewModel.refreshAvailableChallenges()
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
    val allChallenges = (uiState.userChallenges + uiState.availableChallenges).distinctBy { it.id }

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
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when {
            uiState.isLoading && allChallenges.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            allChallenges.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "참여 가능한 챌린지가 없습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allChallenges, key = { it.id }) { challenge ->
                        val isJoined = challenge.participants.contains(viewModel.currentUserId)
                        if (isJoined) {
                            SharedChallengeCard(
                                challenge = challenge,
                                currentUserId = viewModel.currentUserId,
                                consecutiveReadingDays = uiState.consecutiveReadingDays,
                                challengeViewModel = viewModel,
                                onReset = { viewModel.leaveChallenge(challenge.id) }
                            )
                        } else {
                            ChallengeCard(
                                challenge = challenge,
                                onJoinClick = { viewModel.joinChallenge(challenge.id) },
                                onLeaveClick = { viewModel.leaveChallenge(challenge.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}


