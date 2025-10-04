package com.route.readers.ui.screens.login

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

private const val PREFS_NAME = "com.route.readers.AppPrefs"
private const val KEY_REMEMBERED_EMAIL = "remembered_email"
private const val KEY_REMEMBER_ID = "remember_id"
private const val KEY_AUTO_LOGIN = "auto_login"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateBack: () -> Unit
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

    val darkRedColor = Color(0xFFB71C1C)

    LaunchedEffect(Unit) {
        if (autoLogin && auth.currentUser != null && auth.currentUser!!.isEmailVerified) {
            Log.d("LoginScreen", "Auto-login condition met. Navigating to main screen.")
            onLoginSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Readers",
                        fontWeight = FontWeight.Bold,
                        color = darkRedColor
                    )
                },
                navigationIcon = {},
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "로그인",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = darkRedColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "독서를 시작하세요",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("이메일") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("비밀번호") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null,
                shape = RoundedCornerShape(12.dp)
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
                    modifier = Modifier
                        .clickable { rememberId = !rememberId }
                        .padding(end = 16.dp)
                ) {
                    Checkbox(checked = rememberId, onCheckedChange = { rememberId = it })
                    Text(text = "아이디 저장", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { autoLogin = !autoLogin }
                ) {
                    Checkbox(checked = autoLogin, onCheckedChange = { autoLogin = it })
                    Text(text = "자동 로그인", style = MaterialTheme.typography.bodyMedium)
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
                    .height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = darkRedColor,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("로그인", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = {
                    if (!isLoading) {
                        onNavigateToSignUp()
                    }
                },
                enabled = !isLoading
            ) {
                Text(
                    text = "계정이 없으신가요? 회원가입",
                    color = darkRedColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


@Preview(showBackground = true, name = "Login Screen Preview")
@Composable
fun DefaultLoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            onLoginSuccess = { Log.d("Preview", "Login Success Clicked") },
            onNavigateToSignUp = { Log.d("Preview", "Navigate to Sign Up Clicked") },
            onNavigateBack = { Log.d("Preview", "Navigate Back Clicked") }
        )
    }
}
