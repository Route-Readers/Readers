package com.route.readers.ui.screens.signup // 실제 프로젝트 패키지명으로 변경하세요

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // RTL 지원 아이콘
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import com.route.readers.ui.theme.ReadersTheme // 실제 앱 테마가 있다면 import 하세요

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateBack: () -> Unit = {} // 뒤로가기 액션 콜백
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") } // 비밀번호 확인 필드 추가
    var displayName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("회원가입") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors( // 예시: 상단바 색상 지정
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()), // 스크롤 가능하도록
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // 아이템 간 수직 간격
        ) {
            Text(
                text = "새 계정 만들기",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp) // 제목 하단 여백
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("이메일 주소") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("사용자 아이디") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("비밀번호 확인") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
                // TODO: 여기에 비밀번호 일치 여부 검증 로직 추가 (UI 상태 반영)
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("닉네임 (표시될 이름)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp)) // 버튼과의 간격

            Button(
                onClick = {
                    // 기능은 없으므로 현재는 로그만 출력
                    println("회원가입 버튼 클릭됨:")
                    println("Email: $email")
                    println("Username: $username")
                    println("Password: (hidden)")
                    println("Confirm Password: (hidden)")
                    println("DisplayName: $displayName")
                    // TODO: 실제 회원가입 로직 및 입력값 검증 로직 추가
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("계정 만들기", fontSize = 18.sp)
            }

            TextButton(
                onClick = {
                    // 기능은 없으므로 현재는 로그만 출력
                    println("이미 계정이 있으신가요? 로그인 클릭됨")
                    // TODO: 로그인 화면으로 이동하는 로직 추가 (onNavigateBack과 유사한 콜백 사용 가능)
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("이미 계정이 있으신가요? 로그인")
            }
        }
    }
}

// --- 미리보기 코드 ---
@Preview(showBackground = true, name = "SignUp Screen Light")
@Composable
fun SignUpScreenPreview() {
    // ReadersTheme { // 실제 앱 테마가 있다면 해당 테마로 감싸주세요.
    MaterialTheme { // 임시로 기본 MaterialTheme 사용
        Surface(modifier = Modifier.fillMaxSize()) {
            SignUpScreen()
        }
    }
    // }
}

@Preview(showBackground = true, name = "SignUp Screen Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SignUpScreenDarkPreview() {
    // ReadersTheme { // 실제 앱 테마가 있다면 해당 테마로 감싸주세요.
    MaterialTheme(colorScheme = darkColorScheme()) { // 임시로 기본 다크 MaterialTheme 사용
        Surface(modifier = Modifier.fillMaxSize()) {
            SignUpScreen()
        }
    }
    // }
}
