package com.route.readers.ui.screens.mylibrary

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.route.readers.R
import com.route.readers.data.model.MyBook
import com.route.readers.data.remote.MyLibraryRepository
import com.route.readers.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MyLibraryScreen(
    onBookSelected: (MyBook?) -> Unit = {},
    showProgressDialog: Boolean = false,
    onProgressDialogDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val myLibraryRepository = remember { MyLibraryRepository() }
    var books by remember { mutableStateOf<List<MyBook>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showProgressDialogBook by remember { mutableStateOf<MyBook?>(null) }
    var showDeleteDialog by remember { mutableStateOf<MyBook?>(null) }
    var selectedBook by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 외부에서 다이얼로그 표시 요청이 있을 때 처리
    LaunchedEffect(showProgressDialog) {
        if (showProgressDialog) {
            val book = books.find { it.isbn == selectedBook }
            book?.let {
                showProgressDialogBook = it
                onProgressDialogDismiss()
            }
        }
    }

    // 책 목록 새로고침 함수
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

    // 화면 진입 시 책 목록 로드
    LaunchedEffect(Unit) {
        refreshBooks()
    }

    // 주기적으로 새로고침 (검색에서 추가된 책을 반영하기 위해)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000) // 5초마다 새로고침
            if (!isLoading) {
                scope.launch {
                    val newBooks = myLibraryRepository.getMyBooks()
                    if (newBooks.size != books.size) {
                        books = newBooks
                        Log.d("MyLibraryScreen", "Books updated: ${books.size}")
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(16.dp)
    ) {
        // 헤더
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
            
            IconButton(
                onClick = { refreshBooks() },
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "새로고침",
                    tint = if (isLoading) Color.Gray else DarkRed
                )
            }
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
                }
            }
        } else {
            // 통계 정보
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
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${books.size}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkRed
                        )
                        Text("총 책 수", fontSize = 12.sp, color = TextGray)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val readingBooks = books.count { it.currentPage > 0 && it.currentPage < it.totalPages }
                        Text(
                            text = "$readingBooks",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReadingGreen
                        )
                        Text("읽는 중", fontSize = 12.sp, color = TextGray)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val completedBooks = books.count { it.currentPage >= it.totalPages }
                        Text(
                            text = "$completedBooks",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkRed
                        )
                        Text("완독", fontSize = 12.sp, color = TextGray)
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(books) { book ->
                    MyBookCard(
                        book = book,
                        isSelected = selectedBook == book.isbn,
                        onProgressClick = { 
                            if (selectedBook == book.isbn) {
                                // 이미 선택된 책을 다시 클릭하면 선택 해제
                                selectedBook = null
                                onBookSelected(null)
                            } else {
                                // 새로운 책 선택
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

    // 진도 업데이트 다이얼로그
    showProgressDialogBook?.let { book ->
        ProgressUpdateDialog(
            book = book,
            onDismiss = { showProgressDialogBook = null },
            onUpdate = { currentPage ->
                scope.launch {
                    Log.d("MyLibraryScreen", "Updating progress: ${book.title} to page $currentPage")
                    val success = myLibraryRepository.updateReadingProgress(book.isbn, currentPage)
                    Log.d("MyLibraryScreen", "Update result: $success")
                    if (success) {
                        refreshBooks() // 새로고침
                        Toast.makeText(context, "진도가 업데이트되었습니다", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "업데이트 실패. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                    }
                    showProgressDialogBook = null
                }
            }
        )
    }

    // 삭제 확인 다이얼로그
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
                        refreshBooks() // 새로고침
                        Toast.makeText(context, "책이 삭제되었습니다", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "삭제 실패. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                    }
                    showDeleteDialog = null
                }
            }
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
                book.currentPage > 0 && book.currentPage < book.totalPages -> ReadingGreen.copy(alpha = 0.1f)
                else -> White
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            AsyncImage(
                model = book.getHighQualityImageUrl().ifEmpty { null },
                contentDescription = "책 표지",
                modifier = Modifier
                    .size(80.dp, 100.dp)
                    .background(ReadingGreen, RoundedCornerShape(8.dp)),
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
                
                Text(
                    text = "${book.progressPercentage}%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        book.progressPercentage == 0 -> TextGray
                        book.progressPercentage == 100 -> DarkRed
                        else -> ReadingGreen
                    }
                )
                
                LinearProgressIndicator(
                    progress = book.progressPercentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = when {
                        book.progressPercentage == 100 -> DarkRed
                        else -> ReadingGreen
                    },
                    trackColor = Color.LightGray
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${book.currentPage} / ${book.totalPages} 페이지",
                    fontSize = 12.sp,
                    color = TextGray
                )
            }

            // 삭제 버튼
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
    onUpdate: (Int) -> Unit
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
                        currentPageText = it
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
