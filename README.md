# Readers
독서를 더욱 재미있고 오래할 수 있게! 독서 기록 및 공유 SNS를 만들어요

## 프로젝트 개요
**Readers**: 독서 기록 및 공유를 위한 SNS

친구들과 독서 기록을 공유하고 지속적인 독서 습관을 만들 수 있게 돕는 안드로이드 앱

## 현재 구현된 주요 기능 ✅
1. **사용자 인증 시스템**: Firebase Auth 기반 Google 로그인, 회원가입
2. **소셜 피드**: 친구들의 독서 활동 중심 피드, 좋아요 기능, 맞팔하기 카드
3. **내 서재**: 개인 독서 기록 관리 및 진도율 추적
4. **도서 검색**: 알라딘 API 연동 도서 검색 및 도서관 위치 정보
5. **친구 시스템**: 친구 추가/삭제, 팔로우/언팔로우, 차단 기능
6. **알림 시스템**: 독서 알림 및 친구 요청 알림
7. **프로필 관리**: 사용자 프로필 설정 및 편집
8. **커뮤니티**: 전체 사용자 목록, 친구 목록 관리
9. **실시간 위젯**: 홈 화면 독서 진행 상황 위젯

## 개발 예정 기능 🚧
1. 독서 챌린지 시스템
2. AI 기반 도서 추천
3. 헌책 중고 거래 플랫폼 (토큰 시스템)
4. 독서량 달성 보상 시스템
5. 통계 대시보드 (주간/월간/연간)

## 기술 스택
- **Frontend**: Jetpack Compose, Material 3
- **Backend**: Firebase (Auth, Firestore, Storage)
- **API**: 알라딘 Open API, 공공데이터포털 도서관 API
- **Architecture**: MVVM Pattern
- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)

## 개발 일정
- **정기 회의**: 매주 수요일 18시 대면 회의
- **추가 회의**: 매주 금/토/일 중 1회, 19시 Discord 비대면 회의
- **최근 업데이트**: 2025.10.26 - 피드 맞팔하기 기능, 도서관 위치 수정

## 기대 효과
1. **독서량 증가**: 친구들과의 상호작용과 알림을 통한 독서 동기 부여
2. **지속 가능한 독서**: P2P 중고거래로 새로운 책과 쉬운 교환
3. **독서 커뮤니티 형성**: 실제 친구들과의 독서 경험 공유

## 수익화 전략
- 애드몹 광고 수익
- 광고 제거 프리미엄 버전 (990원)
- 출판사 협력 네이티브 광고
- 토큰 현금 구매 시스템

## 차별화 포인트
- **실시간 상호작용**: 친구의 독서 진행상황과 밑줄 친 부분 실시간 공유
- **게임화 요소**: 챌린지, 스트릭, 보상 시스템으로 독서를 재미있게
- **친구 중심 피드**: 광고나 인플루언서가 아닌 실제 친구들의 독서 활동 중심
- **실시간 위젯**: 홈 화면에서 바로 확인 가능한 친구들의 독서 현황
- **토큰 경제 시스템**: 독자적인 화폐로 중고거래 및 상품 교환 가능

## 현재 프로젝트 구조
```
com.route.readers/
├── 📱 MainActivity.kt
├── 📂 data/
│   ├── model/          // 데이터 모델
│   │   ├── Book.kt
│   │   ├── User.kt
│   │   ├── MyBook.kt
│   │   ├── BookClub.kt
│   │   ├── Challenge.kt
│   │   ├── FriendRequest.kt
│   │   └── Notification.kt
│   └── remote/         // API 및 Repository
│       ├── BookRepository.kt
│       ├── FirestoreRepository.kt
│       ├── FriendsRepository.kt
│       ├── LibraryRepository.kt
│       └── NotificationRepository.kt
│
├── 📂 ui/
│   ├── screens/        // 화면별 패키지
│   │   ├── login/      // 로그인, 회원가입, 온보딩
│   │   ├── feed/       // 피드 화면
│   │   ├── mylibrary/  // 내 서재
│   │   ├── search/     // 도서 검색
│   │   ├── profile/    // 프로필 관리
│   │   └── community/  // 커뮤니티, 친구 관리
│   │
│   ├── components/     // 공용 컴포넌트
│   │   ├── BottomNavBar.kt
│   │   ├── BookClubCard.kt
│   │   └── NotificationIconWithBadge.kt
│   │
│   ├── navigation/     // 네비게이션
│   │   └── AppNavigation.kt
│   │
│   └── theme/          // 디자인 테마
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── 📂 notification/    // 알림 시스템
├── 📂 widget/          // 홈 위젯
└── 📂 외부 패키지/
    ├── data/model/     // 추가 데이터 모델
    └── ui/components/  // 추가 컴포넌트
```

## 최근 개발 현황
- ✅ 피드 맞팔하기 카드 기능 구현 완료
- ✅ 도서관 위치 정보 수정 완료
- ✅ 좋아요 기능 오류 수정 완료
- 🔄 현재 진행 중: UI/UX 개선 및 버그 수정

## 개발 환경 설정
1. Android Studio 최신 버전 설치
2. Kotlin 1.9+ 지원
3. Firebase 프로젝트 설정 필요
4. local.properties에 API 키 설정:
   ```
   ALADIN_TTB_KEY=your_aladin_api_key
   DATA_GO_KR_API_KEY=your_data_go_kr_api_key
   ```

## 팀 협업 가이드
- 각 기능별로 별도 브랜치에서 개발
- Pull Request를 통한 코드 리뷰 진행
- 매주 정기 회의에서 진행 상황 공유
- Discord를 통한 실시간 소통
