import {onDocumentCreated, onDocumentUpdated} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import {getMessaging} from "firebase-admin/messaging";

// Firebase Admin SDK 초기화
admin.initializeApp();

// "fcmRequests" 컬렉션에 새 문서가 생성될 때마다 이 함수가 실행됨
export const sendFcmNotification = onDocumentCreated(
    {
        document: "fcmRequests/{requestId}", // <-- 올바른 컬렉션 이름으로 수정
        region: "asia-northeast3",
    },
    async (event) => {
        const snapshot = event.data;
        if (!snapshot) {
            console.log("No data associated with the event");
            return;
        }

        // 1. 요청 데이터 가져오기
        const requestData = snapshot.data();
        // NotificationRepository에서 보낸 필드 이름 사용
        const {targetUserId, title, message: body, notificationType} = requestData;

        console.log(`New FCM request. Type: ${notificationType}`);

        if (!targetUserId || !title || !body) {
            console.error("Request data is missing fields", requestData);
            return;
        }

        try {
            const db = admin.firestore();

            // 2. 받는 사람(Receiver)의 FCM 토큰 및 알림 설정 가져오기
            const receiverDoc = await db.collection("users").doc(targetUserId).get();
            const receiverData = receiverDoc.data();
            const fcmToken = receiverData?.fcmToken;

            if (!fcmToken) {
                console.log(`No FCM token for receiver ${targetUserId}`);
                // 처리 완료된 요청 문서는 삭제
                await snapshot.ref.delete();
                return;
            }

            // 3. 알림 유형에 따른 설정 확인
            let shouldSendNotification = true;

            if (notificationType === "READING_INVITATION") {
                const friendReadingAlarmEnabled = receiverData?.friendReadingAlarmEnabled;
                console.log(`User ${targetUserId} friendReadingAlarmEnabled: ${friendReadingAlarmEnabled}`);
                if (friendReadingAlarmEnabled === false) {
                    console.log(`User ${targetUserId} has disabled friend reading alarms. Skipping notification.`);
                    shouldSendNotification = false;
                }
            } else if (notificationType === "FOLLOW" || notificationType === "FOLLOW_REQUEST") {
                const followAlarmEnabled = receiverData?.followAlarmEnabled; // Use the field from User.kt
                console.log(`User ${targetUserId} followAlarmEnabled: ${followAlarmEnabled}`);
                if (followAlarmEnabled === false) {
                    console.log(`User ${targetUserId} has disabled follow alarms. Skipping notification.`);
                    shouldSendNotification = false;
                }
            } else if (notificationType === "LIKE") {
                const likeAlarmEnabled = receiverData?.likeAlarmEnabled;
                console.log(`User ${targetUserId} likeAlarmEnabled: ${likeAlarmEnabled}`);
                if (likeAlarmEnabled === false) {
                    console.log(`User ${targetUserId} has disabled like alarms. Skipping notification.`);
                    shouldSendNotification = false;
                }
            }

            if (!shouldSendNotification) {
                await snapshot.ref.delete();
                return;
            }

            // 4. 알림 메시지(Payload) 구성
            const fcmMessage = {
                notification: {
                    title: title,
                    body: body,
                },
                data: { // Add data payload for custom handling on client
                    notificationType: String(notificationType),
                    // Add other relevant data if needed, e.g., senderId
                },
                token: fcmToken,
            };

            // 5. FCM으로 메시지 전송
            console.log(`Sending notification to token: ${fcmToken}`);
            await getMessaging().send(fcmMessage);
            console.log("Successfully sent message");

            // 6. 처리 완료된 요청 문서는 삭제
            await snapshot.ref.delete();

        } catch (error) {
            console.error("Error sending notification:", error);
            // 오류가 발생해도 요청 문서는 삭제하여 재시도를 방지
            try {
                await snapshot.ref.delete();
            } catch (deleteError) {
                console.error("Error deleting request document after error:", deleteError);
            }
        }
    });

export const onLikeCreated = onDocumentUpdated(
    {
        document: "feeds/{feedId}",
        region: "asia-northeast3",
    },
    async (event) => {
        const beforeData = event.data?.before.data();
        const afterData = event.data?.after.data();

        if (!beforeData || !afterData) {
            console.log("No data before or after for the event.");
            return;
        }

        const beforeLikedBy: string[] = beforeData.likedBy || [];
        const afterLikedBy: string[] = afterData.likedBy || [];

        // Determine if a new like was added
        const newLikes = afterLikedBy.filter(
            (userId) => !beforeLikedBy.includes(userId)
        );

        if (newLikes.length === 0) {
            console.log("No new likes detected.");
            return;
        }

        const likerId = newLikes[0]; // Assuming only one new like at a time
        const feedOwnerId = afterData.authorId;
        const feedTitle = afterData.title || afterData.reviewContent?.substring(0, 50) + "..."; // Get title or snippet

        if (likerId === feedOwnerId) {
            console.log("Liker is the feed owner. Skipping notification.");
            return;
        }

        try {
            const db = admin.firestore();

            // Get liker's display name
            const likerDoc = await db.collection("users").doc(likerId).get();
            const likerData = likerDoc.data();
            const likerDisplayName = likerData?.nickname || "Someone";

            // Create a request in fcmRequests collection
            await db.collection("fcmRequests").add({
                targetUserId: feedOwnerId,
                title: "새로운 좋아요!",
                message: `${likerDisplayName}님이 회원님의 글을 좋아합니다: ${feedTitle}`,
                notificationType: "LIKE",
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
            });

            console.log(`FCM request created for new like on feed ${event.params.feedId}`);

        } catch (error) {
            console.error("Error processing like event:", error);
        }
    }
);

