package com.route.readers.ui.screens.login

import android.content.Context
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

private const val PREFS_NAME = "com.route.readers.AppPrefs"
private const val KEY_REMEMBERED_EMAIL = "remembered_email"
private const val KEY_REMEMBER_ID = "remember_id"
private const val KEY_AUTO_LOGIN = "auto_login"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var email by rememberSaveable {
        mutableStateOf(sharedPreferences.getString(KEY_REMEMBERED_EMAIL, "") ?: "")
    }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberId by rememberSaveable {
        mutableStateOf(sharedPreferences.getBoolean(KEY_REMEMBER_ID, false))
    }
    var autoLogin by rememberSaveable {
        mutableStateOf(sharedPreferences.getBoolean(KEY_AUTO_LOGIN, false))
    }

    val auth: FirebaseAuth = Firebase.auth
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (sharedPreferences.getBoolean(KEY_AUTO_LOGIN, false) && auth.currentUser != null && auth.currentUser!!.isEmailVerified) {
            Log.d("LoginScreen", "Auto-login condition met. Navigating to main screen.")
            onLoginSuccess()
        }
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { rememberId = !rememberId }
                ) {
                    Checkbox(
                        checked = rememberId,
                        onCheckedChange = { rememberId = it }
                    )
                    Text(text = "아이디 저장")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { autoLogin = !autoLogin }
                ) {
                    Checkbox(
                        checked = autoLogin,
                        onCheckedChange = { autoLogin = it }
                    )
                    Text(text = "자동 로그인")
                }
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
                                val user = auth.currentUser
                                if (user != null && user.isEmailVerified) {
                                    val editor = sharedPreferences.edit()
                                    if (rememberId) {
                                        editor.putString(KEY_REMEMBERED_EMAIL, email.trim())
                                    } else {
                                        editor.remove(KEY_REMEMBERED_EMAIL)
                                    }
                                    editor.putBoolean(KEY_REMEMBER_ID, rememberId)
                                    editor.putBoolean(KEY_AUTO_LOGIN, autoLogin)
                                    editor.apply()

                                    Log.d("LoginScreen", "signInWithEmail:success and email verified")
                                    Toast.makeText(context, "로그인되었습니다.", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                } else {
                                    Log.w("LoginScreen", "signInWithEmail:success but email not verified")
                                    errorMessage = "이메일 인증을 먼저 완료해주세요."
                                    auth.signOut()
                                }
                            } else {
                                Log.w("LoginScreen", "signInWithEmail:failure", task.exception)
                                val exceptionMessage = task.exception?.message
                                errorMessage = when {
                                    exceptionMessage?.contains("INVALID_LOGIN_CREDENTIALS") == true -> "이메일 또는 비밀번호가 잘못되었습니다."
                                    else -> "로그인에 실패했습니다."
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
