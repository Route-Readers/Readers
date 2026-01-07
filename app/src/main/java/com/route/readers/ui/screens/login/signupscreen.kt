package com.route.readers.ui.screens.login

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

    var isGoogleLoading by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateEmail by remember { mutableStateOf<String?>(null) }
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

                        phoneCredential?.let { phoneCred ->
                            try {
                                user.linkWithCredential(phoneCred).await()
                                val phoneHash = java.security.MessageDigest.getInstance("SHA-256")
                                    .digest(phoneNumber.toByteArray())
                                    .joinToString("") { "%02x".format(it) }
                                db.collection("users").document(user.uid)
                                    .update("phoneHash", phoneHash).await()
                            } catch (_: FirebaseAuthUserCollisionException) {}
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
            } catch (_: ApiException) {
                errorMessage = "Google 로그인 실패"
            }
        }
    }

    suspend fun checkPhoneDuplicate(phone: String): Pair<Boolean, String?> {
        val phoneHash = java.security.MessageDigest.getInstance("SHA-256")
            .digest(phone.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val docs = db.collection("users").whereEqualTo("phoneHash", phoneHash).get().await()
        if (docs.isEmpty) return Pair(true, null)
        val existingEmail = docs.documents.firstOrNull()?.getString("email")
        return Pair(false, existingEmail)
    }

    fun sendVerificationCode() {
        if (phoneNumber.length != 11 || !phoneNumber.startsWith("010")) {
            errorMessage = "올바른 전화번호를 입력해주세요."
            return
        }
        isLoading = true
        errorMessage = null

        scope.launch {
            val (isAvailable, email) = checkPhoneDuplicate(phoneNumber)
            if (!isAvailable) {
                isLoading = false
                duplicateEmail = email
                showDuplicateDialog = true
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

    fun verifyCode() {
        val vId = verificationId ?: return
        isLoading = true
        errorMessage = null
        phoneCredential = PhoneAuthProvider.getCredential(vId, verificationCode)
        isLoading = false
        currentStep = SignUpStep.ACCOUNT
    }

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

                phoneCredential?.let { cred ->
                    user.linkWithCredential(cred).await()
                }

                val phoneHash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(phoneNumber.toByteArray())
                    .joinToString("") { "%02x".format(it) }
                db.collection("users").document(user.uid).set(mapOf("phoneHash" to phoneHash)).await()

                // 이메일 인증 - 전화번호 인증으로 대체하여 주석처리
                // user.sendEmailVerification().await()
                Toast.makeText(context, "가입이 완료되었습니다!", Toast.LENGTH_SHORT).show()
                onSignUpSuccess()
            } catch (_: FirebaseAuthUserCollisionException) {
                errorMessage = "이미 사용 중인 이메일입니다."
            } catch (_: FirebaseAuthInvalidCredentialsException) {
                errorMessage = "이메일 형식이 올바르지 않습니다."
            } catch (e: Exception) {
                errorMessage = "가입 실패: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    val stepIndex = when (currentStep) {
        SignUpStep.PHONE -> 0
        SignUpStep.CODE -> 1
        SignUpStep.ACCOUNT -> 2
    }

    // 이미 가입된 전화번호 다이얼로그
    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = {
                Text("이미 가입된 번호예요", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("이 전화번호로 가입된 계정이 있어요.")
                    duplicateEmail?.let { email ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "가입된 이메일: ${email.take(3)}***${email.substringAfter("@")}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDuplicateDialog = false
                    onNavigateToLogin()
                }) {
                    Text("로그인하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateDialog = false }) {
                    Text("취소")
                }
            }
        )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 진행 인디케이터
            StepIndicator(currentStep = stepIndex, totalSteps = 3)

            Spacer(modifier = Modifier.height(48.dp))

            // 헤더 애니메이션
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) + slideInVertically { -40 })
                        .togetherWith(fadeOut(animationSpec = tween(200)) + slideOutVertically { 40 })
                },
                label = "header"
            ) { step ->
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = when (step) {
                            SignUpStep.PHONE -> "전화번호를\n입력해주세요"
                            SignUpStep.CODE -> "인증번호를\n입력해주세요"
                            SignUpStep.ACCOUNT -> "계정 정보를\n입력해주세요"
                        },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 38.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (step) {
                            SignUpStep.PHONE -> "본인 인증을 위해 필요해요"
                            SignUpStep.CODE -> "$phoneNumber 로 발송했어요"
                            SignUpStep.ACCOUNT -> "로그인에 사용할 정보예요"
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 입력 필드 애니메이션
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400, delayMillis = 100)) +
                            slideInHorizontally { if (targetState > initialState) it else -it })
                        .togetherWith(fadeOut(animationSpec = tween(200)) +
                                slideOutHorizontally { if (targetState > initialState) -it else it })
                },
                label = "input"
            ) { step ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    when (step) {
                        SignUpStep.PHONE -> {
                            UnderlineTextField(
                                value = phoneNumber,
                                onValueChange = { if (it.length <= 11 && it.all { c -> c.isDigit() }) phoneNumber = it },
                                label = "전화번호",
                                placeholder = "01012345678",
                                keyboardType = KeyboardType.Phone
                            )
                        }
                        SignUpStep.CODE -> {
                            UnderlineTextField(
                                value = verificationCode,
                                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) verificationCode = it },
                                label = "인증번호",
                                placeholder = "6자리 숫자",
                                keyboardType = KeyboardType.Number
                            )
                        }
                        SignUpStep.ACCOUNT -> {
                            UnderlineTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = "이메일",
                                placeholder = "example@email.com",
                                keyboardType = KeyboardType.Email
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            UnderlineTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = "비밀번호",
                                placeholder = "6자 이상",
                                keyboardType = KeyboardType.Password,
                                isPassword = true
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            UnderlineTextField(
                                value = passwordConfirm,
                                onValueChange = { passwordConfirm = it },
                                label = "비밀번호 확인",
                                placeholder = "다시 입력해주세요",
                                keyboardType = KeyboardType.Password,
                                isPassword = true
                            )
                        }
                    }
                }
            }

            // 에러 메시지
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
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

            // Google 로그인
            if (currentStep == SignUpStep.PHONE) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(" 또는 ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                googleSignInClient.signOut().await()
                            } catch (_: Exception) {
                                // Ignore signOut failure
                            }
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading && !isGoogleLoading,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Google 로그인
            if (currentStep == SignUpStep.ACCOUNT) {

            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("이미 계정이 있으신가요? 로그인", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(totalSteps) { index ->
            val isActive = index <= currentStep
            val width by animateDpAsState(
                targetValue = if (index == currentStep) 32.dp else 12.dp,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                label = "width"
            )
            val color by animateColorAsState(
                targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                animationSpec = tween(300),
                label = "color"
            )

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
            if (index < totalSteps - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun UnderlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val lineColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(200),
        label = "lineColor"
    )
    val lineWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 1.dp,
        animationSpec = tween(200),
        label = "lineWidth"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth(),
            placeholder = {
                Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            },
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = lineColor,
                unfocusedIndicatorColor = lineColor,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is androidx.compose.foundation.interaction.FocusInteraction.Focus -> isFocused = true
                            is androidx.compose.foundation.interaction.FocusInteraction.Unfocus -> isFocused = false
                        }
                    }
                }
            }
        )
    }
}
