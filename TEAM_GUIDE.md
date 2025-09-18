# 팀 협업 가이드

## Git 브랜치 규칙
- `main`: 배포용 브랜치 (직접 푸시 금지)
- `develop`: 개발용 메인 브랜치
- `feature/기능명`: 새 기능 개발용
- feature/login
- feature/mainpage

## 커밋 메시지 규칙
- `feat: 새 기능 추가`
- `fix: 버그 수정`
- `docs: 문서 수정`
- `style: 코드 포맷팅`

## 작업 순서
1. `git pull origin develop`
2. `git checkout -b feature/기능명`
3. 작업 후 커밋
4. `git push origin feature/기능명`
5. GitHub에서 Pull Request 생성

## 주의사항
- 절대 main 브랜치에 직접 푸시하지 말 것
- 작업 전 항상 최신 코드 pull 받기
- 충돌 발생시 팀원과 상의 후 해결 ecjhekdce
