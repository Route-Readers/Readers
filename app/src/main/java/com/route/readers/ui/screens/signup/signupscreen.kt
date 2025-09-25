package com.route.readers.ui.screens.signup

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordConfirm by rememberSaveable { mutableStateOf("") }

    val auth: FirebaseAuth = Firebase.auth
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "회원가입",
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
            isError = errorMessage?.contains("이메일") == true
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
            isError = errorMessage?.contains("비밀번호") == true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = passwordConfirm,
            onValueChange = { passwordConfirm = it; errorMessage = null },
            label = { Text("비밀번호를 다시 입력하세요") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage?.contains("일치하지 않습니다") == true
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "이메일과 비밀번호를 모두 입력해주세요."
                    return@Button
                }
                if (password != passwordConfirm) {
                    errorMessage = "비밀번호가 일치하지 않습니다."
                    return@Button
                }

                isLoading = true
                errorMessage = null

                auth.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            Log.d("SignUpScreen", "createUserWithEmail:success")
                            val firebaseUser = auth.currentUser
                            Toast.makeText(context, "회원가입 성공: ${firebaseUser?.email}", Toast.LENGTH_SHORT).show()
                            onSignUpSuccess()
                        } else {
                            Log.w("SignUpScreen", "createUserWithEmail:failure", task.exception)
                            val exception = task.exception
                            errorMessage = when (exception) {
                                is FirebaseAuthWeakPasswordException -> "비밀번호는 6자 이상이어야 합니다."
                                is FirebaseAuthInvalidCredentialsException -> "이메일 형식이 올바르지 않습니다."
                                is FirebaseAuthUserCollisionException -> "이미 사용 중인 이메일입니다."
                                else -> "회원가입에 실패했습니다. (${exception?.message ?: "알 수 없는 오류"})"
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
                Text("회원가입 완료", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = {
                if (!isLoading) {
                    onNavigateToLogin()
                }
            },
            enabled = !isLoading
        ) {
            Text("이미 계정이 있으신가요? 로그인")
        }
    }
}

@Preview(showBackground = true, name = "SignUp Screen Preview")
@Composable
fun DefaultSignUpScreenPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            SignUpScreen(
                onSignUpSuccess = { Log.d("Preview", "Sign Up Success") },
                onNavigateToLogin = { Log.d("Preview", "Navigate to Login") }
            )
        }
    }
}

