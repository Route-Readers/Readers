package com.route.readers.ui.screens.profileimport

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.User // SimpleUser 대신 통합된 User 모델 사용
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// UI 상태 정의 (SimpleUser 대신 통합된 User 모델 사용)
sealed class FollowListUiState {
    object Loading : FollowListUiState()
    data class Success(val users: List<User>) : FollowListUiState()
    data class Error(val message: String) : FollowListUiState()
}

// ViewModel 정의
open class FollowViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    protected val _uiState = MutableStateFlow<FollowListUiState>(FollowListUiState.Loading)
    val uiState: StateFlow<FollowListUiState> = _uiState

    // ✨✨✨ 오류 수정된 핵심 로직 ✨✨✨
    open fun fetchFollowList(userId: String, listType: String) {
        _uiState.value = FollowListUiState.Loading

        viewModelScope.launch {
            try {
                // 1. users 컬렉션에서 대상 사용자의 문서를 가져옵니다.
                val userDocument = db.collection("users").document(userId).get().await()
                if (!userDocument.exists()) {
                    _uiState.value = FollowListUiState.Error("사용자를 찾을 수 없습니다.")
                    return@launch
                }

                // 2. 문서에서 팔로워/팔로잉 UID 리스트를 가져옵니다.
                val userIds = when (listType) {
                    "followers" -> userDocument.get("followers") as? List<String>
                    "following" -> userDocument.get("following") as? List<String>
                    else -> null
                }

                if (userIds.isNullOrEmpty()) {
                    _uiState.value = FollowListUiState.Success(emptyList())
                    return@launch
                }

                // 3. whereIn 10개 제한 문제를 해결하기 위해 쿼리를 분할 실행합니다.
                val userList = mutableListOf<User>()
                userIds.chunked(10).forEach { chunk ->
                    val usersSnapshot = db.collection("users")
                        .whereIn("uid", chunk)
                        .get()
                        .await()
                    userList.addAll(usersSnapshot.toObjects(User::class.java))
                }

                _uiState.value = FollowListUiState.Success(userList)

            } catch (e: Exception) {
                _uiState.value = FollowListUiState.Error("목록을 불러오는 데 실패했습니다: ${e.message}")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    userId: String, // ✨ 프로필 화면에서 어떤 유저의 목록을 볼지 ID를 받아와야 합니다.
    listType: String,
    onUserClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FollowViewModel = viewModel()
) {
    // 화면이 시작될 때, 전달받은 userId와 listType으로 목록을 요청합니다.
    LaunchedEffect(key1 = userId, key2 = listType) {
        viewModel.fetchFollowList(userId, if (listType == "팔로워") "followers" else "following")
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listType) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is FollowListUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is FollowListUiState.Success -> {
                    if (state.users.isEmpty()) {
                        Text(text = "아직 ${listType}가 없습니다.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.users) { user ->
                                // UserItem에 통합된 User 모델 전달
                                UserItem(
                                    user = user,
                                    onUserClick = { onUserClick(user.uid) }
                                )
                                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
                is FollowListUiState.Error -> {
                    Text(text = state.message)
                }
            }
        }
    }
}

@Composable
fun UserItem(user: User, onUserClick: () -> Unit) { // SimpleUser -> User
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = user.profileImageUrl,
            ),
            contentDescription = "${user.nickname}의 프로필 이미지",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = user.nickname,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


