package com.route.readers.ui.screens.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FeedViewModel : ViewModel() {
    private val _feedItems = MutableLiveData<List<FeedItem>>()
    val feedItems: LiveData<List<FeedItem>> = _feedItems
    
    init {
        loadFeedData()
    }
    
    private fun loadFeedData() {
        _feedItems.value = getMockFeedData()
    }
    
    private fun getMockFeedData(): List<FeedItem> {
        return listOf(
            FeedItem.Follow("김독서님이 김이삭님을 팔로우했어요!", "김독서", "김이삭", "1분 전"),
            FeedItem.ChallengeStart("김이삭님과 김독서님", "친구와 10일동안 100페이지 읽기 챌린지를 시작하셨어요!", "2분 전"),
            FeedItem.ReadingProgress("김독서", "데미안", 45, 200, "오늘 30분 독서했어요", "1시간 전"),
            FeedItem.ChallengeSuccess("김이삭님과 김독서님", "1주일동안 매일 책읽기 챌린지를 성공하셨습니다!", "3시간 전"),
            FeedItem.BookReview("이책좋아", "해리포터와 마법사의 돌", 5, "정말 재미있었어요! 마법의 세계가 생생했습니다.", "5시간 전")
        )
    }
}
