package com.route.readers.ui.screens.login

import android.content.Context // SharedPreferences 사용을 위해 Context 임포트
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

// SharedPreferences 키 상수 정의
private const val PREFS_NAME = "com.route.readers.AppPrefs" // 앱의 고유한 이름으로 설정
private const val KEY_REMEMBERED_EMAIL = "remembered_email"
private const val KEY_REMEMBER_ID = "remember_id"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val context = LocalContext.current
    // SharedPreferences 인스턴스를 remember를 사용하여 컴포지션 중에 유지
    val sharedPreferences = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // SharedPreferences에서 저장된 값으로 상태 변수 초기화
    var email by rememberSaveable {
        mutableStateOf(sharedPreferences.getString(KEY_REMEMBERED_EMAIL, "") ?: "")
    }
    var password by rememberSaveable { mutableStateOf("") } // 비밀번호는 보통 저장하지 않음
    var rememberId by rememberSaveable {
        mutableStateOf(sharedPreferences.getBoolean(KEY_REMEMBER_ID, false))
    }

    val auth: FirebaseAuth = Firebase.auth
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                text = "[앱 로고]", // TODO: 실제 앱 로고 이미지로 교체
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){

            Text(
                text = "로그인",
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("이메일을 입력하세요") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("비밀번호를 입력하세요") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

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
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "이메일과 비밀번호를 모두 입력해주세요."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null

                    auth.signInWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                // 로그인 성공 시 SharedPreferences에 아이디 저장 여부 및 이메일 저장
                                if (rememberId) {
                                    sharedPreferences.edit()
                                        .putString(KEY_REMEMBERED_EMAIL, email.trim())
                                        .putBoolean(KEY_REMEMBER_ID, true)
                                        .apply() // 비동기적으로 저장
                                } else {
                                    // 아이디 저장 체크 해제 시 저장된 정보 삭제
                                    sharedPreferences.edit()
                                        .remove(KEY_REMEMBERED_EMAIL)
                                        .putBoolean(KEY_REMEMBER_ID, false) // 또는 .remove(KEY_REMEMBER_ID)
                                        .apply()
                                }

                                Log.d("LoginScreen", "signInWithEmail:success")
                                val user = auth.currentUser
                                Toast.makeText(context, "로그인 성공: ${user?.email}", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                Log.w("LoginScreen", "signInWithEmail:failure", task.exception)
                                val exceptionMessage = task.exception?.message
                                errorMessage = when {
                                    exceptionMessage?.contains("ERROR_USER_NOT_FOUND") == true ||
                                            exceptionMessage?.contains("auth/user-not-found") == true -> "등록되지 않은 이메일입니다."
                                    exceptionMessage?.contains("ERROR_WRONG_PASSWORD") == true ||
                                            exceptionMessage?.contains("auth/wrong-password") == true -> "잘못된 비밀번호입니다."
                                    exceptionMessage?.contains("INVALID_LOGIN_CREDENTIALS") == true -> "이메일 또는 비밀번호가 잘못되었습니다." // Firebase 최신 에러 코드
                                    else -> "로그인에 실패했습니다. (${exceptionMessage ?: "알 수 없는 오류"})"
                                }
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("로그인", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("계정이 없으신가요? ")
                TextButton(
                    onClick = {
                        if (!isLoading) {
                            onNavigateToSignUp()
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("회원가입")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Login Screen Preview")
@Composable
fun DefaultLoginScreenPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            LoginScreen(
                onLoginSuccess = { Log.d("Preview", "Login Success Clicked") },
                onNavigateToSignUp = { Log.d("Preview", "Navigate to Sign Up Clicked") }
            )
        }
    }
}
