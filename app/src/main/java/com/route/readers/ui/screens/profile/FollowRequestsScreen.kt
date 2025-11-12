package com.route.readers.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.route.readers.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowRequestsScreen(
    onNavigateBack: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val followRequests by profileViewModel.followRequests.collectAsState()
    val isLoading by profileViewModel.isLoadingFollowRequests.collectAsState()

    LaunchedEffect(Unit) {
        profileViewModel.getFollowRequests()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("팔로우 요청") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (followRequests.isEmpty()) {
                Text("팔로우 요청이 없습니다.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(followRequests) { user ->
                        FollowRequestItem(
                            user = user,
                            onAccept = { profileViewModel.acceptFollowRequest(user.uid) },
                            onDecline = { profileViewModel.declineFollowRequest(user.uid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FollowRequestItem(
    user: User,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = user.nickname)
        Row {
            Button(onClick = onAccept) {
                Text("수락")
            }
            Button(onClick = onDecline) {
                Text("거절")
            }
        }
    }
}
