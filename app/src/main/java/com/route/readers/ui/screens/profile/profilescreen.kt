package com.route.readers.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UserProfile(
    val uid: String = "",
    val nickname: String = "",
    val profileImageUrl: String? = null,
    val readingGenres: List<String> = emptyList(),
    val level: Int = 1,
    val followerCount: Long = 0,
    val followingCount: Long = 0,
    val readBookCount: Int = 0 // 이 필드는 별도로 계산해야 할 수 있습니다.
)

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

open class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    protected val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    open fun fetchUserProfile(userId: String?) {
        val targetUserId = userId ?: auth.currentUser?.uid
        if (targetUserId == null) {
            _uiState.value = ProfileUiState.Error("사용자 정보를 찾을 수 없습니다.")
            return
        }

        _uiState.value = ProfileUiState.Loading
        db.collection("users").document(targetUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userProfile = document.toObject(UserProfile::class.java)
                    if (userProfile != null) {
                        _uiState.value = ProfileUiState.Success(userProfile)
                    } else {
                        _uiState.value = ProfileUiState.Error("프로필 변환에 실패했습니다.")
                    }
                } else {
                    _uiState.value = ProfileUiState.Error("프로필 데이터가 존재하지 않습니다.")
                }
            }
            .addOnFailureListener { e ->
                _uiState.value = ProfileUiState.Error("프로필을 불러오는 데 실패했습니다: ${e.message}")
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    // ▼▼▼ 1. onNavigateToSettings 파라미터를 onNavigateToLogin으로 변경 ▼▼▼
    onNavigateToLogin: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    LaunchedEffect(key1 = Unit) {
        viewModel.fetchUserProfile(null) // 자신의 프로필을 불러옴
    }

    val uiState by viewModel.uiState.collectAsState()
    // ▼▼▼ 2. 드롭다운 메뉴 표시 상태를 관리할 변수 추가 ▼▼▼
    var showMenu by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프로필") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                actions = {
                    // ▼▼▼ 3. 설정 아이콘과 드롭다운 메뉴 구현 ▼▼▼
                    Box {
                        // 설정 아이콘 버튼
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "설정")
                        }

                        // 드롭다운 메뉴
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // 로그아웃 버튼 아이템
                            DropdownMenuItem(
                                text = { Text("로그아웃") },
                                onClick = {
                                    // 메뉴 닫고, Firebase에서 로그아웃 후, 로그인 화면으로 이동
                                    showMenu = false
                                    FirebaseAuth.getInstance().signOut()
                                    onNavigateToLogin()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> CircularProgressIndicator()
                is ProfileUiState.Error -> Text(text = state.message)
                is ProfileUiState.Success -> {
                    ProfileContent(
                        userProfile = state.profile,
                        onFollowersClick = onFollowersClick,
                        onFollowingClick = onFollowingClick
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    userProfile: UserProfile,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    val primaryRed = Color(0xFFC0392B)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileImage(
                    imageUrl = userProfile.profileImageUrl,
                    nickname = userProfile.nickname,
                    size = 100.dp,
                    backgroundColor = primaryRed
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = userProfile.nickname,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Chip(
                    label = "레벨 ${userProfile.level}",
                    backgroundColor = Color(0xFFF5E1DF),
                    contentColor = primaryRed
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileInfoItem(
                        count = userProfile.followerCount.toString(),
                        label = "팔로워",
                        onClick = onFollowersClick
                    )
                    ProfileInfoItem(
                        count = userProfile.followingCount.toString(),
                        label = "팔로잉",
                        onClick = onFollowingClick
                    )
                    ProfileInfoItem(
                        count = userProfile.readBookCount.toString(),
                        label = "읽은 책"
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileImage(
    imageUrl: String?,
    nickname: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 100.dp,
    backgroundColor: Color
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(model = imageUrl),
                contentDescription = "프로필 이미지",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = nickname.firstOrNull()?.toString() ?: "",
                color = Color.White,
                fontSize = (size.value / 2.5).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun ProfileInfoItem(count: String, label: String, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = count,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFC0392B)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun Chip(label: String, backgroundColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = contentColor, fontSize = 12.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    class FakeProfileViewModel : ProfileViewModel() {
        override fun fetchUserProfile(userId: String?) {
            _uiState.value = ProfileUiState.Success(
                UserProfile(
                    uid = "previewUser",
                    nickname = "책벌레독서가",
                    level = 8,
                    followerCount = 234,
                    followingCount = 89,
                    readBookCount = 47
                )
            )
        }
    }

    MaterialTheme {
        ProfileScreen(
            onNavigateBack = {},
            // ▼▼▼ 4. Preview에서도 파라미터 이름 변경 ▼▼▼
            onNavigateToLogin = {},
            onFollowersClick = {},
            onFollowingClick = {},
            viewModel = FakeProfileViewModel()
        )
    }
}
