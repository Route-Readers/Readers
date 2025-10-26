package com.route.readers.ui.screens.add_feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.route.readers.data.model.Book
import com.route.readers.data.remote.BookRepository // BookRepository를 import 합니다.
import com.route.readers.ui.screens.feed.FeedItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AddFeedUiState {
    data object Idle : AddFeedUiState()
    data object Loading : AddFeedUiState()
    data object Success : AddFeedUiState()
    data class Error(val message: String) : AddFeedUiState()
}

class AddFeedViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // 1. BookRepository 인스턴스를 다시 생성합니다.
    private val bookRepository = BookRepository()
    private var searchJob: Job? = null

    var uiState by mutableStateOf<AddFeedUiState>(AddFeedUiState.Idle)
        private set

    var searchText by mutableStateOf("")
        private set
    var searchedBooks by mutableStateOf<List<Book>>(emptyList())
        private set
    var selectedBook by mutableStateOf<Book?>(null)
        private set
    var isSearching by mutableStateOf(false)
        private set

    var reviewText by mutableStateOf("")
    var rating by mutableStateOf(0)

    fun onSearchTextChanged(text: String) {
        searchText = text
        if (text.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                delay(500L) // 사용자가 타이핑을 멈출 때까지 0.5초 대기
                isSearching = true
                searchedBooks = try {
                    // 2. BookRepository를 통해 실제 API를 호출하도록 코드를 복원합니다.
                    bookRepository.getBookSearch(query = text, maxResults = 10)
                } catch (e: Exception) {
                    // API 호출 중 오류 발생 시 빈 리스트를 반환합니다.
                    emptyList()
                } finally {
                    isSearching = false
                }
            }
        } else {
            searchedBooks = emptyList()
        }
    }

    fun onBookSelected(book: Book) {
        selectedBook = book
        searchedBooks = emptyList()
        searchText = book.title
    }

    fun clearSelectedBook() {
        selectedBook = null
        searchText = ""
        reviewText = ""
        rating = 0
    }

    fun submitFeed() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            uiState = AddFeedUiState.Error("로그인이 필요합니다.")
            return
        }

        val bookToSave = selectedBook
        if (bookToSave == null) {
            uiState = AddFeedUiState.Error("리뷰를 작성할 책을 선택해주세요.")
            return
        }
        if (reviewText.isBlank()) {
            uiState = AddFeedUiState.Error("리뷰 내용을 입력해주세요.")
            return
        }
        if (rating == 0) {
            uiState = AddFeedUiState.Error("별점을 매겨주세요.")
            return
        }

        uiState = AddFeedUiState.Loading

        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(currentUserId).get().await()
                val currentUserNickname = userDoc.getString("nickname") ?: "익명"

                val feedRef = db.collection("feeds").document()

                val newFeed = FeedItem.BookReview(
                    id = feedRef.id,
                    authorId = currentUserId,
                    userName = currentUserNickname,
                    book = bookToSave, // 수정된 대로 Book 객체를 통째로 저장합니다.
                    review = reviewText,
                    rating = rating,
                    timestamp = com.google.firebase.Timestamp.now(),
                    likeCount = 0,
                    commentCount = 0,
                    likedBy = emptyList()
                )

                feedRef.set(newFeed).await()
                uiState = AddFeedUiState.Success
            } catch (e: Exception) {
                uiState = AddFeedUiState.Error("피드 저장에 실패했습니다: ${e.message}")
            }
        }
    }
}
