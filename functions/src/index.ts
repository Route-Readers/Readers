import {onDocumentCreated, onDocumentUpdated, onDocumentDeleted} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import {getMessaging} from "firebase-admin/messaging";

// Dummy comment to force redeployment (2025-11-27 - second attempt)

// Firebase Admin SDK 초기화
admin.initializeApp();

// "fcm_messages" 컬렉션에 새 문서가 생성될 때마다 이 함수가 실행됨 (독서 알림용)
export const sendReadingReminderNotification = onDocumentCreated(
    {
        document: "fcm_messages/{messageId}",
        region: "asia-northeast3",
    },
    async (event) => {
        console.log(`[sendReadingReminderNotification] Triggered for message ID: ${event.params.messageId}`);
        const snapshot = event.data;
        if (!snapshot) {
            console.log("[sendReadingReminderNotification] No data associated with the event");
            return;
        }

        const messageData = snapshot.data();
        console.log("[sendReadingReminderNotification] Message data:", JSON.stringify(messageData, null, 2));
        
        const {type, fromUserName, fcmToken, title, body} = messageData;

        if (type !== "reading_reminder" || !fcmToken || !title || !body) {
            console.log("[sendReadingReminderNotification] Invalid message data, deleting document");
            await snapshot.ref.delete();
            return;
        }

        try {
            // FCM 메시지 구성
            const fcmMessage = {
                notification: {
                    title: title,
                    body: body,
                },
                data: {
                    type: "reading_reminder",
                    fromUserName: fromUserName || "",
                },
                token: fcmToken,
            };

            // FCM으로 메시지 전송
            console.log(`[sendReadingReminderNotification] Sending FCM message to token: ${fcmToken}`);
            await getMessaging().send(fcmMessage);
            console.log("[sendReadingReminderNotification] Successfully sent reading reminder.");

            // 처리 완료된 문서 삭제
            await snapshot.ref.delete();

        } catch (error) {
            console.error("[sendReadingReminderNotification] Error sending notification:", error);
            await snapshot.ref.delete();
        }
    });

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


                const fcmRequestPayload = {
                    targetUserId: feedOwnerId,
                    title: "새로운 좋아요!",
                    message: `${likerDisplayName}님이 회원님의 글을 좋아합니다. `,
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

// 유저 문서 삭제 시 관련 데이터 정리
export const cleanupUserData = onDocumentDeleted(
    {
        document: "users/{uid}",
        region: "asia-northeast3",
    },
    async (event) => {
        const uid = event.params.uid;
        console.log(`[cleanupUserData] User deleted: ${uid}`);

        const db = admin.firestore();

        try {
            // 1. feeds에서 해당 유저의 게시글 삭제
            const feedsSnapshot = await db.collection("feeds")
                .where("authorId", "==", uid).get();
            for (const doc of feedsSnapshot.docs) {
                await doc.ref.delete();
            }

            // 2. usedBooks에서 해당 유저의 중고책 게시글 삭제
            const usedBooksSnapshot = await db.collection("usedBooks")
                .where("sellerId", "==", uid).get();
            for (const doc of usedBooksSnapshot.docs) {
                await doc.ref.delete();
            }

            // 3. 다른 유저들의 followers/following/friends 목록에서 제거
            const usersWithFollower = await db.collection("users")
                .where("followers", "array-contains", uid).get();
            for (const doc of usersWithFollower.docs) {
                await doc.ref.update({
                    followers: admin.firestore.FieldValue.arrayRemove(uid),
                    followerCount: admin.firestore.FieldValue.increment(-1)
                });
            }

            const usersWithFollowing = await db.collection("users")
                .where("following", "array-contains", uid).get();
            for (const doc of usersWithFollowing.docs) {
                await doc.ref.update({
                    following: admin.firestore.FieldValue.arrayRemove(uid),
                    followingCount: admin.firestore.FieldValue.increment(-1)
                });
            }

            const usersWithFriend = await db.collection("users")
                .where("friends", "array-contains", uid).get();
            for (const doc of usersWithFriend.docs) {
                await doc.ref.update({
                    friends: admin.firestore.FieldValue.arrayRemove(uid)
                });
            }

            // 4. 챌린지 참여자 목록에서 제거
            const challengesWithUser = await db.collection("challenges")
                .where("participants", "array-contains", uid).get();
            for (const doc of challengesWithUser.docs) {
                await doc.ref.update({
                    participants: admin.firestore.FieldValue.arrayRemove(uid)
                });
            }

            // 5. 피드의 likedBy에서 제거
            const feedsLikedByUser = await db.collection("feeds")
                .where("likedBy", "array-contains", uid).get();
            for (const doc of feedsLikedByUser.docs) {
                await doc.ref.update({
                    likedBy: admin.firestore.FieldValue.arrayRemove(uid),
                    likeCount: admin.firestore.FieldValue.increment(-1)
                });
            }

            console.log(`[cleanupUserData] Successfully cleaned up data for user: ${uid}`);

        } catch (error) {
            console.error(`[cleanupUserData] Error cleaning up user data:`, error);
        }
    }
);


// 기존 유령 계정 정리 (HTTP 호출용 - 한 번만 실행)
import {onRequest} from "firebase-functions/v2/https";
import {getAuth} from "firebase-admin/auth";
import {defineSecret} from "firebase-functions/params";

const cleanupSecret = defineSecret("CLEANUP_SECRET");

export const cleanupGhostUsers = onRequest(
    { region: "asia-northeast3", secrets: [cleanupSecret] },
    async (req, res) => {
        const authHeader = req.headers.authorization;
        if (authHeader !== `Bearer ${cleanupSecret.value()}`) {
            res.status(403).send("Unauthorized");
            return;
        }

        const db = admin.firestore();
        const auth = getAuth();
        
        let deletedCount = 0;
        let checkedCount = 0;
        const deletedUids: string[] = [];

        try {
            const usersSnapshot = await db.collection("users").get();
            
            for (const doc of usersSnapshot.docs) {
                checkedCount++;
                const uid = doc.id;
                
                try {
                    await auth.getUser(uid);
                } catch (error: unknown) {
                    if ((error as {code?: string}).code === "auth/user-not-found") {
                        console.log(`Deleting ghost user: ${uid}`);
                        deletedUids.push(uid);
                        
                        // 유저 문서 삭제
                        await doc.ref.delete();
                        
                        // 피드 삭제
                        const feeds = await db.collection("feeds").where("authorId", "==", uid).get();
                        for (const f of feeds.docs) await f.ref.delete();
                        
                        // myLibrary 서브컬렉션 삭제
                        const myLibrary = await db.collection("users").doc(uid).collection("myLibrary").get();
                        for (const b of myLibrary.docs) await b.ref.delete();
                        
                        // 중고책 삭제
                        const usedBooks = await db.collection("usedBooks").where("sellerId", "==", uid).get();
                        for (const u of usedBooks.docs) await u.ref.delete();
                        
                        deletedCount++;
                    }
                }
            }

            // 삭제된 유저들을 다른 유저의 followers/following/friends에서 제거
            for (const uid of deletedUids) {
                const usersWithFollower = await db.collection("users")
                    .where("followers", "array-contains", uid).get();
                for (const doc of usersWithFollower.docs) {
                    await doc.ref.update({
                        followers: admin.firestore.FieldValue.arrayRemove(uid),
                        followerCount: admin.firestore.FieldValue.increment(-1)
                    });
                }

                const usersWithFollowing = await db.collection("users")
                    .where("following", "array-contains", uid).get();
                for (const doc of usersWithFollowing.docs) {
                    await doc.ref.update({
                        following: admin.firestore.FieldValue.arrayRemove(uid),
                        followingCount: admin.firestore.FieldValue.increment(-1)
                    });
                }

                const usersWithFriend = await db.collection("users")
                    .where("friends", "array-contains", uid).get();
                for (const doc of usersWithFriend.docs) {
                    await doc.ref.update({
                        friends: admin.firestore.FieldValue.arrayRemove(uid)
                    });
                }

                // 챌린지에서 제거
                const challenges = await db.collection("challenges")
                    .where("participants", "array-contains", uid).get();
                for (const doc of challenges.docs) {
                    await doc.ref.update({
                        participants: admin.firestore.FieldValue.arrayRemove(uid)
                    });
                }

                // 피드 좋아요에서 제거
                const likedFeeds = await db.collection("feeds")
                    .where("likedBy", "array-contains", uid).get();
                for (const doc of likedFeeds.docs) {
                    await doc.ref.update({
                        likedBy: admin.firestore.FieldValue.arrayRemove(uid),
                        likeCount: admin.firestore.FieldValue.increment(-1)
                    });
                }
            }
            
            res.status(200).json({
                message: "Cleanup completed",
                checked: checkedCount,
                deleted: deletedCount,
                deletedUids: deletedUids
            });
        } catch (error) {
            console.error("Cleanup error:", error);
            res.status(500).send("Error during cleanup");
        }
    }
);
