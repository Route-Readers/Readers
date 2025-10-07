package com.route.readers.ui.screens.profile

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

data class SelectableItem(val name: String)

val readingGenres = listOf(
    SelectableItem("소설"), SelectableItem("로맨스"), SelectableItem("판타지"),
    SelectableItem("여행"), SelectableItem("자기계발"), SelectableItem("예술"),
    SelectableItem("스릴러"), SelectableItem("역사"), SelectableItem("과학"), SelectableItem("에세이")
)

val readingStyles = listOf(
    SelectableItem("빠르게 읽기"), SelectableItem("천천히 음미하기"), SelectableItem("다양한 분야 읽기"),
    SelectableItem("한 분야 깊게 파기"), SelectableItem("가볍게 즐기기"), SelectableItem("토론하며 읽기")
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProfileSetupScreen(
    onSetupComplete: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    var nickname by rememberSaveable { mutableStateOf("") }
    var selectedGenres by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var selectedStyles by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var imageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val imageUri = imageUriString?.toUri()

    val setupState by viewModel.setupState.collectAsState()

    var nicknameCheckMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isNicknameAvailable by rememberSaveable { mutableStateOf(false) }

    val primaryRed = Color(0xFFC0392B)

    val permissionState = rememberPermissionState(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> imageUriString = uri?.toString() }
    )

    LaunchedEffect(setupState) {
        when (val state = setupState) {
            is ProfileSetupState.Error -> {
                nicknameCheckMessage = state.message
                isNicknameAvailable = state.message == "사용 가능한 닉네임입니다."
            }

            is ProfileSetupState.Success -> {
                Toast.makeText(context, "프로필 설정이 완료되었습니다!", Toast.LENGTH_SHORT).show()
                onSetupComplete()
            }

            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable {
                            if (permissionState.status.isGranted) {
                                singlePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else if (permissionState.status.shouldShowRationale) {
                                Toast
                                    .makeText(context, "갤러리 접근을 위해 권한이 필요합니다.", Toast.LENGTH_SHORT)
                                    .show()
                                permissionState.launchPermissionRequest()
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "선택된 프로필 이미지",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "프로필 아이콘",
                            modifier = Modifier
                                .size(80.dp)
                                .padding(16.dp),
                            tint = primaryRed
                        )
                        Text(
                            text = "사진 추가",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("프로필 설정", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "다른 독서 애호가들과 소통할 때 사용할 프로필을 정해주세요.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Section("나만의 닉네임") {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = {
                            if (it.length <= 12) {
                                nickname = it
                                isNicknameAvailable = false
                                nicknameCheckMessage = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("닉네임 (2~12자)") },
                        singleLine = true,
                        trailingIcon = {
                            Text(
                                "${nickname.length}/12",
                                modifier = Modifier.padding(end = 8.dp),
                                color = Color.Gray
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    nicknameCheckMessage?.let {
                        val messageColor =
                            if (isNicknameAvailable) Color(0xFF27AE60) else MaterialTheme.colorScheme.error
                        Text(it, color = messageColor, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.checkNicknameAvailability(nickname) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = setupState !is ProfileSetupState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryRed.copy(alpha = 0.8f))
                    ) {
                        Text("중복 확인")
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Section("선호하는 장르 (다중 선택 가능)") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        readingGenres.forEach { item ->
                            SelectableChip(
                                text = item.name,
                                isSelected = selectedGenres.contains(item.name),
                                onClick = {
                                    selectedGenres =
                                        if (selectedGenres.contains(item.name)) selectedGenres - item.name else selectedGenres + item.name
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Section("독서 스타일 (다중 선택 가능)") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        readingStyles.forEach { item ->
                            SelectableChip(
                                text = item.name,
                                isSelected = selectedStyles.contains(item.name),
                                onClick = {
                                    selectedStyles =
                                        if (selectedStyles.contains(item.name)) selectedStyles - item.name else selectedStyles + item.name
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }

            item {
                Button(
                    onClick = {
                        if (selectedGenres.isEmpty()) {
                            Toast.makeText(context, "선호 장르를 1개 이상 선택해주세요.", Toast.LENGTH_SHORT).show()
                        } else if (imageUri == null) {
                            Toast.makeText(context, "프로필 사진을 선택해주세요.", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.createOrUpdateUserProfile(
                                nickname = nickname,
                                profileImageUri = imageUri,
                                genres = selectedGenres.toList(),
                                styles = selectedStyles.toList()
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryRed),
                    enabled = setupState !is ProfileSetupState.Loading && isNicknameAvailable
                ) {
                    if (setupState is ProfileSetupState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text("설정 완료하고 시작하기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

@Composable
fun SelectableChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Color(0xFFC0392B) else Color.LightGray
    val containerColor = if (isSelected) Color(0xFFC0392B).copy(alpha = 0.1f) else Color.Transparent

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(borderColor))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(text, color = if (isSelected) borderColor else Color.Black, fontSize = 14.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileSetupScreenPreview() {
    ProfileSetupScreen(onSetupComplete = {}, onNavigateBack = {})
}
