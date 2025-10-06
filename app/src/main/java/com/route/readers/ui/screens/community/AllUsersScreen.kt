package com.route.readers.ui.screens.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*import androidx.compose.foundation.lazy.LazyColumn
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
// KTX import가 제거되었습니다.
import com.route.readers.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// --- ViewModel 및 UI 상태 정의 ---

sealed class AllUsersUiState {
    object Loading : AllUsersUiState()
    data class Success(val users: List<User>, val currentUserFollowingIds: Set<String>) : AllUsersUiState()
    data class Error(val message: String) : AllUsersUiState()
}

class AllUsersViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    private val _uiState = MutableStateFlow<AllUsersUiState>(AllUsersUiState.Loading)
    val uiState: StateFlow<AllUsersUiState> = _uiState

    init {
        fetchAllUsers()
    }

    private fun fetchAllUsers() {
        viewModelScope.launch {
            _uiState.value = AllUsersUiState.Loading
            if (currentUserId == null) {
                _uiState.value = AllUsersUiState.Error("로그인이 필요합니다.")
                return@launch
            }

            try {
                val currentUserDoc = db.collection("users").document(currentUserId).get().await()
                val followingIds = (currentUserDoc.get("following") as? List<String>)?.toSet() ?: emptySet()
                val allUsersSnapshot = db.collection("users").whereNotEqualTo("uid", currentUserId).get().await()

                // KTX를 사용하지 않는 방식으로 수정되었습니다.
                val allUsers = allUsersSnapshot.toObjects(User::class.java)

                _uiState.value = AllUsersUiState.Success(allUsers, followingIds)
            } catch (e: Exception) {
                _uiState.value = AllUsersUiState.Error("사용자 목록을 불러오는 데 실패했습니다.")
            }
        }
    }

    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch {
            if (currentUserId == null) return@launch

            val currentUserRef = db.collection("users").document(currentUserId)
            val targetUserRef = db.collection("users").document(targetUserId)

            val currentState = (uiState.value as? AllUsersUiState.Success) ?: return@launch
            val isCurrentlyFollowing = currentState.currentUserFollowingIds.contains(targetUserId)

            try {
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

                // UI 상태 즉시 업데이트
                val updatedFollowingIds = if (isCurrentlyFollowing) {
                    currentState.currentUserFollowingIds - targetUserId
                } else {
                    currentState.currentUserFollowingIds + targetUserId
                }
                _uiState.value = currentState.copy(currentUserFollowingIds = updatedFollowingIds)

            } catch (e: Exception) {
                // 에러 처리
            }
        }
    }
}


// --- 화면 Composable 함수 ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllUsersScreen(
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: AllUsersViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("모든 사용자", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("사용자 검색") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색 아이콘") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
            )

            when (val state = uiState) {
                is AllUsersUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AllUsersUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message)
                    }
                }
                is AllUsersUiState.Success -> {
                    val filteredUsers = state.users.filter {
                        it.nickname.contains(searchQuery, ignoreCase = true)
                    }
                    if (filteredUsers.isEmpty() && searchQuery.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("검색 결과가 없습니다.")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredUsers, key = { it.uid }) { user ->
                                UserItem(
                                    user = user,
                                    isFollowing = state.currentUserFollowingIds.contains(user.uid),
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
private fun UserItem(
    user: User,
    isFollowing: Boolean,
    onUserClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = user.profileImageUrl ?: ""),
            contentDescription = "${user.nickname} 프로필 이미지",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.nickname, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            user.bio?.let {
                if(it.isNotBlank()) {
                    Text(it, color = Color.Gray, fontSize = 14.sp, maxLines = 1)
                }
            }
        }
        if (user.uid != FirebaseAuth.getInstance().currentUser?.uid) {
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
