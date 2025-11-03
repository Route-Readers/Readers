package com.route.readers.ui.screens.token

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TokenPackage(
    val tokens: Int,
    val price: String,
    val bonus: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenShopScreen(
    onNavigateBack: () -> Unit
) {
    val tokenPackages = listOf(
        TokenPackage(100, "990원"),
        TokenPackage(500, "4,900원", bonus = 50),
        TokenPackage(1000, "9,900원", bonus = 150),
        TokenPackage(3000, "29,900원", bonus = 500)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("토큰 상점") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "토큰 구매",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TokenPackageCard(
                        package_ = tokenPackages[0],
                        modifier = Modifier.weight(1f)
                    )
                    TokenPackageCard(
                        package_ = tokenPackages[1],
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TokenPackageCard(
                        package_ = tokenPackages[2],
                        modifier = Modifier.weight(1f)
                    )
                    TokenPackageCard(
                        package_ = tokenPackages[3],
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "무료로 토큰 받기",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                FreeTokenMethodCard(
                    icon = Icons.Default.CardGiftcard,
                    title = "출석 체크",
                    description = "7일 연속 출석 시 10토큰 지급",
                    tokenAmount = "+10"
                )
            }

            item {
                FreeTokenMethodCard(
                    icon = Icons.Default.EmojiEvents,
                    title = "챌린지 완료",
                    description = "독서 챌린지 완료 시 토큰 획득",
                    tokenAmount = "+20~50"
                )
            }

            item {
                FreeTokenMethodCard(
                    icon = Icons.Default.People,
                    title = "친구 초대",
                    description = "친구가 가입하면 30토큰 지급",
                    tokenAmount = "+30"
                )
            }
        }
    }
}

@Composable
fun TokenPackageCard(package_: TokenPackage, modifier: Modifier = Modifier) {
    val tokenColor = Color(0xFFFFA000)
    
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.MonetizationOn,
                contentDescription = null,
                tint = tokenColor,
                modifier = Modifier.size(40.dp)
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${package_.tokens}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokenColor
                )
                
                if (package_.bonus > 0) {
                    Text(
                        text = "+${package_.bonus}",
                        fontSize = 12.sp,
                        color = tokenColor.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = { /* TODO: 결제 로직 */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokenColor
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = package_.price,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun FreeTokenMethodCard(
    icon: ImageVector,
    title: String,
    description: String,
    tokenAmount: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = tokenAmount,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFA000)
            )
        }
    }
}
