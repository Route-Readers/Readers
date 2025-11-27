import {onDocumentCreated, onDocumentUpdated} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import {getMessaging} from "firebase-admin/messaging";

// Dummy comment to force redeployment (2025-11-27 - second attempt)

// Firebase Admin SDK 초기화
admin.initializeApp();

// "fcmRequests" 컬렉션에 새 문서가 생성될 때마다 이 함수가 실행됨
export const sendFcmNotification = onDocumentCreated(
    {
        document: "fcmRequests/{requestId}",
        region: "asia-northeast3",
    },
    async (event) => {
        console.log(`[sendFcmNotification] Triggered for request ID: ${event.params.requestId}`);
        const snapshot = event.data;
        if (!snapshot) {
            console.log("[sendFcmNotification] No data associated with the event");
            return;
        }

        // 1. 요청 데이터 가져오기
        const requestData = snapshot.data();
        console.log("[sendFcmNotification] Request data:", JSON.stringify(requestData, null, 2));
        const {targetUserId, title, message: body, notificationType} = requestData;

        console.log(`[sendFcmNotification] New FCM request. Type: ${notificationType}`);

        if (!targetUserId || !title || !body) {
            console.error("[sendFcmNotification] Request data is missing fields. Deleting request.", requestData);
            await snapshot.ref.delete(); // Delete request to prevent re-tries
            return;
        }

        try {
            const db = admin.firestore();

            // 2. 받는 사람(Receiver)의 FCM 토큰 및 알림 설정 가져오기
            console.log(`[sendFcmNotification] Fetching user data for targetUserId: ${targetUserId}`);
            const receiverDoc = await db.collection("users").doc(targetUserId).get();
            const receiverData = receiverDoc.data();
            const fcmToken = receiverData?.fcmToken;
            console.log(`[sendFcmNotification] Receiver data:`, JSON.stringify(receiverData, null, 2));


            if (!fcmToken) {
                console.log(`[sendFcmNotification] No FCM token for receiver ${targetUserId}. Deleting request.`);
                await snapshot.ref.delete();
                return;
            }

            // 3. 알림 유형에 따른 설정 확인
            let shouldSendNotification = true;
            let dataPayload: { [key: string]: string } = {
                notificationType: String(notificationType),
            };

            if (notificationType === "READING_INVITATION") {
                const friendReadingAlarmEnabled = receiverData?.friendReadingAlarmEnabled;
                console.log(`[sendFcmNotification] User ${targetUserId} friendReadingAlarmEnabled: ${friendReadingAlarmEnabled}`);
                if (friendReadingAlarmEnabled === false) {
                    console.log(`[sendFcmNotification] User ${targetUserId} has disabled friend reading alarms. Skipping notification.`);
                    shouldSendNotification = false;
                }
            } else if (notificationType === "FOLLOW" || notificationType === "FOLLOW_REQUEST") {
                const followAlarmEnabled = receiverData?.followAlarmEnabled;
                console.log(`[sendFcmNotification] User ${targetUserId} followAlarmEnabled: ${followAlarmEnabled}`);
                if (followAlarmEnabled === false) {
                    console.log(`[sendFcmNotification] User ${targetUserId} has disabled follow alarms. Skipping notification.`);
                    shouldSendNotification = false;
                }
            } else if (notificationType === "LIKE") {
                const likeAlarmEnabled = receiverData?.likeAlarmEnabled;
                console.log(`[sendFcmNotification] User ${targetUserId} likeAlarmEnabled: ${likeAlarmEnabled}`);
                if (likeAlarmEnabled === false) {
                    console.log(`[sendFcmNotification] User ${targetUserId} has disabled like alarms. Skipping notification.`);
                    shouldSendNotification = false;
                }
            } else if (notificationType === "CHAT_MESSAGE") {
                const messageAlarmEnabled = receiverData?.messageAlarmEnabled;
                console.log(`[sendFcmNotification] User ${targetUserId} messageAlarmEnabled: ${messageAlarmEnabled}`);
                if (messageAlarmEnabled === false) {
                    console.log(`[sendFcmNotification] User ${targetUserId} has disabled chat message alarms. Skipping notification.`);
                    shouldSendNotification = false;
                }
                // Extract chat-specific data for payload
                const { chatId, senderId, bookId } = requestData;
                dataPayload = { ...dataPayload, chatId: String(chatId), senderId: String(senderId), bookId: String(bookId) };
            }
            // Add other notification types here as needed

            if (!shouldSendNotification) {
                console.log("[sendFcmNotification] shouldSendNotification is false. Deleting request.");
                await snapshot.ref.delete();
                return;
            }

            // 4. 알림 메시지(Payload) 구성
            const fcmMessage = {
                notification: {
                    title: title,
                    body: body,
                },
                data: dataPayload, // Use the constructed data payload
                token: fcmToken,
            };

            // 5. FCM으로 메시지 전송
            console.log(`[sendFcmNotification] Sending FCM message to token: ${fcmToken}`);
            await getMessaging().send(fcmMessage);
            console.log("[sendFcmNotification] Successfully sent message.");

            // 6. 처리 완료된 요청 문서는 삭제
            console.log(`[sendFcmNotification] Deleting request document: ${snapshot.ref.path}`);
            await snapshot.ref.delete();

        } catch (error) {
            console.error("[sendFcmNotification] Error sending notification:", error);
            // 오류가 발생해도 요청 문서는 삭제하여 재시도를 방지
            try {
                await snapshot.ref.delete();
            } catch (deleteError) {
                console.error("[sendFcmNotification] Error deleting request document after error:", deleteError);
            }
        }
    });

