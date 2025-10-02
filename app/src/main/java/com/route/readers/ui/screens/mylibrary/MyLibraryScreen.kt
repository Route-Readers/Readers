package com.route.readers.ui.screens.mylibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        books = firestoreRepository.getMyBooks()
        isLoading = false
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
            EmptyLibraryState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(books) { book ->
                    MyBookCard(
                        book = book,
                        onUpdateProgress = { isbn, page ->
                            scope.launch {
                                firestoreRepository.updateReadingProgress(isbn, page)
                                books = firestoreRepository.getMyBooks()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MyBookCard(
    book: MyBook,
    onUpdateProgress: (String, Int) -> Unit
) {
    var showProgressDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showProgressDialog = true },
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
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (book.totalPages > 0) {
                    // 진도율을 큰 텍스트로 표시
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${book.progressPercentage}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkRed
                        )
                        Text(
                            text = "%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkRed,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${book.currentPage}/${book.totalPages}p",
                            fontSize = 14.sp,
                            color = TextGray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 진도 바
                    LinearProgressIndicator(
                        progress = book.progressPercentage / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = ReadingGreen,
                        trackColor = CreamBackground
                    )
                } else {
                    Text(
                        text = "페이지 정보 없음",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "탭해서 진도 입력",
                        fontSize = 12.sp,
                        color = DarkRed
                    )
                }
            }
        }
    }

    if (showProgressDialog) {
        ProgressUpdateDialog(
            book = book,
            onDismiss = { showProgressDialog = false },
            onUpdate = { page ->
                onUpdateProgress(book.isbn, page)
                showProgressDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressUpdateDialog(
    book: MyBook,
    onDismiss: () -> Unit,
    onUpdate: (Int) -> Unit
) {
    var pageText by remember { mutableStateOf(book.currentPage.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("읽기 진도 업데이트") },
        text = {
            Column {
                Text("${book.title}")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { pageText = it },
                    label = { Text("현재 페이지") },
                    suffix = { 
                        if (book.totalPages > 0) {
                            Text("/ ${book.totalPages}")
                        } else {
                            Text("페이지")
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val page = pageText.toIntOrNull() ?: 0
                    if (book.totalPages > 0) {
                        if (page in 0..book.totalPages) {
                            onUpdate(page)
                        }
                    } else {
                        onUpdate(page)
                    }
                }
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

@Composable
fun EmptyLibraryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📚",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "서재가 비어있습니다",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkRed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "검색 탭에서 책을 추가해보세요",
                fontSize = 14.sp,
                color = TextGray
            )
        }
    }
}
