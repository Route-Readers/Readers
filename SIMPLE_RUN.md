# 간단한 실행 방법

## 방법 1: Android Studio에서 새 프로젝트로 시작

1. **Android Studio 열기**
2. **"New Project" 선택**
3. **"Empty Activity" 선택**
4. **프로젝트 설정:**
   - Name: `Readers`
   - Package name: `com.route.readers`
   - Language: `Kotlin`
   - Minimum SDK: `API 24`

5. **프로젝트 생성 후 파일 교체:**
   - `MainActivity.kt` → 우리가 만든 MainActivity.kt로 교체
   - `ui` 폴더 전체 복사
   - `data` 폴더 전체 복사

## 방법 2: 현재 폴더 직접 열기

1. **Android Studio에서 "Open" 선택**
2. **폴더 선택할 때 `build.gradle.kts` 파일이 보이는 폴더 선택**
3. **"Open as Project" 클릭**

## 방법 3: 파일 확인

현재 폴더에 이 파일들이 있는지 확인해주세요:
- `build.gradle.kts` (프로젝트 루트)
- `app/build.gradle.kts` (앱 모듈)
- `settings.gradle.kts`
- `gradle.properties`

없으면 다시 만들어드릴게요!
