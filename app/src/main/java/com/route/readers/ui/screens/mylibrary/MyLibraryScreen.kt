package com.route.readers.ui.screens.mylibrary

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.route.readers.R
import com.route.readers.data.model.MyBook
import com.route.readers.data.remote.MyLibraryRepository
import com.route.readers.data.remote.FirestoreRepository
import com.route.readers.ui.screens.attendance.AttendanceViewModel
import com.route.readers.ui.screens.profile.ProfileViewModel
import com.route.readers.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.Pair
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star

private enum class FilterState {
    ALL, READING, COMPLETED
}

@Composable
fun MyLibraryScreen(
    onBookSelected: (MyBook?) -> Unit = {},
    showProgressDialog: Boolean = false,
    onProgressDialogDismiss: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    attendanceViewModel: AttendanceViewModel,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val myLibraryRepository = remember { MyLibraryRepository() }
    var books by remember { mutableStateOf<List<MyBook>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showProgressDialogBook by remember { mutableStateOf<MyBook?>(null) }
    var showDeleteDialog by remember { mutableStateOf<MyBook?>(null) }
    var showPostToFeedDialog by remember { mutableStateOf<Pair<MyBook, Int>?>(null) }
    var selectedBook by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf(FilterState.ALL) }

    val filteredBooks by remember {
        derivedStateOf {
            when (selectedFilter) {
                FilterState.ALL -> books
                FilterState.READING -> books.filter { !it.isCompleted }
                FilterState.COMPLETED -> books.filter { it.isCompleted }
            }
        }
    }

    val firestoreRepository = remember { FirestoreRepository() }

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

    LaunchedEffect(showProgressDialog) {
        if (showProgressDialog) {
            val book = books.find { it.isbn == selectedBook }
            book?.let {
                showProgressDialogBook = it
                onProgressDialogDismiss()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "내 서재",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DarkRed
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = DarkRed)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("책 목록을 불러오는 중...", color = TextGray)
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
                        text = "📚",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "서재가 비어있습니다",
                        color = DarkRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "검색에서 책을 추가해보세요",
                        color = TextGray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onNavigateToSearch() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkRed),
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "책 추가하기",
                            tint = White
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        count = books.size,
                        label = "총 책 수",
                        isSelected = selectedFilter == FilterState.ALL,
                        onClick = { selectedFilter = FilterState.ALL }
                    )
                    FilterChip(
                        count = books.count { !it.isCompleted },
                        label = "읽는 중",
                        isSelected = selectedFilter == FilterState.READING,
                        onClick = { selectedFilter = FilterState.READING }
                    )
                    FilterChip(
                        count = books.count { it.isCompleted },
                        label = "완독",
                        isSelected = selectedFilter == FilterState.COMPLETED,
                        onClick = { selectedFilter = FilterState.COMPLETED }
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredBooks, key = { it.id }) { book ->
                    MyBookCard(
                        book = book,
                        isSelected = selectedBook == book.isbn,
                        onProgressClick = {
                            if (selectedBook == book.isbn) {
                                selectedBook = null
                                onBookSelected(null)
                            } else {
                                selectedBook = book.isbn
                                onBookSelected(book)
                            }
                        },
                        onDeleteClick = { showDeleteDialog = book }
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
                        val isCompleted = currentPage == book.totalPages && book.totalPages > 0
                        val success = myLibraryRepository.updateReadingProgress(book.isbn, currentPage, isCompleted)

                        if (success) {
                            if (isCompleted && !book.isCompleted) {
                                profileViewModel.onBookFinished()
                                Toast.makeText(context, "🎉 완독을 축하합니다! 업적이 업데이트되었습니다.", Toast.LENGTH_LONG).show()
                            }
                            refreshBooks()
                            attendanceViewModel.markReadingActivity()
                            if (isCompleted) {
                                showPostToFeedDialog = Pair(book, currentPage)
                            }
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
                    Log.d("MyLibraryScreen", "Deleting book: ${book.title}")
                    val success = myLibraryRepository.removeBookFromLibrary(book.isbn)
                    Log.d("MyLibraryScreen", "Delete result: $success")
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

    showPostToFeedDialog?.let { (book, currentPage) ->
        PostToFeedDialog(
            book = book,
            onDismiss = { showPostToFeedDialog = null },
            onPost = { rating, review ->
                scope.launch {
                    val progress = if (book.totalPages > 0) (currentPage.toFloat() / book.totalPages * 100).toInt() else 0
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
                        currentPage = currentPage,
                        progress = progress
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
}

@Composable
fun FilterChip(
    count: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) DarkRed else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "$count",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) DarkRed else Color.Black
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) DarkRed else TextGray
        )
    }
}


@Composable
fun MyBookCard(
    book: MyBook,
    isSelected: Boolean = false,
    onProgressClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProgressClick() }
            .then(
                if (isSelected) Modifier.border(
                    3.dp,
                    DarkRed,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> DarkRed.copy(alpha = 0.1f)
                book.isCompleted -> DarkRed.copy(alpha = 0.1f)
                book.currentPage > 0 -> ReadingGreen.copy(alpha = 0.1f)
                else -> White
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            AsyncImage(
                model = book.cover.ifEmpty { null },
                contentDescription = "책 표지",
                modifier = Modifier
                    .width(80.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop,
                error = painterResource(R.mipmap.readerslogo),
                placeholder = painterResource(R.mipmap.readerslogo)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.author,
                    fontSize = 14.sp,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(8.dp))

                val progressPercentage = book.progressPercentage

                Text(
                    text = if (book.isCompleted) "완독!" else "$progressPercentage%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        book.isCompleted -> DarkRed
                        progressPercentage == 0 -> TextGray
                        else -> ReadingGreen
                    }
                )

                LinearProgressIndicator(
                    progress = { if (book.isCompleted) 1f else progressPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (book.isCompleted) DarkRed else ReadingGreen,
                    trackColor = Color.LightGray.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${book.currentPage} / ${book.totalPages} 페이지",
                    fontSize = 12.sp,
                    color = TextGray
                )
            }

            IconButton(
                onClick = onDeleteClick
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = Color.Gray
                )
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
            Text("책을 서재에서 삭제하시겠습니까?")
        },
        text = {
            Text("'$bookTitle'을(를) 서재에서 삭제합니다.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("예", color = Color.Red)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DarkRed)
            ) {
                Text("아니오", color = White)
            }
        }
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
                Text("${book.title}")
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
                    Log.d("ProgressDialog", "Input: $currentPageText, Parsed: $page, Total: ${book.totalPages}")
                    if (page != null && page >= 0 && page <= book.totalPages) {
                        onUpdate(page)
                    } else {
                        isError = true
                        Log.w("ProgressDialog", "Invalid page number: $currentPageText")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkRed)
            ) {
                Text("업데이트")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("피드에 기록 남기기") },
        text = {
            Column {
                Text("『${book.title}』 읽기 진행 상황을 공유해보세요.")
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "$star",
                                tint = if (star <= rating) Color(0xFFFFD700) else Color.Gray
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
                onClick = { onPost(rating, review) },
                colors = ButtonDefaults.buttonColors(containerColor = DarkRed)
            ) {
                Text("포스팅")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("건너뛰기")
            }
        }
    )
}
