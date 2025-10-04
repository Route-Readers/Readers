package com.route.readers.ui.screens.signup

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.route.readers.R
import com.route.readers.ui.theme.ReadersTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val primaryRed = Color(0xFFC0392B)

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("725580725763-a6efs546tsd56hridug8ifsav9af0lav.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        isGoogleLoading = true
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                isGoogleLoading = false
                if (task.isSuccessful) {
                    Log.d("SignUpScreen", "Google signInWithCredential success")
                    Toast.makeText(context, "Google 계정으로 로그인했습니다.", Toast.LENGTH_SHORT).show()
                    onSignUpSuccess()
                } else {
                    Log.w("SignUpScreen", "Google signInWithCredential failure", task.exception)
                    errorMessage = "Google 로그인에 실패했습니다. (${task.exception?.message})"
                }
            }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d("SignUpScreen", "Google Sign In successful, getting idToken")
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Log.w("SignUpScreen", "Google sign in failed", e)
                    errorMessage = "Google 로그인에 실패했습니다. (API Exception)"
                }
            } else {
                Log.w("SignUpScreen", "Google sign in cancelled or failed. Result code: ${result.resultCode}")
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Readers",
                        color = primaryRed,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = primaryRed,
                    actionIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            Text(
                text = "회원가입",
                color = primaryRed,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Readers에 오신 것을 환영합니다!",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("이메일") },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = errorMessage?.contains("이메일") == true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = primaryRed,
                    unfocusedBorderColor = Color.LightGray,
                    cursorColor = primaryRed
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("비밀번호") },
                shape = RoundedCornerShape(16.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = errorMessage?.contains("비밀번호") == true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = primaryRed,
                    unfocusedBorderColor = Color.LightGray,
                    cursorColor = primaryRed
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = passwordConfirm,
                onValueChange = { passwordConfirm = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("비밀번호 확인") },
                shape = RoundedCornerShape(16.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = errorMessage?.contains("일치하지 않습니다") == true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = primaryRed,
                    unfocusedBorderColor = Color.LightGray,
                    cursorColor = primaryRed
                )
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
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
                                firebaseUser?.sendEmailVerification()
                                    ?.addOnCompleteListener { verificationTask ->
                                        if (verificationTask.isSuccessful) {
                                            Log.d("SignUpScreen", "Email verification sent.")
                                            Toast.makeText(context, "인증 메일을 보냈습니다. 프로필을 설정해주세요.", Toast.LENGTH_LONG).show()
                                        } else {
                                            Log.e("SignUpScreen", "sendEmailVerification failed", verificationTask.exception)
                                            Toast.makeText(context, "인증 메일 발송에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                        onSignUpSuccess()
                                    }
                            } else {
                                Log.w("SignUpScreen", "createUserWithEmail:failure", task.exception)
                                errorMessage = when (val exception = task.exception) {
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryRed),
                enabled = !isLoading && !isGoogleLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("가입하기", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Divider(modifier = Modifier.weight(1f))
                Text(" 또는 ", modifier = Modifier.padding(horizontal = 8.dp), color = Color.Gray)
                Divider(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (!isGoogleLoading && !isLoading) {
                        errorMessage = null
                        Log.d("SignUpScreen", "Launching Google Sign-In flow")
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading && !isGoogleLoading,
                contentPadding = PaddingValues(0.dp),
                border = null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            ) {
                if (isGoogleLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = primaryRed,
                        strokeWidth = 3.dp
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.mipmap.signupgoogle),
                        contentDescription = "Google 계정으로 계속하기",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            TextButton(
                onClick = {
                    if (!isLoading && !isGoogleLoading) {
                        onNavigateToLogin()
                    }
                },
                enabled = !isLoading && !isGoogleLoading
            ) {
                Text("이미 계정이 있으신가요? 로그인", color = Color.Gray)
            }

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultSignUpScreenPreview() {
    ReadersTheme {
        SignUpScreen(
            onSignUpSuccess = {},
            onNavigateToLogin = {},
            onNavigateBack = {}
        )
    }
}
