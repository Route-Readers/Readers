# Feed 화면 구현

## 구현된 기능
- 친구들의 독서 현황을 인스타그램 스토리 형태로 표시
- 현재 읽고 있는 친구는 초록색 점으로 표시
- 친구들의 독서 진행률을 퍼센트와 프로그레스 바로 표시
- 완독한 책은 "완독 완료! 🎉" 메시지로 표시

## 파일 구조
```
ui/screens/feed/
├── FeedScreen.kt      # 메인 피드 화면
├── FeedViewModel.kt   # 피드 데이터 관리
└── README.md         # 이 파일

data/model/
├── User.kt           # 사용자 데이터 모델
├── Book.kt           # 책 데이터 모델
└── FeedItem.kt       # 피드 아이템 데이터 모델

ui/theme/
└── Color.kt          # 앱 테마 색상
```

## 주요 컴포넌트
- `FeedScreen`: 전체 피드 화면
- `FriendStoriesRow`: 상단 친구 스토리 영역
- `FriendStoryItem`: 개별 친구 스토리 아이템
- `FeedItemCard`: 개별 피드 아이템 카드

## 사용된 색상
- `ReadingGreen`: 현재 읽고 있는 상태 표시
- `ProgressBlue`: 진행률 표시
- `CompletedGreen`: 완독 완료 표시
- `TextGray`: 부가 정보 텍스트
