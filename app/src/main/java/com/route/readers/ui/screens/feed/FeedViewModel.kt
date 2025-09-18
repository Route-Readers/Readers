package com.route.readers.ui.screens.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.route.readers.data.model.*

class FeedViewModel : ViewModel() {
    private val _feedItems = MutableLiveData<List<FeedItem>>()
    val feedItems: LiveData<List<FeedItem>> = _feedItems
    
    init {
        loadFeedData()
    }
    
    private fun loadFeedData() {
        // 실제 구현에서는 API 호출이나 데이터베이스 조회
        _feedItems.value = getMockFeedData()
    }
    
    private fun getMockFeedData(): List<FeedItem> {
        return listOf(
            FeedItem(
                user = User("1", "김독서", isCurrentlyReading = true),
                book = Book("1", "해리포터와 마법사의 돌", "J.K. 롤링"),
                currentPage = 150,
                progressPercent = 45
            ),
            FeedItem(
                user = User("2", "박책벌레", isCurrentlyReading = false),
                book = Book("2", "1984", "조지 오웰"),
                currentPage = 328,
                progressPercent = 100,
                isCompleted = true
            ),
            FeedItem(
                user = User("3", "이문학", isCurrentlyReading = true),
                book = Book("3", "코스모스", "칼 세이건"),
                currentPage = 89,
                progressPercent = 23
            )
        )
    }
}
