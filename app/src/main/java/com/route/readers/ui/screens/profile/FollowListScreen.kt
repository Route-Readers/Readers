package com.route.readers.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SimpleUser(
    val uid: String = "",
    val nickname: String = "",
    val profileImageUrl: String? = null
)

sealed class FollowListUiState {
    object Loading : FollowListUiState()
    data class Success(val users: List<SimpleUser>) : FollowListUiState()
    data class Error(val message: String) : FollowListUiState()
}

open class FollowViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    protected val _uiState = MutableStateFlow<FollowListUiState>(FollowListUiState.Loading)
    val uiState: StateFlow<FollowListUiState> = _uiState

    open fun fetchFollowList(userId: String, listType: String) {
        _uiState.value = FollowListUiState.Loading

        db.collection("users").document(userId).collection(listType)
            .get()
            .addOnSuccessListener { documents ->
                val userIds = documents.map { it.id }

                if (userIds.isEmpty()) {
                    _uiState.value = FollowListUiState.Success(emptyList())
                    return@addOnSuccessListener
                }

                db.collection("users").whereIn("uid", userIds)
                    .get()
                    .addOnSuccessListener { userDocs ->
                        val userList = userDocs.toObjects(SimpleUser::class.java)
                        _uiState.value = FollowListUiState.Success(userList)
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = FollowListUiState.Error("사용자 정보를 불러오는데 실패했습니다: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                _uiState.value = FollowListUiState.Error("목록을 불러오는데 실패했습니다: ${e.message}")
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    listType: String,
    onUserClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FollowViewModel = viewModel()
) {
    LaunchedEffect(key1 = Unit) {
        viewModel.fetchFollowList("someUserId", if (listType == "팔로워") "followers" else "following")
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
fun UserItem(user: SimpleUser, onUserClick: () -> Unit) {
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
        Column {
            Text(
                text = user.nickname,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true, name = "User Item")
@Composable
fun UserItemPreview() {
    val sampleUser = SimpleUser(uid = "123", nickname = "책읽는고양이")
    UserItem(user = sampleUser, onUserClick = {})
}

@Preview(showBackground = true, name = "Follow List (Full)")
@Composable
fun FollowListScreenPreview() {
    class FakeFollowViewModel : FollowViewModel() {
        override fun fetchFollowList(userId: String, listType: String) {
            val fakeUsers = listOf(
                SimpleUser("1", "여행하는 독서가", null),
                SimpleUser("2", "코드읽는 개발자", null),
                SimpleUser("3", "별헤는 밤", null)
            )
            _uiState.value = FollowListUiState.Success(fakeUsers)
        }
    }

    MaterialTheme {
        val fakeVm = FakeFollowViewModel().apply { fetchFollowList("", "") }
        FollowListScreen(
            listType = "팔로워",
            onUserClick = {},
            onNavigateBack = {},
            viewModel = fakeVm
        )
    }
}

@Preview(showBackground = true, name = "Follow List (Empty)")
@Composable
fun FollowListScreenEmptyPreview() {
    class FakeFollowViewModel : FollowViewModel() {
        override fun fetchFollowList(userId: String, listType: String) {
            _uiState.value = FollowListUiState.Success(emptyList())
        }
    }

    MaterialTheme {
        val fakeVm = FakeFollowViewModel().apply { fetchFollowList("", "") }
        FollowListScreen(
            listType = "팔로잉",
            onUserClick = {},
            onNavigateBack = {},
            viewModel = fakeVm
        )
    }
}
