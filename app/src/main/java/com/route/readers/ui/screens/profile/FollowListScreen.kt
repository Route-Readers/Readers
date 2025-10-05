package com.route.readers.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class FollowListUiState {
    object Loading : FollowListUiState()
    data class Success(val users: List<User>, val currentUserFollowingIds: Set<String>) : FollowListUiState()
    data class Error(val message: String) : FollowListUiState()
}

class FollowViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    private val _uiState = MutableStateFlow<FollowListUiState>(FollowListUiState.Loading)
    val uiState: StateFlow<FollowListUiState> = _uiState

    fun fetchFollowList(userId: String, listType: String) {
        viewModelScope.launch {
            _uiState.value = FollowListUiState.Loading
            if (currentUserId == null) {
                _uiState.value = FollowListUiState.Error("로그인이 필요합니다.")
                return@launch
            }

            try {
                val currentUserDoc = db.collection("users").document(currentUserId).get().await()
                val currentUserFollowingIds = (currentUserDoc.get("following") as? List<String>)?.toSet() ?: emptySet()

                val targetUserDoc = db.collection("users").document(userId).get().await()
                if (!targetUserDoc.exists()) {
                    _uiState.value = FollowListUiState.Error("사용자를 찾을 수 없습니다.")
                    return@launch
                }

                val userIds = targetUserDoc.get(listType) as? List<String>

                if (userIds.isNullOrEmpty()) {
                    _uiState.value = FollowListUiState.Success(emptyList(), currentUserFollowingIds)
                    return@launch
                }

                val userList = mutableListOf<User>()
                userIds.chunked(10).forEach { chunk ->
                    val usersSnapshot = db.collection("users").whereIn("uid", chunk).get().await()
                    userList.addAll(usersSnapshot.toObjects(User::class.java))
                }
                _uiState.value = FollowListUiState.Success(userList, currentUserFollowingIds)
            } catch (e: Exception) {
                _uiState.value = FollowListUiState.Error("목록을 불러오는 데 실패했습니다: ${e.message}")
            }
        }
    }

    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch {
            if (currentUserId == null || currentUserId == targetUserId) return@launch

            val currentUserRef = db.collection("users").document(currentUserId)
            val targetUserRef = db.collection("users").document(targetUserId)
            val currentState = (uiState.value as? FollowListUiState.Success) ?: return@launch
            val isCurrentlyFollowing = currentState.currentUserFollowingIds.contains(targetUserId)

            db.runTransaction { transaction ->
                if (isCurrentlyFollowing) {
                    transaction.update(currentUserRef, "following", FieldValue.arrayRemove(targetUserId))
                    transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1))
                    transaction.update(targetUserRef, "followers", FieldValue.arrayRemove(currentUserId))
                    transaction.update(targetUserRef, "followerCount", FieldValue.increment(-1))
                } else {
                    transaction.update(currentUserRef, "following", FieldValue.arrayUnion(targetUserId))
                    transaction.update(currentUserRef, "followingCount", FieldValue.increment(1))
                    transaction.update(targetUserRef, "followers", FieldValue.arrayUnion(currentUserId))
                    transaction.update(targetUserRef, "followerCount", FieldValue.increment(1))
                }
            }.await()

            val updatedFollowingIds = if (isCurrentlyFollowing) {
                currentState.currentUserFollowingIds - targetUserId
            } else {
                currentState.currentUserFollowingIds + targetUserId
            }
            _uiState.value = currentState.copy(currentUserFollowingIds = updatedFollowingIds)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    userId: String,
    initialListType: String,
    nickname: String,
    onUserClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FollowViewModel = viewModel()
) {
    val tabs = listOf("팔로워", "팔로잉")
    var selectedTabIndex by remember { mutableIntStateOf(if (initialListType == "followers") 0 else 1) }
    var searchQuery by remember { mutableStateOf("") }
    val darkRedColor = Color(0xFFB71C1C)

    LaunchedEffect(key1 = userId, key2 = selectedTabIndex) {
        val listType = if (selectedTabIndex == 0) "followers" else "following"
        viewModel.fetchFollowList(userId, listType)
    }

    val uiState by viewModel.uiState.collectAsState()

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
                onValueChange = { searchQuery = it },
                placeholder = { Text("검색") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색 아이콘") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
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
                    val filteredUsers = state.users.filter { it.nickname.contains(searchQuery, ignoreCase = true) }
                    if (state.users.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "아직 ${tabs[selectedTabIndex]} 목록이 없습니다.")
                        }
                    } else if (filteredUsers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "검색 결과가 없습니다.")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredUsers, key = { it.uid }) { user ->
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
        Image(
            painter = rememberAsyncImagePainter(model = user.profileImageUrl),
            contentDescription = "${user.nickname}의 프로필 이미지",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.width(16.dp))
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
