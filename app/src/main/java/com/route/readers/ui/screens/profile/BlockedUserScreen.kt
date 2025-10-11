package com.route.readers.ui.screens.profile // 1. 여기를 수정했습니다.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.data.model.User
// 2. 아래 두 줄의 import를 추가했습니다.
import com.route.readers.ui.screens.profile.BlockedUserUiState
import com.route.readers.ui.screens.profile.BlockedUserViewModel
import com.route.readers.ui.theme.DarkRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUserScreen(
    onNavigateBack: () -> Unit,
    viewModel: BlockedUserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("차단된 사용자 관리") },
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
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is BlockedUserUiState.Loading -> CircularProgressIndicator()
                is BlockedUserUiState.Error -> Text(state.message) // 'message'는 BlockedUserUiState.Error에 정의되어 있어야 합니다.
                is BlockedUserUiState.Success -> {
                    if (state.users.isEmpty()) {
                        Text("차단된 사용자가 없습니다.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.users, key = { it.uid }) { user ->
                                BlockedUserItem(
                                    user = user,
                                    onUnblockClick = { viewModel.unblockUser(user.uid) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedUserItem(
    user: User,
    onUnblockClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = user.profileImageUrl,
                contentDescription = "프로필 이미지",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Text(text = user.nickname, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onUnblockClick,
            colors = ButtonDefaults.buttonColors(containerColor = DarkRed)
        ) {
            Text("차단 해제")
        }
    }
}
