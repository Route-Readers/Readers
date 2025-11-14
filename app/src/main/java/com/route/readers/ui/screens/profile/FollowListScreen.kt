package com.route.readers.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.User
import com.route.readers.ui.theme.DarkRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    userId: String,
    initialListType: String,
    nickname: String,
    onUserClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToFollowRequests: () -> Unit,
    viewModel: FollowListViewModel = viewModel()
) {
    val tabs = listOf("팔로워", "팔로잉", "사용자")
    val initialIndex = when (initialListType) {
        "followers" -> 0
        "following" -> 1
        "all" -> 2
        else -> 1
    }
    var selectedTabIndex by remember { mutableIntStateOf(initialIndex) }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val searchedUsers by viewModel.searchedUsers.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val isMyProfile by viewModel.isMyProfile.collectAsState()

    LaunchedEffect(key1 = userId, key2 = selectedTabIndex) {
        viewModel.loadListForTab(userId, selectedTabIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nickname, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                },
                actions = {
                    if (isMyProfile) {
                        IconButton(onClick = onNavigateToFollowRequests) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "팔로우 요청"
                            )
                        }
                    }
                    IconButton(onClick = {
                        if (selectedTabIndex == 2) viewModel.loadAllUsers() else viewModel.refresh()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = MaterialTheme.colorScheme.primary
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
                                color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("닉네임으로 검색", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색 아이콘", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (uiState) {
                is FollowListUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is FollowListUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = (uiState as FollowListUiState.Error).message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is FollowListUiState.Success -> {
                    if (searchedUsers.isEmpty()) {
                        val emptyMessage = if (searchQuery.isNotBlank()) {
                            "검색 결과가 없습니다."
                        } else {
                            when (selectedTabIndex) {
                                0 -> "팔로워가 없습니다."
                                1 -> "팔로잉하는 사용자가 없습니다."
                                else -> "사용자가 없습니다."
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(searchedUsers, key = { it.uid }) { user ->
                                val isFollowing = (uiState as FollowListUiState.Success).currentUserFollowingIds.contains(user.uid)
                                val isRequestPending = (uiState as FollowListUiState.Success).pendingFollowRequests.contains(user.uid)
                                UserItem(
                                    user = user,
                                    isFollowing = isFollowing,
                                    isRequestPending = isRequestPending,
                                    selectedTabIndex = selectedTabIndex,
                                    onUserClick = { onUserClick(user.uid) },
                                    onFollowClick = { viewModel.toggleFollow(user.uid) }
                                )
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserItem(
    user: User,
    isFollowing: Boolean,
    isRequestPending: Boolean,
    selectedTabIndex: Int,
    onUserClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.nickname,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            user.bio?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (user.uid != currentUserId) {
            val buttonText = if (isRequestPending) {
                "요청됨" // Reverted to "요청됨"
            } else {
                when (selectedTabIndex) {
                    0 -> if (isFollowing) "팔로우 취소" else "맞팔로우"
                    1 -> "팔로우 취소"
                    else -> if (isFollowing) "팔로우 취소" else "팔로우"
                }
            }

            // usePrimaryColor should not apply if a request is pending, as the button will be disabled.
            val usePrimaryColor = !isFollowing && selectedTabIndex != 1 && !isRequestPending

            Button(
                onClick = onFollowClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (usePrimaryColor) DarkRed else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (usePrimaryColor) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                ),
                enabled = !isRequestPending // Reverted to disabled if request is pending
            ) {
                Text(buttonText, fontSize = 13.sp)
            }
        }
    }
}
