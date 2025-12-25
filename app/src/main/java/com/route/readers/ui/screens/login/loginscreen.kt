package com.route.readers.ui.screens.login

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.route.readers.R

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

    var email by remember { mutableStateOf(sharedPreferences.getString(KEY_REMEMBERED_EMAIL, "") ?: "") }
    var password by remember { mutableStateOf("") }
    var rememberId by remember { mutableStateOf(sharedPreferences.getBoolean(KEY_REMEMBER_ID, false)) }

    val uiState by loginViewModel.uiState.collectAsState()
    val isLoading = uiState is LoginUiState.Loading || uiState is LoginUiState.GoogleLoading

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("725580725763-a6efs546tsd56hridug8ifsav9af0lav.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { loginViewModel.loginWithGoogle(it) }
                    ?: loginViewModel.resetStateToError("Google ID 토큰을 가져오지 못했습니다.")
            } catch (e: ApiException) {
                loginViewModel.resetStateToError("Google 로그인 실패")
            }
        } else {
            loginViewModel.resetState()
        }
    }

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
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "다시 만나서\n반가워요!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "로그인하고 독서를 계속하세요",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("이메일") },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("비밀번호") },
                shape = RoundedCornerShape(12.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { rememberId = !rememberId }
                ) {
                    Checkbox(
                        checked = rememberId,
                        onCheckedChange = { rememberId = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text("아이디 저장", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    sharedPreferences.edit().apply {
                        if (rememberId) putString(KEY_REMEMBERED_EMAIL, email.trim())
                        else remove(KEY_REMEMBERED_EMAIL)
                        putBoolean(KEY_REMEMBER_ID, rememberId)
                        apply()
                    }
                    loginViewModel.login(email, password)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (uiState is LoginUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("로그인", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(" 또는 ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (uiState is LoginUiState.GoogleLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Image(
                        painter = painterResource(id = R.mipmap.signupgoogle),
                        contentDescription = null,
                        modifier = Modifier.height(24.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google로 계속하기", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = { if (!isLoading) onNavigateToSignUp() }) {
                Text("계정이 없으신가요? 회원가입", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
