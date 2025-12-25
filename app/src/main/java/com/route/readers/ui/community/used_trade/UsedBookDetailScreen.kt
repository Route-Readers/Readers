import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.route.readers.data.model.UsedBook
import com.route.readers.ui.community.used_trade.UsedBookTradeViewModel
import com.route.readers.ui.theme.DarkRed

import com.route.readers.ui.components.UserProfileImage
import com.route.readers.data.model.User
import com.route.readers.ui.community.used_trade.BuyResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsedBookDetailScreen(
    bookId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit
) {
    val viewModel: UsedBookTradeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val buyResult by viewModel.buyResult.collectAsState()
    val book = uiState.books.find { it.id == bookId }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isMyBook = book?.sellerId == currentUserId
    var showBuyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf<BuyResult?>(null) }

    // 구매 결과 처리
    LaunchedEffect(buyResult) {
        buyResult?.let {
            showResultDialog = it
            viewModel.clearBuyResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("중고책 상세") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Scaffod의 기본 인셋 제거
    ) { padding ->
        if (uiState.books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DarkRed)
            }
        } else if (book == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("책을 찾을 수 없습니다")
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 150.dp) // 하단 버튼 공간 충분히 확보
                ) {
                    AsyncImage(
                        model = book.bookCover,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column {
                            Text(
                                text = book.bookTitle,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = book.bookAuthor,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "가격",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${book.price} P",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkRed
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "상태",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = book.condition,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 임시 User 객체 생성하여 UserProfileImage 사용
                                // 실제 앱에서는 UsedBook 모델에 프로필 캐릭터, 배경색 정보도 포함하는 것이 좋음
                                // 현재는 이미지만 있는 경우 이미지, 없으면 기본 처리가 되므로
                                // UserProfileImage가 이미지를 우선적으로 처리하도록 함.
                                val sellerUser = User(
                                    uid = book.sellerId,
                                    nickname = book.sellerName,
                                    profileImageUrl = book.sellerProfileImage
                                )
                                UserProfileImage(user = sellerUser, size = 48.dp)

                                Column {
                                    Text(
                                        text = "판매자",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = book.sellerName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (book.description.isNotBlank()) {
                            Column {
                                Text(
                                    text = "설명",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = book.description,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                if (!isMyBook) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background) // 배경색 추가
                            .navigationBarsPadding() // 시스템 내비게이션 바 패딩 추가
                    ) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToChat(book.sellerId, book.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("채팅하기", fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = { showBuyDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DarkRed,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("구매하기", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .navigationBarsPadding() // 시스템 내비게이션 바 패딩 추가
                    ) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Button(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("삭제하기", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBuyDialog) {
        AlertDialog(
            onDismissRequest = { showBuyDialog = false },
            title = { Text("구매 확인") },
            text = { Text("${book?.price} P로 이 책을 구매하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        book?.let {
                            viewModel.buyBook(it.id, currentUserId)
                        }
                        showBuyDialog = false
                    }
                ) {
                    Text("구매", color = DarkRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBuyDialog = false }) {
                    Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    // 구매 결과 다이얼로그
    showResultDialog?.let { result ->
        AlertDialog(
            onDismissRequest = { showResultDialog = null },
            title = { Text(when (result) {
                is BuyResult.Success -> "구매 완료"
                is BuyResult.InsufficientTokens -> "토큰 부족"
                is BuyResult.Error -> "오류"
            }) },
            text = { Text(when (result) {
                is BuyResult.Success -> "구매가 완료되었습니다!"
                is BuyResult.InsufficientTokens -> "토큰이 부족합니다.\n현재: ${result.current}P / 필요: ${result.required}P"
                is BuyResult.Error -> result.message
            }) },
            confirmButton = {
                TextButton(onClick = {
                    showResultDialog = null
                    if (result is BuyResult.Success) onNavigateBack()
                }) {
                    Text("확인", color = DarkRed)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("삭제 확인") },
            text = { Text("이 게시글을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        book?.let {
                            viewModel.deleteBook(it.id)
                        }
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
