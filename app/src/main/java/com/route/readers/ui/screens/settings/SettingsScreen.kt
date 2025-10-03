package com.route.readers.ui.screens.profile.settings

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.LocalAppNavController

data class SettingItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var currentSettingView by remember { mutableStateOf("main") }
    val appNavController = LocalAppNavController.current
    val firebaseAuth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("725580725763-a6efs546tsd56hridug8ifsav9af0lav.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentSettingView == "account") "계정 관리" else "환경설정",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentSettingView == "account") {
                            currentSettingView = "main"
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        when (currentSettingView) {
            "main" -> {
                MainSettingsContent(
                    modifier = Modifier.padding(innerPadding),
                    onNavigateToAccountManagement = {
                        currentSettingView = "account"
                    }
                )
            }
            "account" -> {
                AccountManagementContent(
                    modifier = Modifier.padding(innerPadding),
                    onLogout = {
                        firebaseAuth.signOut()
                        googleSignInClient.signOut().addOnCompleteListener {
                            Log.d("SettingsScreen", "Firebase and Google user signed out.")
                            appNavController?.navigate("login_route") {
                                popUpTo("main_app_content_route") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MainSettingsContent(
    modifier: Modifier = Modifier,
    onNavigateToAccountManagement: () -> Unit
) {
    val settingItems = listOf(
        SettingItem(
            id = "account",
            title = "계정 관리",
            icon = Icons.Filled.ManageAccounts,
            action = onNavigateToAccountManagement
        ),
        SettingItem(
            id = "notifications",
            title = "알림 설정",
            icon = Icons.Filled.Notifications,
            action = { Log.d("SettingsScreen", "알림 설정 클릭") }
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        settingItems.forEach { item ->
            SettingRow(item = item)
            HorizontalDivider()
        }
    }
}

@Composable
fun AccountManagementContent(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "계정 정보를 관리하고 로그아웃 할 수 있습니다.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("로그아웃", color = MaterialTheme.colorScheme.onError)
        }
    }
}

@Composable
fun SettingRow(item: SettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.action)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = item.icon, contentDescription = item.title, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = item.title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "이동")
    }
}
