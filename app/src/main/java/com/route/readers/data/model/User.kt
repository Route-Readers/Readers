package com.route.readers.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class User(@DocumentId @get:Exclude
                val documentId: String = "",

                val uid: String = "",

                val nickname: String = "",
                val email: String? = null,
                val profileImageUrl: String? = null,
                val profileCharacter: String? = null,
                val profileBackgroundColor: String? = null,
                val bio: String? = null,
                val readingGenres: List<String> = emptyList(),
                val readingStyles: List<String> = emptyList(),
                val savedFeeds: List<String> = emptyList(),
                val blockedUsers: List<String> = emptyList(),
                val level: Int = 1,
                val title: String? = null,
                val titles: List<String> = emptyList(),
                val totalPoints: Int = 0,
                val tokens: Int = 0,
                val claimedAchievements: List<String> = emptyList(),

                val followers: List<String> = emptyList(),
                val following: List<String> = emptyList(),
                val followerCount: Long = 0,
                val followingCount: Long = 0,

                val readBookCount: Long = 0,
                val isCurrentlyReading: Boolean = false,
                val consecutiveDays: Int = 0,
                val consecutiveReadingDays: Int = 0,
                val totalReadingDays: Int = 0,
                val lastLoginDate: String = "",
                @PropertyName("private")
                val isPrivate: Boolean = false,

                val friendReadingAlarmEnabled: Boolean? = true,
                val followAlarmEnabled: Boolean? = true,
                val messageAlarmEnabled: Boolean? = true,
                val friendRequestAlarmEnabled: Boolean? = true,
                val likeAlarmEnabled: Boolean? = true
)
