# FCM 푸시 알림 설정 가이드

## 1. Firebase Functions 설치 및 배포

```bash
cd functions
npm install
firebase deploy --only functions
```

## 2. 앱 빌드 및 실행

```bash
# Android Studio에서 Gradle Sync 실행
# 앱을 두 기기에 설치
```

## 3. 테스트 방법

1. 두 기기에서 각각 다른 계정으로 로그인
2. 서로 팔로우하여 맞팔 상태 만들기
3. 한 기기에서 "친구에게 알림보내기" 버튼 클릭
4. 다른 기기에 푸시 알림이 즉시 표시됨

## 4. 작동 원리

1. 로그인 시 FCM 토큰이 Firestore의 users/{userId}/fcmToken에 저장됨
2. "알림보내기" 버튼 클릭 시:
   - 맞팔 친구 목록 조회
   - 각 친구의 FCM 토큰 조회
   - fcmRequests 컬렉션에 알림 요청 생성
3. Cloud Functions가 fcmRequests를 감지하고 FCM API로 푸시 알림 전송
4. 친구의 기기에서 MyFirebaseMessagingService가 알림 수신 및 표시

## 5. 주의사항

- Firebase Console에서 Cloud Functions가 활성화되어 있어야 함
- 앱이 백그라운드/포그라운드 모두에서 알림 수신 가능
- Android 13+ 에서는 알림 권한 필요 (이미 AndroidManifest에 추가됨)

## 6. 로그 확인

```bash
# Android 로그
adb logcat | grep "FCM\|NotificationRepository"

# Firebase Functions 로그
firebase functions:log
```
