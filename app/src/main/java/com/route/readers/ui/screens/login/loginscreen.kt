package com.route.readers.ui.screens.login // 실제 패키지명으로 수정

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // LoginPageTheme을 제거했다면 이 Color는 사용되지 않을 수 있음import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import com.route.readers.ui.theme.ReadersTheme // 만약 프로젝트 테마가 있다면 import

@Composable
fun LoginScreen(
    // 필요하다면 네비게이션을 위한 콜백 함수 등을 파라미터로 추가할 수 있습니다.
    // 예: onLoginSuccess: () -> Unit, onNavigateToSignUp: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberId by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 32.dp)
                .height(60.dp)
        ) {
            Text(
                text = "[앱 로고]",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            // TODO: 실제 로고 이미지로 교체 (예: Image Composable 사용)
            // Image(
            //     painter = painterResource(id = R.drawable.your_logo_resource_id),
            //     contentDescription = "앱 로고",
            // )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "로그인",
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("이메일을 입력하세요") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호를 입력하세요") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Checkbox(
                    checked = rememberId,
                    onCheckedChange = { rememberId = it }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "아이디 저장",
                    modifier = Modifier.clickable { rememberId = !rememberId }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // TODO: 로그인 로직 구현
                    Log.d("LoginScreen", "Email: $email, Password: $password, RememberID: $rememberId")
                    // onLoginSuccess() // 예시: 로그인 성공 시 호출
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("로그인", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("계정이 없으신가요? ")
                TextButton(
                    onClick = {
                        // TODO: 회원가입 화면으로 이동
                        Log.d("LoginScreen", "회원가입 클릭")
                        // onNavigateToSignUp() // 예시: 회원가입 화면 이동 시 호출
                    }
                ) {
                    Text("회원가입")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Login Screen Preview")
@Composable
fun DefaultLoginScreenPreview() { // Preview 함수 이름 변경 (선택 사항)
    // ReadersTheme { // 프로젝트의 기본 테마로 감싸주세요
    Surface(modifier = Modifier.fillMaxSize()) { // Surface 추가하면 배경색 등이 더 잘 보일 수 있음
        LoginScreen()
    }
    // }
}
