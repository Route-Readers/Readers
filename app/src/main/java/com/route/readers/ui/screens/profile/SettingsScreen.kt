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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.ui.screens.profile.ProfileUiState
import com.route.readers.ui.screens.profile.ProfileViewModel

data class SettingItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bottomNavController: NavController?,
    appNavController: NavController?,
    profileViewModel: ProfileViewModel = viewModel()
) {
    var currentSettingView by remember { mutableStateOf("main") }
    val firebaseAuth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    val uiState by profileViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        profileViewModel.fetchUserProfile(null)
    }

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
                        text = when (currentSettingView) {
                            "account" -> "계정 관리"
                            "notifications" -> "알림 설정"
                            else -> "환경설정"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentSettingView != "main") {
                            currentSettingView = "main"
                        } else {
                            bottomNavController?.popBackStack()
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
                    },
                    onNavigateToNotifications = {
                        currentSettingView = "notifications"
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
                            appNavController?.navigate("onboarding_route") {
                                popUpTo(appNavController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            "notifications" -> {
                NotificationSettingsContent(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    viewModel = profileViewModel
                )
            }
        }
    }
}

@Composable
fun MainSettingsContent(
    modifier: Modifier = Modifier,
    onNavigateToAccountManagement: () -> Unit,
    onNavigateToNotifications: () -> Unit
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
            action = onNavigateToNotifications
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
fun NotificationSettingsContent(
    modifier: Modifier = Modifier,
    uiState: ProfileUiState,
    viewModel: ProfileViewModel
) {
    val user = (uiState as? ProfileUiState.Success)?.user

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (user != null) {
            NotificationSwitch(
                title = "팔로우 알림",
                checked = user.followAlarmEnabled,
                onCheckedChange = { isEnabled ->
                    viewModel.updateNotificationSetting("followAlarmEnabled", isEnabled)
                }
            )
            HorizontalDivider()
            NotificationSwitch(
                title = "좋아요 알림",
                checked = user.likeAlarmEnabled,
                onCheckedChange = { isEnabled ->
                    viewModel.updateNotificationSetting("likeAlarmEnabled", isEnabled)
                }
            )
            HorizontalDivider()
            NotificationSwitch(
                title = "친구 독서 알림",
                checked = user.friendReadingAlarmEnabled,
                onCheckedChange = { isEnabled ->
                    viewModel.updateNotificationSetting("friendReadingAlarmEnabled", isEnabled)
                }
            )
            HorizontalDivider()
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun NotificationSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
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
