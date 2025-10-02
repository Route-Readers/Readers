package com.route.readers.ui.screens.mylibrary

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import coil.compose.AsyncImage
import com.route.readers.R
import com.route.readers.data.model.MyBook
import com.route.readers.data.remote.FirestoreRepository
import com.route.readers.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MyLibraryScreen(
    firestoreRepository: FirestoreRepository = FirestoreRepository()
) {
    var books by remember { mutableStateOf<List<MyBook>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showProgressDialog by remember { mutableStateOf<MyBook?>(null) }
    var showDeleteDialog by remember { mutableStateOf<MyBook?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 책 목록 새로고침 함수
    fun refreshBooks() {
        scope.launch {
            isLoading = true
            books = firestoreRepository.getMyBooks()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshBooks()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "내 서재",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkRed,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (books.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "서재가 비어있습니다\n검색에서 책을 추가해보세요",
                    color = TextGray,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(books) { book ->
                    MyBookCard(
                        book = book,
                        onProgressClick = { showProgressDialog = book },
                        onDeleteClick = { showDeleteDialog = book }
                    )
                }
            }
        }
    }

    // 진도 업데이트 다이얼로그
    showProgressDialog?.let { book ->
        ProgressUpdateDialog(
            book = book,
            onDismiss = { showProgressDialog = null },
            onUpdate = { currentPage ->
                scope.launch {
                    Log.d("MyLibraryScreen", "Updating progress: ${book.title} to page $currentPage")
                    val success = firestoreRepository.updateReadingProgress(book.isbn, currentPage)
                    Log.d("MyLibraryScreen", "Update result: $success")
                    if (success) {
                        refreshBooks() // 새로고침
                        Toast.makeText(context, "진도가 업데이트되었습니다", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "업데이트 실패. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                    }
                    showProgressDialog = null
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
                    val success = firestoreRepository.removeBookFromLibrary(book.isbn)
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
    onProgressClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProgressClick() },
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            AsyncImage(
                model = book.cover.ifEmpty { null },
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
                    color = DarkRed
                )
                
                LinearProgressIndicator(
                    progress = book.progressPercentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = ReadingGreen,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("읽은 페이지 업데이트") },
        text = {
            Column {
                Text("${book.title}")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentPageText,
                    onValueChange = { currentPageText = it },
                    label = { Text("현재 페이지") },
                    suffix = { Text("/ ${book.totalPages}") }
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
                        // 잘못된 입력에 대한 피드백 (여기서는 간단히 무시)
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
