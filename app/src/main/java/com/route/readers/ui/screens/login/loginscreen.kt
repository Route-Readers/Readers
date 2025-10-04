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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private const val PREFS_NAME = "com.route.readers.AppPrefs"
private const val KEY_REMEMBERED_EMAIL = "remembered_email"
private const val KEY_REMEMBER_ID = "remember_id"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToCreateProfile: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateBack: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var email by remember {
        mutableStateOf(sharedPreferences.getString(KEY_REMEMBERED_EMAIL, "") ?: "")
    }
    var password by remember { mutableStateOf("") }
    var rememberId by remember {
        mutableStateOf(sharedPreferences.getBoolean(KEY_REMEMBER_ID, false))
    }

    val uiState by loginViewModel.uiState.collectAsState()
    val isLoading = uiState is LoginUiState.Loading

    val darkRedColor = Color(0xFFB71C1C)

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.ExistingUser -> {
                Toast.makeText(context, "로그인되었습니다.", Toast.LENGTH_SHORT).show()
                onNavigateToHome()
                loginViewModel.resetState()
            }
            is LoginUiState.NewUser -> {
                onNavigateToCreateProfile()
                loginViewModel.resetState()
            }
            is LoginUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                loginViewModel.resetState()
            }
            else -> { /* Idle or Loading */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Readers", fontWeight = FontWeight.Bold, color = darkRedColor) },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
                onValueChange = { email = it },
                label = { Text("이메일") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
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
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    sharedPreferences.edit().apply {
                        if (rememberId) {
                            putString(KEY_REMEMBERED_EMAIL, email.trim())
                        } else {
                            remove(KEY_REMEMBERED_EMAIL)
                        }
                        putBoolean(KEY_REMEMBER_ID, rememberId)
                        apply()
                    }
                    loginViewModel.login(email, password)
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
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("로그인", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = { if (!isLoading) onNavigateToSignUp() },
                enabled = !isLoading
            ) {
                Text("계정이 없으신가요? 회원가입", color = darkRedColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultLoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            onNavigateToHome = { Log.d("Preview", "Navigating to Home") },
            onNavigateToCreateProfile = { Log.d("Preview", "Navigating to Create Profile") },
            onNavigateToSignUp = { Log.d("Preview", "Navigating to Sign Up") },
            onNavigateBack = { Log.d("Preview", "Navigating Back") }
        )
    }
}
