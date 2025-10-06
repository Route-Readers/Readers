package com.route.readers.ui.screens.bookclub

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.route.readers.ui.components.BookClubCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookClubScreen(
    viewModel: BookClubViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("북클럽") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "북클럽 만들기")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.bookClubs) { bookClub ->
                BookClubCard(
                    bookClub = bookClub,
                    onJoinClick = { viewModel.joinBookClub(bookClub.id) }
                )
            }
        }
    }
    
    if (uiState.showCreateDialog) {
        CreateBookClubDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { name, book -> viewModel.createBookClub(name, book) }
        )
    }
}

@Composable
fun CreateBookClubDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var clubName by remember { mutableStateOf("") }
    var bookTitle by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 북클럽 만들기") },
        text = {
            Column {
                OutlinedTextField(
                    value = clubName,
                    onValueChange = { clubName = it },
                    label = { Text("클럽 이름") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bookTitle,
                    onValueChange = { bookTitle = it },
                    label = { Text("읽을 책") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(clubName, bookTitle) },
                enabled = clubName.isNotBlank() && bookTitle.isNotBlank()
            ) {
                Text("만들기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
