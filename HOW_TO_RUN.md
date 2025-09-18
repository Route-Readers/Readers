# 피드 화면 실행 방법

## 1. Android Studio에서 실행

1. **Android Studio 열기**
   - Android Studio를 실행합니다
   - "Open an Existing Project" 선택
   - `/mnt/c/Users/admin/Downloads/Readers` 폴더 선택

2. **프로젝트 동기화**
   - Android Studio가 프로젝트를 로드하면 "Sync Now" 클릭
   - Gradle 동기화가 완료될 때까지 기다립니다

3. **실행**
   - 상단 툴바에서 "Run" 버튼 클릭 (▶️)
   - 또는 `Shift + F10` 단축키 사용
   - 에뮬레이터나 실제 기기에서 앱이 실행됩니다

## 2. 명령줄에서 실행 (선택사항)

```bash
cd /mnt/c/Users/admin/Downloads/Readers
./gradlew assembleDebug
./gradlew installDebug
```

## 현재 구현된 기능

✅ **친구 스토리**: 상단에 원형 프로필 이미지  
✅ **읽는 중 표시**: 초록색 점으로 현재 읽고 있는 친구 표시  
✅ **진행률 표시**: 퍼센트와 프로그레스 바  
✅ **완독 표시**: "완독 완료! 🎉" 메시지  
✅ **카드 UI**: 깔끔한 Material 3 디자인  

## 주의사항

- 현재는 목업 데이터로 동작합니다
- 실제 이미지는 회색 박스로 표시됩니다
- API 연동은 아직 구현되지 않았습니다

## 문제 해결

**빌드 오류가 발생하면:**
1. Android Studio에서 "File" → "Invalidate Caches and Restart"
2. 프로젝트 클린: "Build" → "Clean Project"
3. 리빌드: "Build" → "Rebuild Project"
