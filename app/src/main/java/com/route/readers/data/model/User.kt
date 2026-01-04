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
                val unlockedCharacters: List<String> = emptyList(),
                val unlockedColors: List<String> = emptyList(),
                val claimedAchievements: List<String> = emptyList(),

                val followers: List<String> = emptyList(),
                val following: List<String> = emptyList(),
                val followerCount: Long = 0,
                val followingCount: Long = 0,

                val friends: List<String> = emptyList(),

                val readBookCount: Long = 0,
                val totalPagesRead: Int = 0,
                val isCurrentlyReading: Boolean = false,
                val consecutiveDays: Int = 0,
                val consecutiveReadingDays: Int = 0,
                val totalReadingDays: Int = 0,
                val lastLoginDate: String = "",
                @PropertyName("private")
                val isPrivate: Boolean = false,

                val followAlarmEnabled: Boolean = true,
                val likeAlarmEnabled: Boolean = true,
                val friendReadingAlarmEnabled: Boolean = true,

                val readingTimeAlarmEnabled: Boolean = true,
                val messageAlarmEnabled: Boolean = true,
                val friendRequestAlarmEnabled: Boolean = true,
                val readingAlarmHour: Int = 20,
                val readingAlarmMinute: Int = 0,

                val fcmToken: String? = null,
                val phoneHash: String? = null,
                val role: String = "user",
                val isBanned: Boolean = false,
                val banReason: String? = null,
                val banExpiry: Long? = null,
                val warnings: Int = 0
)
