package com.route.readers.ui.screens.login

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

enum class SignUpStep { PHONE, CODE, ACCOUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(SignUpStep.PHONE) }
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var verificationId by remember { mutableStateOf<String?>(null) }
    var phoneCredential by remember { mutableStateOf<PhoneAuthCredential?>(null) }

    // Google Sign-In
    var isGoogleLoading by remember { mutableStateOf(false) }
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
                val account = task.getResult(ApiException::class.java)!!
                isGoogleLoading = true
                scope.launch {
                    try {
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        val authResult = auth.signInWithCredential(credential).await()
                        val user = authResult.user!!

                        // 전화번호 연결
                        phoneCredential?.let { phoneCred ->
                            try {
                                user.linkWithCredential(phoneCred).await()
                                val phoneHash = java.security.MessageDigest.getInstance("SHA-256")
                                    .digest(phoneNumber.toByteArray())
                                    .joinToString("") { "%02x".format(it) }
                                db.collection("users").document(user.uid)
                                    .update("phoneHash", phoneHash).await()
                            } catch (e: FirebaseAuthUserCollisionException) {
                                // 이미 연결된 경우 무시
                            }
                        }

                        val userDoc = db.collection("users").document(user.uid).get().await()
                        if (userDoc.exists() && userDoc.getString("nickname") != null) {
                            Toast.makeText(context, "로그인합니다.", Toast.LENGTH_SHORT).show()
                            onNavigateToHome()
                        } else {
                            Toast.makeText(context, "프로필을 설정해주세요.", Toast.LENGTH_SHORT).show()
                            onSignUpSuccess()
                        }
                    } catch (e: Exception) {
                        errorMessage = "Google 로그인 실패: ${e.message}"
                    } finally {
                        isGoogleLoading = false
                    }
                }
            } catch (e: ApiException) {
                errorMessage = "Google 로그인 실패"
            }
        }
    }

    // 전화번호 중복 체크
    suspend fun checkPhoneDuplicate(phone: String): Boolean {
        val phoneHash = java.security.MessageDigest.getInstance("SHA-256")
            .digest(phone.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val docs = db.collection("users").whereEqualTo("phoneHash", phoneHash).get().await()
        return docs.isEmpty
    }

    // SMS 발송
    fun sendVerificationCode() {
        if (phoneNumber.length != 11 || !phoneNumber.startsWith("010")) {
            errorMessage = "올바른 전화번호를 입력해주세요."
            return
        }
        isLoading = true
        errorMessage = null

        scope.launch {
            if (!checkPhoneDuplicate(phoneNumber)) {
                isLoading = false
                errorMessage = "이미 가입된 전화번호입니다."
                return@launch
            }

            val formattedPhone = "+82${phoneNumber.substring(1)}"
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    phoneCredential = credential
                    isLoading = false
                    currentStep = SignUpStep.ACCOUNT
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    isLoading = false
                    errorMessage = "인증 실패: ${e.message}"
                }

                override fun onCodeSent(vId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = vId
                    isLoading = false
                    currentStep = SignUpStep.CODE
                }
            }

            PhoneAuthProvider.verifyPhoneNumber(
                PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(formattedPhone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build()
            )
        }
    }

    // 인증코드 확인
    fun verifyCode() {
        val vId = verificationId ?: return
        isLoading = true
        errorMessage = null
        phoneCredential = PhoneAuthProvider.getCredential(vId, verificationCode)
        isLoading = false
        currentStep = SignUpStep.ACCOUNT
    }

    // 이메일 계정 생성
    fun createAccount() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "이메일과 비밀번호를 입력해주세요."
            return
        }
        if (password != passwordConfirm) {
            errorMessage = "비밀번호가 일치하지 않습니다."
            return
        }
        if (password.length < 6) {
            errorMessage = "비밀번호는 6자 이상이어야 합니다."
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val user = result.user!!

                // 전화번호 연결
                phoneCredential?.let { cred ->
                    user.linkWithCredential(cred).await()
                }

                // phoneHash 저장
                val phoneHash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(phoneNumber.toByteArray())
                    .joinToString("") { "%02x".format(it) }
                db.collection("users").document(user.uid).set(mapOf("phoneHash" to phoneHash)).await()

                // 이메일 인증 발송
                user.sendEmailVerification().await()
                Toast.makeText(context, "인증 메일을 확인해주세요.", Toast.LENGTH_SHORT).show()
                onSignUpSuccess()
            } catch (e: FirebaseAuthUserCollisionException) {
                errorMessage = "이미 사용 중인 이메일입니다."
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                errorMessage = "이메일 형식이 올바르지 않습니다."
            } catch (e: Exception) {
                errorMessage = "가입 실패: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        when (currentStep) {
                            SignUpStep.PHONE -> onNavigateBack()
                            SignUpStep.CODE -> currentStep = SignUpStep.PHONE
                            SignUpStep.ACCOUNT -> currentStep = SignUpStep.CODE
                        }
                    }) {
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

            // 헤더 문구
            Text(
                text = when (currentStep) {
                    SignUpStep.PHONE -> "친구와 함께\n책을 읽어볼까요?"
                    SignUpStep.CODE -> "인증번호를\n입력해주세요"
                    SignUpStep.ACCOUNT -> "계정 정보를\n입력해주세요"
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (currentStep) {
                    SignUpStep.PHONE -> "전화번호로 본인 인증을 진행합니다"
                    SignUpStep.CODE -> "$phoneNumber 로 발송된 코드를 입력하세요"
                    SignUpStep.ACCOUNT -> "로그인에 사용할 이메일과 비밀번호를 설정하세요"
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Step별 입력 필드
            AnimatedContent(targetState = currentStep, label = "step") { step ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        SignUpStep.PHONE -> {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { if (it.length <= 11 && it.all { c -> c.isDigit() }) phoneNumber = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("전화번호") },
                                placeholder = { Text("01012345678") },
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        SignUpStep.CODE -> {
                            OutlinedTextField(
                                value = verificationCode,
                                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) verificationCode = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("인증번호 6자리") },
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        SignUpStep.ACCOUNT -> {
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
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = passwordConfirm,
                                onValueChange = { passwordConfirm = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("비밀번호 확인") },
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // 에러 메시지
            errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 메인 버튼
            Button(
                onClick = {
                    errorMessage = null
                    when (currentStep) {
                        SignUpStep.PHONE -> sendVerificationCode()
                        SignUpStep.CODE -> verifyCode()
                        SignUpStep.ACCOUNT -> createAccount()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && !isGoogleLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        when (currentStep) {
                            SignUpStep.PHONE -> "인증번호 받기"
                            SignUpStep.CODE -> "확인"
                            SignUpStep.ACCOUNT -> "가입하기"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Google 로그인 (계정 단계에서만)
            if (currentStep == SignUpStep.ACCOUNT) {
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
                    enabled = !isLoading && !isGoogleLoading
                ) {
                    if (isGoogleLoading) {
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
            }

            Spacer(modifier = Modifier.weight(1f))

            // 로그인 링크
            TextButton(onClick = onNavigateToLogin) {
                Text("이미 계정이 있으신가요? 로그인", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