export const sendBookClubChatMessageNotification = onDocumentCreated(
    {
        document: "bookClubs/{bookClubId}/messages/{messageId}",
        region: "asia-northeast3", // Use the same region as other functions
    },
    async (event) => {
        const snapshot = event.data;
        if (!snapshot) {
            console.log("No data associated with the chat message event");
            return;
        }

        const chatMessage = snapshot.data();
        const bookClubId = event.params.bookClubId;
        const senderId = chatMessage.senderId;
        const senderName = chatMessage.senderName;
        const messageText = chatMessage.message;

        console.log(`New chat message in bookClub ${bookClubId} from ${senderName} (${senderId}): ${messageText}`);

        if (!bookClubId || !senderId || !senderName || !messageText) {
            console.error("Missing chat message fields", chatMessage);
            return;
        }

        try {
            const db = admin.firestore();

            // 1. Get BookClub members
            const bookClubDoc = await db.collection("bookClubs").doc(bookClubId).get();
            const bookClubData = bookClubDoc.data();
            const members: string[] = bookClubData?.members || [];
            const bookClubName: string = bookClubData?.name || "Book Club";

            if (members.length === 0) {
                console.log(`No members in book club ${bookClubId}. Skipping notification.`);
                return;
            }

            // 2. Filter out sender and fetch FCM tokens for remaining members
            const recipientUids = members.filter(uid => uid !== senderId);

            if (recipientUids.length === 0) {
                console.log(`No other members to notify in book club ${bookClubId}.`);
                return;
            }

            const messagePromises = recipientUids.map(async (recipientUid) => {
                const userDoc = await db.collection("users").doc(recipientUid).get();
                const userData = userDoc.data();
                const fcmToken = userData?.fcmToken;
                const messageAlarmEnabled = userData?.messageAlarmEnabled;

                if (!fcmToken) {
                    console.log(`No FCM token for user ${recipientUid}`);
                    return null;
                }
                if (messageAlarmEnabled === false) {
                    console.log(`User ${recipientUid} has disabled message alarms. Skipping.`);
                    return null;
                }

                const notificationTitle = `${bookClubName} 채팅방`;
                const notificationBody = `${senderName}: ${messageText.substring(0, 50)}${messageText.length > 50 ? "..." : ""}`; // Snippet of message

                const fcmMessage = {
                    notification: {
                        title: notificationTitle,
                        body: notificationBody,
                    },
                    data: {
                        notificationType: "BOOK_CLUB_CHAT_MESSAGE", // New type
                        bookClubId: bookClubId,
                        senderId: senderId,
                        messageId: snapshot.id, // ID of the chat message document
                        // Potentially add the full message text to data payload for client to parse
                        message: messageText,
                        senderName: senderName,
                        bookClubName: bookClubName
                    },
                    token: fcmToken,
                };
                return getMessaging().send(fcmMessage);
            });

            const sendResults = await Promise.all(messagePromises);
            const successfulSends = sendResults.filter(result => result !== null).length;
            console.log(`Sent ${successfulSends} chat message notifications for book club ${bookClubId}.`);

        } catch (error) {
            console.error(`Error sending chat message notification for book club ${bookClubId}:`, error);
        }
    }
);

