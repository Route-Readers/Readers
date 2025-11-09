import {onDocumentCreated} from "firebase-functions/v2/firestore";
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
        const {targetUserId, title, message: body} = requestData;

        if (!targetUserId || !title || !body) {
            console.error("Request data is missing fields", requestData);
            return;
        }

        try {
            const db = admin.firestore();

            // 2. 받는 사람(Receiver)의 FCM 토큰 가져오기
            const receiverDoc = await db.collection("users").doc(targetUserId).get();
            const fcmToken = receiverDoc.data()?.fcmToken;

            if (!fcmToken) {
                console.log(`No FCM token for receiver ${targetUserId}`);
                // 처리 완료된 요청 문서는 삭제
                await snapshot.ref.delete();
                return;
            }

            // 3. 알림 메시지(Payload) 구성
            const fcmMessage = {
                notification: {
                    title: title,
                    body: body,
                },
                token: fcmToken,
            };

            // 4. FCM으로 메시지 전송
            console.log(`Sending notification to token: ${fcmToken}`);
            await getMessaging().send(fcmMessage);

            // 5. 처리 완료된 요청 문서는 삭제
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
