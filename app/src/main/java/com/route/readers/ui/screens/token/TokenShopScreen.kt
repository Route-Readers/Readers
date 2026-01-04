package com.route.readers.ui.screens.token

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.R
import com.route.readers.ui.theme.DarkRed
import com.route.readers.utils.RewardedAdManager

data class ShopCharacter(
    val id: String,
    val drawableRes: Int,
    val name: String,
    val price: Int
)

data class ShopColor(
    val id: String,
    val color: Color,
    val name: String,
    val price: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenShopScreen(
    onNavigateBack: () -> Unit,
    rewardedAdManager: RewardedAdManager,
    onTokenEarned: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val tokenColor = Color(0xFFFFA000)
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var currentTokens by remember { mutableIntStateOf(0) }
    var unlockedCharacters by remember { mutableStateOf<List<String>>(emptyList()) }
    var unlockedColors by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentCharacter by remember { mutableStateOf<String?>(null) }
    var currentColor by remember { mutableStateOf<String?>(null) }
    var nickname by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // 구매 확인 다이얼로그 상태
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var pendingPurchase by remember { mutableStateOf<Any?>(null) }

    val characters = listOf(
        ShopCharacter("lion", R.drawable.lion, "사자", 5),
        ShopCharacter("penguin", R.drawable.penguin, "펭귄", 5),
        ShopCharacter("redpanda", R.drawable.redpanda, "레서판다", 8),
        ShopCharacter("squirrel", R.drawable.squirrel, "다람쥐", 5),
        ShopCharacter("cat", R.drawable.cat, "고양이", 5),
        ShopCharacter("dog", R.drawable.dog, "강아지", 5),
        ShopCharacter("fox", R.drawable.fox, "여우", 8),
        ShopCharacter("rabbit", R.drawable.rabbit, "토끼", 5)
    )

    val colors = listOf(
        ShopColor("#5C6BC0", Color(0xFF5C6BC0), "인디고", 3),
        ShopColor("#26A69A", Color(0xFF26A69A), "틸", 3),
        ShopColor("#FF7043", Color(0xFFFF7043), "코랄", 3),
        ShopColor("#AB47BC", Color(0xFFAB47BC), "퍼플", 3),
        ShopColor("#42A5F5", Color(0xFF42A5F5), "스카이", 3),
        ShopColor("#66BB6A", Color(0xFF66BB6A), "민트", 3),
        ShopColor("#FFA726", Color(0xFFFFA726), "앰버", 3),
        ShopColor("#EC407A", Color(0xFFEC407A), "로즈", 3),
        ShopColor("#78909C", Color(0xFF78909C), "슬레이트", 3),
        ShopColor("#8D6E63", Color(0xFF8D6E63), "브라운", 3)
    )

    // 데이터 로드
    LaunchedEffect(Unit) {
        uid?.let {
            db.collection("users").document(it).get().addOnSuccessListener { doc ->
                currentTokens = doc.getLong("tokens")?.toInt() ?: 0
                unlockedCharacters = (doc.get("unlockedCharacters") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                unlockedColors = (doc.get("unlockedColors") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                currentCharacter = doc.getString("profileCharacter")
                currentColor = doc.getString("profileBackgroundColor")
                nickname = doc.getString("nickname") ?: ""
                isLoading = false
            }
        }
    }

    fun purchaseCharacter(character: ShopCharacter) {
        if (currentTokens < character.price || uid == null) return
        db.collection("users").document(uid).update(
            mapOf(
                "tokens" to FieldValue.increment(-character.price.toLong()),
                "unlockedCharacters" to FieldValue.arrayUnion(character.id)
            )
        ).addOnSuccessListener {
            currentTokens -= character.price
            unlockedCharacters = unlockedCharacters + character.id
            Toast.makeText(context, "${character.name} 구매 완료!", Toast.LENGTH_SHORT).show()
        }
    }

    fun purchaseColor(color: ShopColor) {
        if (currentTokens < color.price || uid == null) return
        db.collection("users").document(uid).update(
            mapOf(
                "tokens" to FieldValue.increment(-color.price.toLong()),
                "unlockedColors" to FieldValue.arrayUnion(color.id)
            )
        ).addOnSuccessListener {
            currentTokens -= color.price
            unlockedColors = unlockedColors + color.id
            Toast.makeText(context, "${color.name} 구매 완료!", Toast.LENGTH_SHORT).show()
        }
    }

    fun applyCharacter(characterId: String?) {
        uid?.let {
            db.collection("users").document(it).update("profileCharacter", characterId)
            currentCharacter = characterId
            Toast.makeText(context, "캐릭터 적용!", Toast.LENGTH_SHORT).show()
        }
    }

    fun applyColor(colorId: String?) {
        uid?.let {
            db.collection("users").document(it).update("profileBackgroundColor", colorId)
            currentColor = colorId
            Toast.makeText(context, "배경색 적용!", Toast.LENGTH_SHORT).show()
        }
    }

    // 구매 확인 다이얼로그
    if (showPurchaseDialog && pendingPurchase != null) {
        val (name, price) = when (val item = pendingPurchase) {
            is ShopCharacter -> item.name to item.price
            is ShopColor -> item.name to item.price
            else -> "" to 0
        }
        AlertDialog(
            onDismissRequest = { showPurchaseDialog = false; pendingPurchase = null },
            title = { Text("구매 확인") },
            text = { Text("${name}을(를) ${price} 토큰에 구매하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    when (val item = pendingPurchase) {
                        is ShopCharacter -> purchaseCharacter(item)
                        is ShopColor -> purchaseColor(item)
                    }
                    showPurchaseDialog = false
                    pendingPurchase = null
                }) { Text("구매") }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDialog = false; pendingPurchase = null }) { Text("취소") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("교환소") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 프로필 미리보기 + 토큰
            ProfilePreviewCard(
                nickname = nickname,
                character = currentCharacter,
                backgroundColor = currentColor,
                tokens = currentTokens,
                isLoading = isLoading,
                tokenColor = tokenColor
            )

            // 토큰 획득 섹션
            Text("토큰 획득", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            // 짧은 광고 - 토큰 1개
            WatchAdCard(
                tokenColor = tokenColor,
                title = "짧은 광고 보기",
                description = "15초 광고 시청",
                reward = 1
            ) {
                activity?.let {
                    rewardedAdManager.showAdFor1Token(
                        activity = it,
                        onRewardEarned = {
                            currentTokens += 1
                            onTokenEarned()
                            Toast.makeText(context, "🎉 토큰 1개 획득!", Toast.LENGTH_SHORT).show()
                        },
                        onAdNotReady = {
                            Toast.makeText(context, "광고를 불러오는 중...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // 긴 광고 - 토큰 3개
            WatchAdCard(
                tokenColor = tokenColor,
                title = "긴 광고 보기",
                description = "30초 광고 시청",
                reward = 3
            ) {
                activity?.let {
                    rewardedAdManager.showAdFor3Tokens(
                        activity = it,
                        onRewardEarned = {
                            currentTokens += 3
                            onTokenEarned()
                            Toast.makeText(context, "🎉 토큰 3개 획득!", Toast.LENGTH_SHORT).show()
                        },
                        onAdNotReady = {
                            Toast.makeText(context, "광고를 불러오는 중...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // 캐릭터 상점
            Text("캐릭터", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // 기본 (닉네임 첫글자)
                item {
                    DefaultCharacterItem(
                        nickname = nickname,
                        isApplied = currentCharacter == null,
                        onApply = { applyCharacter(null) }
                    )
                }
                items(characters) { char ->
                    val isUnlocked = char.id in unlockedCharacters
                    val isApplied = currentCharacter == char.id
                    CharacterShopItem(
                        character = char,
                        isUnlocked = isUnlocked,
                        isApplied = isApplied,
                        canAfford = currentTokens >= char.price,
                        onPurchase = { pendingPurchase = char; showPurchaseDialog = true },
                        onApply = { applyCharacter(char.id) }
                    )
                }
            }

            // 배경색 상점
            Text("배경색", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // 기본 (DarkRed)
                item {
                    DefaultColorItem(
                        isApplied = currentColor == null,
                        onApply = { applyColor(null) }
                    )
                }
                items(colors) { color ->
                    val isUnlocked = color.id in unlockedColors
                    val isApplied = currentColor == color.id
                    ColorShopItem(
                        shopColor = color,
                        isUnlocked = isUnlocked,
                        isApplied = isApplied,
                        canAfford = currentTokens >= color.price,
                        onPurchase = { pendingPurchase = color; showPurchaseDialog = true },
                        onApply = { applyColor(color.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfilePreviewCard(
    nickname: String,
    character: String?,
    backgroundColor: String?,
    tokens: Int,
    isLoading: Boolean,
    tokenColor: Color
) {
    val bgColor = backgroundColor?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { DarkRed }
    } ?: DarkRed

    val animatedTokens by animateIntAsState(targetValue = tokens, animationSpec = tween(500), label = "")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 미리보기
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                if (character != null) {
                    val drawableRes = when (character) {
                        "lion" -> R.drawable.lion
                        "penguin" -> R.drawable.penguin
                        "redpanda" -> R.drawable.redpanda
                        "squirrel" -> R.drawable.squirrel
                        "cat" -> R.drawable.cat
                        "dog" -> R.drawable.dog
                        "fox" -> R.drawable.fox
                        "rabbit" -> R.drawable.rabbit
                        else -> null
                    }
                    drawableRes?.let {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(it).build(),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Text(
                        text = nickname.firstOrNull()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("내 프로필", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(nickname, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // 토큰 잔액
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(tokenColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MonetizationOn, null, tint = tokenColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("$animatedTokens", fontWeight = FontWeight.Bold, color = tokenColor)
                }
            }
        }
    }
}

@Composable
private fun WatchAdCard(
    tokenColor: Color,
    title: String,
    description: String,
    reward: Int,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = ""
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(tokenColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = tokenColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onClick,
                modifier = Modifier.scale(scale),
                colors = ButtonDefaults.buttonColors(containerColor = tokenColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("+$reward", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DefaultCharacterItem(nickname: String, isApplied: Boolean, onApply: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isApplied) DarkRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, if (isApplied) DarkRed else Color.Transparent, RoundedCornerShape(16.dp))
                .clickable { onApply() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(DarkRed),
                contentAlignment = Alignment.Center
            ) {
                Text(nickname.firstOrNull()?.toString() ?: "?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            if (isApplied) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).clip(CircleShape).background(DarkRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("기본", fontSize = 11.sp)
    }
}

@Composable
private fun CharacterShopItem(
    character: ShopCharacter,
    isUnlocked: Boolean,
    isApplied: Boolean,
    canAfford: Boolean,
    onPurchase: () -> Unit,
    onApply: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when {
                        isApplied -> DarkRed.copy(alpha = 0.1f)
                        !isUnlocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .border(2.dp, if (isApplied) DarkRed else Color.Transparent, RoundedCornerShape(16.dp))
                .clickable { if (isUnlocked) onApply() else if (canAfford) onPurchase() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(character.drawableRes).build(),
                contentDescription = character.name,
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit,
                alpha = if (isUnlocked) 1f else 0.4f
            )
            if (!isUnlocked) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            if (isApplied) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).clip(CircleShape).background(DarkRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (isUnlocked) {
            Text(character.name, fontSize = 11.sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonetizationOn, null, modifier = Modifier.size(12.dp), tint = Color(0xFFFFA000))
                Text("${character.price}", fontSize = 11.sp, color = if (canAfford) Color(0xFFFFA000) else Color.Gray)
            }
        }
    }
}

@Composable
private fun DefaultColorItem(isApplied: Boolean, onApply: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(DarkRed)
                .border(3.dp, if (isApplied) Color.Black else Color.Transparent, CircleShape)
                .clickable { onApply() },
            contentAlignment = Alignment.Center
        ) {
            if (isApplied) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("기본", fontSize = 11.sp)
    }
}

@Composable
private fun ColorShopItem(
    shopColor: ShopColor,
    isUnlocked: Boolean,
    isApplied: Boolean,
    canAfford: Boolean,
    onPurchase: () -> Unit,
    onApply: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isUnlocked) shopColor.color else shopColor.color.copy(alpha = 0.3f))
                .border(3.dp, if (isApplied) Color.Black else Color.Transparent, CircleShape)
                .clickable { if (isUnlocked) onApply() else if (canAfford) onPurchase() },
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(20.dp))
            } else if (isApplied) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (isUnlocked) {
            Text(shopColor.name, fontSize = 11.sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonetizationOn, null, modifier = Modifier.size(12.dp), tint = Color(0xFFFFA000))
                Text("${shopColor.price}", fontSize = 11.sp, color = if (canAfford) Color(0xFFFFA000) else Color.Gray)
            }
        }
    }
}
