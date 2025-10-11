package com.route.readers.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    userId: String,
    initialListType: String,
    nickname: String,
    onUserClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FollowListViewModel = viewModel()
) {
    // ▼▼▼ "사용자" 탭 추가 ▼▼▼
    val tabs = listOf("팔로워", "팔로잉", "사용자")
    var selectedTabIndex by remember { mutableIntStateOf(if (initialListType == "followers") 0 else 1) }
    val darkRedColor = Color(0xFFB71C1C)

    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val searchedUsers by viewModel.searchedUsers.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val isAllUsersTab = selectedTabIndex == 2 // "사용자" 탭인지 확인하는 변수

    // ▼▼▼ ViewModel의 새로운 통합 함수 호출 ▼▼▼
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
                    // ▼▼▼ "사용자" 탭이 아닐 때만 새로고침 버튼 표시 ▼▼▼
                    if (!isAllUsersTab) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "새로고침"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = darkRedColor,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = darkRedColor
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
                                color = if (selectedTabIndex == index) darkRedColor else Color.Gray,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // ▼▼▼ "사용자" 탭이 아닐 때만 검색창 표시 ▼▼▼
            if (!isAllUsersTab) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("검색") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색 아이콘") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
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
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is FollowListUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is FollowListUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message)
                    }
                }
                is FollowListUiState.Success -> {
                    val listToDisplay = if (isAllUsersTab) state.allUsers else searchedUsers

                    if (listToDisplay.isEmpty()) {
                        val emptyMessage = if (isAllUsersTab) {
                            "사용자가 없습니다."
                        } else if (searchQuery.isNotBlank()) {
                            "검색 결과가 없습니다."
                        } else {
                            "아직 ${tabs[selectedTabIndex]} 목록이 없습니다."
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = emptyMessage)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(listToDisplay, key = { it.uid }) { user ->
                                val isFollowing = state.currentUserFollowingIds.contains(user.uid)
                                UserItem(
                                    user = user,
                                    isFollowing = isFollowing,
                                    onUserClick = { onUserClick(user.uid) },
                                    onFollowClick = { viewModel.toggleFollow(user.uid) }
                                )
                                Divider(color = Color.LightGray.copy(alpha = 0.5f))
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
                fontSize = 16.sp
            )
            user.bio?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = it,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (user.uid != currentUserId) {
            Button(
                onClick = onFollowClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(if (isFollowing) "팔로잉" else "팔로우")
            }
        }
    }
}
