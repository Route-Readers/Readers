package com.route.readers.ui.screens.mylibrary

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.R
import com.route.readers.data.model.MyBook
import com.route.readers.data.model.Book
import com.route.readers.data.remote.MyLibraryRepository
import com.route.readers.data.remote.FirestoreRepository
import com.route.readers.ui.screens.attendance.AttendanceViewModel
import com.route.readers.ui.screens.profile.ProfileViewModel
import com.route.readers.ui.theme.*
import kotlinx.coroutines.launch
import com.route.readers.ui.screens.reading.TimerState
import com.route.readers.ui.screens.feed.FeedItem
import kotlin.Pair
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star

private enum class FilterState {
    ALL, READING, COMPLETED
}

@Composable
fun MyLibraryScreen(
    onNavigateToSearch: () -> Unit,
    attendanceViewModel: AttendanceViewModel,
    mainViewModel: com.route.readers.ui.screens.MainViewModel = viewModel(), // 새로 추가된 파라미터
    profileViewModel: ProfileViewModel = viewModel(),
    onBookSelected: (MyBook?) -> Unit = {},
    bookToUpdate: MyBook?,
    onUpdateFinished: () -> Unit,
    lastReadingSessionDuration: Int? = null,
    showFinishReadingDialogBook: MyBook?,
    onDismissFinishReadingDialog: () -> Unit
) {
    val context = LocalContext.current
    val myLibraryRepository = remember { MyLibraryRepository() }
    val firestoreRepository = remember { FirestoreRepository() }
    var books by remember { mutableStateOf<List<MyBook>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showProgressDialogBook by remember { mutableStateOf<MyBook?>(null) }
    var showDeleteDialog by remember { mutableStateOf<MyBook?>(null) }
    var showPostToFeedDialog by remember { mutableStateOf<Pair<MyBook, Int>?>(null) }
    var showTimerCompletedDialog by remember { mutableStateOf<MyBook?>(null) }
    var selectedBook by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf(FilterState.READING) }

    val filteredBooks by remember {
        derivedStateOf {
            when (selectedFilter) {
                FilterState.ALL -> books
                FilterState.READING -> books.filter { !it.isCompleted }
                FilterState.COMPLETED -> books.filter { it.isCompleted }
            }
        }
    }

    fun refreshBooks() {
        scope.launch {
            isLoading = true
            try {
                books = myLibraryRepository.getMyBooks()
                Log.d("MyLibraryScreen", "Books loaded: ${books.size}")
            } catch (e: Exception) {
                Log.e("MyLibraryScreen", "Error loading books: ${e.message}", e)
                Toast.makeText(context, "책 목록을 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshBooks()
        attendanceViewModel.checkAttendance()
    }

    // 타이머 완료 감지 로직
    LaunchedEffect(books) {
        books.forEach { book ->
            if (TimerState.isCompleted(book.isbn)) {
                showTimerCompletedDialog = book
                TimerState.clearCompleted(book.isbn)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 24.dp, end = 24.dp, top = 24.dp) // Increased padding
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "내 서재",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        if (isLoading) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(5) {
                    ShimmerBookCard()
                }
            }
        } else if (books.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📖",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "서재가 비어있습니다",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "새로운 이야기를 만나보세요",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { onNavigateToSearch() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "책 추가하기",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // Premium Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilterTab(
                    count = books.size,
                    label = "전체",
                    isSelected = selectedFilter == FilterState.ALL,
                    onClick = { selectedFilter = FilterState.ALL },
                    modifier = Modifier.weight(1f)
                )
                FilterTab(
                    count = books.count { !it.isCompleted },
                    label = "읽는 중",
                    isSelected = selectedFilter == FilterState.READING,
                    onClick = { selectedFilter = FilterState.READING },
                    modifier = Modifier.weight(1f)
                )
                FilterTab(
                    count = books.count { it.isCompleted },
                    label = "완독",
                    isSelected = selectedFilter == FilterState.COMPLETED,
                    onClick = { selectedFilter = FilterState.COMPLETED },
                    modifier = Modifier.weight(1f)
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp), // Increased spacing
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredBooks, key = { it.isbn }) { book ->
                    MyBookCard(
                        book = book,
                        isSelected = selectedBook == book.isbn,
                        showTimer = false,
                        timerSeconds = 0,
                        onProgressClick = {
                            if (selectedBook == book.isbn) {
                                selectedBook = null
                                onBookSelected(null)
                            } else {
                                selectedBook = book.isbn
                                onBookSelected(book)
                            }
                        },
                        onDeleteClick = { showDeleteDialog = book },
                        onUpdateClick = { showProgressDialogBook = book }
                    )
                }
            }
        }
    }

    showProgressDialogBook?.let { book ->
        ProgressUpdateDialog(
            book = book,
            onDismiss = { showProgressDialogBook = null },
            onUpdate = { currentPage ->
                scope.launch {
                    if (currentPage >= book.currentPage) {
                        val result = mainViewModel.saveReadingSession(
                            book = book,
                            newCurrentPage = currentPage,
                            durationInSeconds = lastReadingSessionDuration ?: 0 // durationInSeconds 전달
                        )

                        if (result != null) {
                            val (updatedBook, pagesReadThisSession) = result
                            if (updatedBook.isCompleted && !book.isCompleted) {
                                firestoreRepository.markBookAsRead(book.isbn)
                                Toast.makeText(context, "완독을 축하합니다!", Toast.LENGTH_LONG).show()
                            }
                            refreshBooks()
                            attendanceViewModel.markReadingActivity()
                            showPostToFeedDialog = Pair(updatedBook, pagesReadThisSession)
                        } else {
                            Toast.makeText(context, "업데이트 실패. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "이전보다 높은 페이지를 입력해주세요", Toast.LENGTH_SHORT).show()
                    }
                    showProgressDialogBook = null
                }
            }
        )
    }

    showDeleteDialog?.let { book ->
        DeleteConfirmDialog(
            bookTitle = book.title,
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                scope.launch {
                    val success = myLibraryRepository.removeBookFromLibrary(book.isbn)
                    if (success) {
                        refreshBooks()
                        Toast.makeText(context, "책이 삭제되었습니다", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "삭제 실패. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                    }
                    showDeleteDialog = null
                }
            }
        )
    }

    showPostToFeedDialog?.let { (book, pagesReadThisSession) ->
        PostToFeedDialog(
            book = book,
            onDismiss = { showPostToFeedDialog = null },
            onPost = { rating, review ->
                scope.launch {
                    val feedItem = com.route.readers.ui.screens.feed.FeedItem.BookReview(
                        book = com.route.readers.data.model.Book(
                            title = book.title,
                            author = book.author,
                            description = "",
                            isbn = book.isbn,
                            cover = book.cover,
                        ),
                        review = review,
                        rating = rating,
                        currentPage = pagesReadThisSession,
                        progress = book.progressPercentage
                    )
                    val postSuccess = firestoreRepository.addFeedItem(feedItem)
                    if (postSuccess) {
                        Toast.makeText(context, "피드에 기록되었습니다", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "피드 기록에 실패했습니다", Toast.LENGTH_SHORT).show()
                    }
                    showPostToFeedDialog = null
                }
            }
        )
    }

    showFinishReadingDialogBook?.let { book ->
        FinishReadingDialog(
            book = book,
            onDismiss = { onDismissFinishReadingDialog() },
            onConfirm = { finalPage ->
                scope.launch {
                    val result = mainViewModel.saveReadingSession(
                        book = book,
                        newCurrentPage = finalPage,
                        durationInSeconds = lastReadingSessionDuration ?: 0
                    )

                    if (result != null) {
                        val (updatedBook, pagesReadThisSession) = result
                        
                        if (updatedBook.isCompleted && !book.isCompleted) {
                            firestoreRepository.markBookAsRead(book.isbn)
                            Toast.makeText(context, "완독을 축하합니다!", Toast.LENGTH_LONG).show()
                        }
                        
                        refreshBooks()
                        attendanceViewModel.markReadingActivity()
                        showPostToFeedDialog = Pair(updatedBook, pagesReadThisSession)
                    } else {
                        Toast.makeText(context, "업데이트 실패. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                    }
                    onDismissFinishReadingDialog()
                }
            }
        )
    }

    // 타이머 완료 후 평가 다이얼로그
    showTimerCompletedDialog?.let { book ->
        PostToFeedDialog(
            book = book,
            onDismiss = { showTimerCompletedDialog = null },
            onPost = { rating, review ->
                scope.launch {
                    val bookObj = Book(
                        title = book.title,
                        author = book.author,
                        isbn = book.isbn,
                        cover = book.cover,
                        currentPage = book.currentPage,
                        totalPages = book.totalPages,
                        progress = if (book.totalPages > 0) (book.currentPage * 100) / book.totalPages else 0,
                        isCompleted = book.isCompleted
                    )
                    val feedItem = FeedItem.BookReview(
                        book = bookObj,
                        bookTitle = book.title,
                        review = review,
                        rating = rating,
                        currentPage = book.currentPage,
                        progress = if (book.totalPages > 0) (book.currentPage * 100) / book.totalPages else 0
                    )
                    val success = firestoreRepository.addFeedItem(feedItem)
                    if (success) {
                        Toast.makeText(context, "피드에 기록되었습니다", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "피드 기록에 실패했습니다", Toast.LENGTH_SHORT).show()
                    }
                    showTimerCompletedDialog = null
                }
            }
        )
    }
}

@Composable
fun FilterTab(
    count: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    
    // Add a shadow only when selected for the "lifted tab" effect
    val shadowElevation = if (isSelected) 2.dp else 0.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .shadow(elevation = shadowElevation, shape = RoundedCornerShape(12.dp), clip = false)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "$count",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun MyBookCard(
    book: MyBook,
    isSelected: Boolean = false,
    showTimer: Boolean = false,
    timerSeconds: Int = 0,
    onProgressClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUpdateClick: () -> Unit = {}
) {
    val elevation by animateDpAsState(
        if (isSelected) 8.dp else 2.dp,
        label = "elevation"
    )
    val scale by animateFloatAsState(
        if (isSelected) 1.02f else 1.0f,
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(16.dp),
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            .clickable { onProgressClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Book Cover with Depth Effect
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(120.dp)
                        .shadow(6.dp, RoundedCornerShape(4.dp))
                ) {
                    AsyncImage(
                        model = book.cover.ifEmpty { null },
                        contentDescription = "책 표지",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.mipmap.readerslogo),
                        placeholder = painterResource(R.mipmap.readerslogo)
                    )
                    // Spine shadow effect (left side)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(4.dp)
                            .align(Alignment.CenterStart)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    // Border for definition
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = book.title,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 24.sp
                        ),
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = book.author,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val progressPercentage = book.progressPercentage
                    val progressColor = MaterialTheme.colorScheme.primary
                    
                    Row(verticalAlignment = Alignment.Bottom) {
                         Text(
                            text = if (book.isCompleted) "완독" else "$progressPercentage",
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                color = progressColor
                            )
                        )
                        if (!book.isCompleted) {
                            Text(
                                text = "%",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = progressColor,
                                    baselineShift = androidx.compose.ui.text.style.BaselineShift(0.2f)
                                ),
                                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDeleteClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            // Elegant Progress Bar
            Spacer(modifier = Modifier.height(20.dp))
            
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                 // Progress fill
                 Box(
                    modifier = Modifier
                        .fillMaxWidth(if (book.isCompleted) 1f else book.progressPercentage / 100f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                 Text(
                    text = "${book.currentPage} 페이지",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                    )
                )
                Text(
                    text = "${book.totalPages} 페이지",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                    )
                )
            }
            
            if (showTimer) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Text(
                            text = "⏱️",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format("%02d:%02d", timerSeconds / 60, timerSeconds % 60),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    
                    Button(
                        onClick = onUpdateClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("기록하기", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    bookTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("책을 서재에서 삭제하시겠습니까?", color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Text("'$bookTitle'을(를) 서재에서 삭제합니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("예", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("아니오", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressUpdateDialog(
    book: MyBook,
    onDismiss: () -> Unit,
    onUpdate: (currentPage: Int) -> Unit
) {
    var currentPageText by remember { mutableStateOf(book.currentPage.toString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("읽은 페이지 업데이트") },
        text = {
            Column {
                Text("『${book.title}』")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentPageText,
                    onValueChange = {
                        currentPageText = it.filter { char -> char.isDigit() }
                        isError = false
                    },
                    label = { Text("현재 페이지") },
                    suffix = { Text("/ ${book.totalPages}") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("0부터 ${book.totalPages} 사이의 숫자를 입력해주세요", color = MaterialTheme.colorScheme.error) }
                    } else null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val page = currentPageText.toIntOrNull()
                    if (page != null && page >= 0 && page <= book.totalPages) {
                        onUpdate(page)
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("업데이트")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostToFeedDialog(
    book: MyBook,
    onDismiss: () -> Unit,
    onPost: (rating: Int, review: String) -> Unit
) {
    var rating by remember { mutableStateOf(0) }
    var review by remember { mutableStateOf("") }
    val starColor = Color(0xFFFFD700)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("피드에 기록 남기기") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = book.getHighQualityImageUrl(),
                    contentDescription = "책 표지",
                    modifier = Modifier
                        .width(80.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.mipmap.readerslogo),
                    placeholder = painterResource(R.mipmap.readerslogo)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "『${book.title}』",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${book.currentPage} / ${book.totalPages} 페이지",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "$star",
                                tint = if (star <= rating) starColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("한줄평 (선택 사항)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPost(rating, review)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("포스팅")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("건너뛰기")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishReadingDialog(
    book: MyBook,
    onDismiss: () -> Unit,
    onConfirm: (finalPage: Int) -> Unit
) {
    var currentPageText by remember { mutableStateOf(book.currentPage.toString()) }
    var isError by remember { mutableStateOf(false) }
    val errorMessage = "현재 페이지(${book.currentPage})와 전체 페이지(${book.totalPages}) 사이의 숫자를 입력해주세요"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("독서 종료") },
        text = {
            Column {
                Text("『${book.title}』 독서를 마칩니다.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("어디까지 읽으셨나요?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentPageText,
                    onValueChange = {
                        currentPageText = it.filter { char -> char.isDigit() }
                        isError = false
                    },
                    label = { Text("읽은 페이지") },
                    suffix = { Text("/ ${book.totalPages}") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                    } else null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val page = currentPageText.toIntOrNull()
                    if (page != null && page >= book.currentPage && page <= book.totalPages) {
                        onConfirm(page)
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("완료")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun ShimmerBookCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Book Cover Placeholder
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.width(20.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Title Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Author Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress Percentage Placeholder
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }

            // Progress Bar Placeholder
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ),
        label = "shimmer_offset"
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val highlightColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                baseColor,
                highlightColor,
                baseColor,
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
        .onGloballyPositioned {
            size = it.size
        }
}
