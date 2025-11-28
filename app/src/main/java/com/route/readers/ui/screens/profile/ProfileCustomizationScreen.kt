package com.route.readers.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.route.readers.R
import com.route.readers.ui.theme.DarkRed
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Activity
import androidx.compose.material.icons.filled.CheckCircle

data class CharacterOption(
    val id: String,
    val drawableRes: Int,
    val name: String
)

data class BackgroundColorOption(
    val id: String,
    val color: Color,
    val name: String
)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCustomizationScreen(
    currentCharacter: String?,
    currentBackgroundColor: String?,
    nickname: String,
    onSave: (character: String?, backgroundColor: String?, phoneNumber: String?) -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel() // Inject ViewModel
) {
    var selectedCharacter by remember { mutableStateOf(currentCharacter) }
    var selectedBackgroundColor by remember { mutableStateOf(currentBackgroundColor ?: "#D32F2F") }
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val verificationState by viewModel.phoneVerificationState.collectAsState()

    // Restore verification state and pre-fill phone number if verified
    LaunchedEffect(Unit) {
        viewModel.restoreVerificationState()
        val savedNumber = viewModel.getVerifiedPhoneNumber()
        if (savedNumber.isNotEmpty()) {
            phoneNumber = savedNumber
        }
    }

    val characters = listOf(
        CharacterOption("lion", R.drawable.lion, "사자"),
        CharacterOption("penguin", R.drawable.penguin, "펭귄"),
        CharacterOption("redpanda", R.drawable.redpanda, "레서판다"),
        CharacterOption("squirrel", R.drawable.squirrel, "다람쥐"),
        CharacterOption("cat", R.drawable.cat, "고양이"),
        CharacterOption("dog", R.drawable.dog, "강아지"),
        CharacterOption("fox", R.drawable.fox, "여우"),
        CharacterOption("rabbit", R.drawable.rabbit, "토끼")
    )

    val backgroundColors = listOf(
        BackgroundColorOption("#D32F2F", Color(0xFFD32F2F), "빨강"),
        BackgroundColorOption("#1976D2", Color(0xFF1976D2), "파랑"),
        BackgroundColorOption("#388E3C", Color(0xFF388E3C), "초록"),
        BackgroundColorOption("#F57C00", Color(0xFFF57C00), "주황"),
        BackgroundColorOption("#7B1FA2", Color(0xFF7B1FA2), "보라"),
        BackgroundColorOption("#5D4037", Color(0xFF5D4037), "갈색"),
        BackgroundColorOption("#004D40", Color(0xFF004D40), "청록"),
        BackgroundColorOption("#F48FB1", Color(0xFFF48FB1), "분홍"),
        BackgroundColorOption("#303F9F", Color(0xFF303F9F), "남색"),
        BackgroundColorOption("#BDBDBD", Color(0xFFBDBDBD), "회색")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프로필 꾸미기") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    val isVerified = verificationState is PhoneVerificationState.Verified
                    // Allow save if verified OR if phone number is empty (user didn't try to set one)
                    val canSave = isVerified || phoneNumber.isEmpty()
                    
                    TextButton(
                        onClick = { 
                            if (canSave) {
                                onSave(selectedCharacter, selectedBackgroundColor, if(phoneNumber.isNotEmpty()) phoneNumber else null) 
                            } else {
                                // Optional: Show toast or error
                            }
                        },
                        enabled = canSave // Disable save if phone entered but not verified
                    ) {
                        Text("저장", color = if(canSave) DarkRed else Color.Gray)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("미리보기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    ProfilePreview(
                        character = selectedCharacter,
                        backgroundColor = selectedBackgroundColor,
                        nickname = nickname
                    )
                }
            }

            // Character Selection
            Text("캐릭터 선택", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(264.dp), // Give a fixed height to the grid
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                // Default option (no character)
                item {
                    CharacterItem(
                        isSelected = selectedCharacter == null,
                        onClick = { selectedCharacter = null }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.Gray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("기본", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                items(characters) { character ->
                    CharacterItem(
                        isSelected = selectedCharacter == character.id,
                        onClick = { selectedCharacter = character.id }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(character.drawableRes)
                                .build(),
                            contentDescription = character.name,
                            modifier = Modifier.size(60.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Phone Number Registration
            Text("전화번호 등록 (친구 찾기용)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "숫자만 입력해주세요. (예: 01012345678)",
                fontSize = 12.sp,
                color = Color.Gray
            )

            val isVerified = verificationState is PhoneVerificationState.Verified
            val isCodeSent = verificationState is PhoneVerificationState.CodeSent
            val isLoading = verificationState is PhoneVerificationState.Loading
            val isError = verificationState is PhoneVerificationState.Error

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { input ->
                        if (!isVerified && !isCodeSent) {
                            val filtered = input.filter { it.isDigit() }
                            if (filtered.length <= 11) {
                                phoneNumber = filtered
                            }
                        }
                    },
                    label = { Text("전화번호 (하이픈 없이)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    enabled = !isVerified && !isCodeSent,
                    isError = phoneNumber.isNotEmpty() && (phoneNumber.length < 10 || !phoneNumber.startsWith("010")),
                )
                
                Spacer(modifier = Modifier.width(8.dp))

                if (!isVerified) {
                    Button(
                        onClick = {
                            if (context is Activity) {
                                viewModel.sendVerificationCode(context, phoneNumber)
                            }
                        },
                        enabled = !isLoading && !isCodeSent && phoneNumber.length >= 10 && phoneNumber.startsWith("010"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                         if (isLoading) {
                             CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                         } else {
                             Text(if (isCodeSent) "전송됨" else "인증요청")
                         }
                    }
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF4CAF50))
                }
            }
            
            if (isCodeSent) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { if(it.length <= 6) verificationCode = it },
                        label = { Text("인증번호 6자리") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.verifyPhoneNumberWithCode(verificationCode) },
                        enabled = !isLoading && verificationCode.length == 6,
                         shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                             CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                         } else {
                             Text("확인")
                         }
                    }
                }
            }

            if (isError) {
                Text(
                    text = (verificationState as PhoneVerificationState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            
            if (isVerified) {
                 Text(
                    text = "인증이 완료되었습니다.",
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Background Color Selection
            Text("배경색 선택", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(backgroundColors) { colorOption ->
                    ColorItem(
                        color = colorOption.color,
                        isSelected = selectedBackgroundColor == colorOption.id,
                        onClick = { selectedBackgroundColor = colorOption.id }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfilePreview(
    character: String?,
    backgroundColor: String,
    nickname: String
) {
    val bgColor = try {
        Color(android.graphics.Color.parseColor(backgroundColor))
    } catch (e: Exception) {
        DarkRed
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (character != null) {
            val drawableRes = when (character) {
                "lion" -> R.drawable.lion
                "penguin" -> R.drawable.penguin
                "redpanda" -> R.drawable.redpanda
                "squirrel" -> R.drawable.squirrel
                "cat" -> R.drawable.cat
                "dog" -> R.drawable.dog
                "fox" -> R.drawable.fox
                "rabbit" -> R.drawable.rabbit
                else -> null
            }

            drawableRes?.let {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(it)
                        .build(),
                    contentDescription = "캐릭터",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            Text(
                text = nickname.firstOrNull()?.toString() ?: "",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CharacterItem(
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) DarkRed.copy(alpha = 0.1f) else Color.Transparent)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) DarkRed else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        content()

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(DarkRed)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "선택됨",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun ColorItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.Black else Color.Gray.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "선택됨",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
