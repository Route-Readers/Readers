package com.route.readers.data.model

sealed class FeedItem {
    data class Follow(
        val message: String,
        val follower: String,
        val following: String,
        val timestamp: String
    ) : FeedItem()
    
    data class ChallengeStart(
        val participants: String,
        val message: String,
        val timestamp: String
    ) : FeedItem()
    
    data class ReadingProgress(
        val user: String,
        val bookTitle: String,
        val currentPage: Int,
        val totalPages: Int,
        val message: String,
        val timestamp: String
    ) : FeedItem()
    
    data class ChallengeSuccess(
        val participants: String,
        val message: String,
        val timestamp: String
    ) : FeedItem()
    
    data class BookReview(
        val user: String,
        val bookTitle: String,
        val rating: Int,
        val review: String,
        val timestamp: String
    ) : FeedItem()
}