export const onLikeCreated = onDocumentUpdated(
    {
        document: "feeds/{feedId}",
        region: "asia-northeast3",
    },
    async (event) => {
        console.log(`[onLikeCreated] Triggered for feed ID: ${event.params.feedId}`);
        const beforeData = event.data?.before.data();
        const afterData = event.data?.after.data();

        if (!beforeData || !afterData) {
            console.log("[onLikeCreated] No data before or after for the event.");
            return;
        }

        console.log("[onLikeCreated] Before data:", JSON.stringify(beforeData, null, 2));
        console.log("[onLikeCreated] After data:", JSON.stringify(afterData, null, 2));

        const beforeLikedBy: string[] = beforeData.likedBy || [];
        const afterLikedBy: string[] = afterData.likedBy || [];

        // Determine if a new like was added
        const newLikes = afterLikedBy.filter(
            (userId) => !beforeLikedBy.includes(userId)
        );

        if (newLikes.length === 0) {
            console.log("[onLikeCreated] No new likes detected.");
            return;
        }
        console.log(`[onLikeCreated] Found new likes: ${newLikes.join(", ")}`);

        const feedOwnerId = afterData.authorId;
        if (!feedOwnerId) {
            console.error("[onLikeCreated] feedOwnerId is missing from afterData.");
            return;
        }
        console.log(`[onLikeCreated] Feed owner ID: ${feedOwnerId}`);


        // Process each new like
        for (const likerId of newLikes) {
            if (likerId === feedOwnerId) {
                console.log(`[onLikeCreated] Liker ${likerId} is the feed owner. Skipping.`);
                continue; // Skip to the next liker
            }

            try {
                const db = admin.firestore();

                console.log(`[onLikeCreated] Getting nickname for liker ID: ${likerId}`);
                const likerDoc = await db.collection("users").doc(likerId).get();
                const likerData = likerDoc.data();
                const likerDisplayName = likerData?.nickname || "Someone";
                console.log(`[onLikeCreated] Liker nickname: ${likerDisplayName}`);


                const feedRef = db.collection("feeds").doc(event.params.feedId);
                const feedDoc = await feedRef.get();
                const feedData = feedDoc.data();
                const bookTitle = feedData?.book?.title || "어떤 글";

                const fcmRequestPayload = {
                    targetUserId: feedOwnerId,
                    title: "새로운 좋아요!",
                    message: `${likerDisplayName}님이 회원님의 "${bookTitle}" 글을 좋아합니다. `,
                    notificationType: "LIKE",
                    createdAt: admin.firestore.FieldValue.serverTimestamp(),
                };
                
                console.log("[onLikeCreated] Creating fcmRequest with payload:", JSON.stringify(fcmRequestPayload, null, 2));
                await db.collection("fcmRequests").add(fcmRequestPayload);

                console.log(`[onLikeCreated] FCM request created for new like by ${likerId} on feed ${event.params.feedId}`);

            } catch (error) {
                console.error(`[onLikeCreated] Error processing like event for liker ${likerId}:`, error);
            }
        }
    }
);



