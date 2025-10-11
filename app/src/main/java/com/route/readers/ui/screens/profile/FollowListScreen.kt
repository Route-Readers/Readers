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
    val tabs = listOf("팔로워", "팔로잉", "사용자")
    var selectedTabIndex by remember { mutableIntStateOf(if (initialListType == "followers") 0 else 1) }
    val darkRedColor = Color(0xFFB71C1C)

    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val searchedUsers by viewModel.searchedUsers.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

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
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침"
                        )
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
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    val finalList = if (selectedTabIndex == 2 && searchQuery.isBlank()) {
                        val currentUser = searchedUsers.find { it.uid == currentUserId }
                        val otherUsers = searchedUsers.filter { it.uid != currentUserId }
                        listOfNotNull(currentUser) + otherUsers
                    } else {
                        searchedUsers
                    }

                    if (finalList.isEmpty()) {
                        val emptyMessage = if (searchQuery.isNotBlank()) {
                            "검색 결과가 없습니다."
                        } else {
                            if (selectedTabIndex == 2) "사용자가 없습니다." else "아직 ${tabs[selectedTabIndex]} 목록이 없습니다."
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = emptyMessage)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(finalList, key = { it.uid }) { user ->
                                val isFollowing = state.currentUserFollowingIds.contains(user.uid)
                                UserItem(
                                    user = user,
                                    isFollowing = isFollowing,
                                    selectedTab = selectedTabIndex,
                                    isMyProfile = userId == currentUserId,
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
    selectedTab: Int,
    isMyProfile: Boolean,
    onUserClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // 버튼의 텍스트와 색상을 결정하는 로직
    val (buttonText, buttonColors) = if (isMyProfile) {
        // 내 프로필의 팔로워/팔로잉 목록을 볼 때
        when (selectedTab) {
            0 -> { // '팔로워' 탭
                if (isFollowing) {
                    "언팔로우" to ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                } else {
                    // '팔로워' 탭에서 팔로우가 필요할 때 '맞팔로우' 텍스트 표시
                    "맞팔로우" to ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                }
            }
            1 -> { // '팔로잉' 탭
                "언팔로우" to ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            }
            else -> { // '사용자' 탭
                if (isFollowing) {
                    "언팔로우" to ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                } else {
                    "팔로우" to ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                }
            }
        }
    } else {
        // 다른 사람의 프로필 목록을 볼 때 (항상 '나' 기준)
        if (isFollowing) {
            "언팔로우" to ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        } else {
            "팔로우" to ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = user.nickname,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (user.uid != currentUserId) {
            Button(
                onClick = onFollowClick,
                shape = RoundedCornerShape(8.dp),
                colors = buttonColors
            ) {
                Text(buttonText)
            }
        }
    }
}
