package com.route.readers.notification

import android.content.Context

object FollowNotificationHelper {
    
    fun sendFollowNotification(context: Context, followerName: String) {
        val notificationManager = ReadingNotificationManager(context)
        val title = "새로운 팔로워"
        val message = "${followerName}님이 당신을 팔로우하기 시작했습니다!"
        
        notificationManager.showReadingInvitation(title, message)
    }
}
