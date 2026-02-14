# CAA Collector - 개발자 가이드

> **개발 환경 설정, 커밋 규칙, 릴리스 프로세스 Quick Reference**

**작성자**: jongtix + Claude (pm)
**Last Updated**: 2026-02-14

---

## 빠른 시작

### 환경 준비

**필수 도구**:
- Java 21 이상
- Docker Desktop (실행 상태 유지) - 테스트 필수
- Git

**환경 변수 설정** (`.env` 파일):
```bash
# KIS API
KIS_APP_KEY=your_app_key
KIS_APP_SECRET=your_app_secret
KIS_ACCOUNT_NUMBER=your_account_number

# Redis & MySQL
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/caa_collector

# 보안 설정 (Phase 2)
TOKEN_ENCRYPTION_KEY=your_32byte_base64_key
REDIS_KEY_HMAC_SECRET=your_hmac_secret
ACTUATOR_USERNAME=actuator
ACTUATOR_PASSWORD=your_password
```

**자세한 설정**: [README.md - Quick Start](../README.md#quick-start)

---

## 개발 워크플로우

```bash
# 1. Feature 브랜치 생성
git checkout -b feature/watchlist-search

# 2. 개발 및 테스트
./gradlew build -q
./gradlew test -q

# 3. Conventional Commits 형식으로 커밋
git commit -m "feat(watchlist): 관심종목 검색 API 추가"

# 4. Push & PR 생성
git push origin feature/watchlist-search

# 5. PR 머지 후 release-please가 자동 릴리스 PR 생성
# 6. 릴리스 PR 승인 → 자동 배포 (Git Tag → Docker Hub → Watchtower)
```

---

## Conventional Commits 핵심

### 기본 형식

```
type(scope): description

[optional body]
```

**주요 Type**:
- `feat`: 새 기능 추가 (MINOR 버전 증가)
- `fix`: 버그 수정 (PATCH 버전 증가)
- `chore`: 설정, 빌드 변경 (버전 유지)
- `docs`: 문서 변경 (버전 유지)

**Scope 예시**: `watchlist`, `stockprice`, `kis`, `ci`

### 실무 예시

```bash
# Feature
feat(watchlist): 관심종목 검색 API 추가

# Bug Fix
fix(stockprice): 국내 지수 수집 오류 수정

# Breaking Change
feat(watchlist): API 응답 구조 변경

BREAKING CHANGE: WatchlistResponse DTO 변경
```

**상세 가이드**: [guides/conventional-commits.md](./guides/conventional-commits.md)

---

## Semantic Versioning

### 버전 형식

```
MAJOR.MINOR.PATCH (예: 1.2.3)
```

### 버전 증가 규칙

| 변경 유형 | 커밋 Type | 버전 증가 |
|----------|----------|----------|
| 하위 호환 깨짐 | `BREAKING CHANGE` | MAJOR (1.2.3 → 2.0.0) |
| 새 기능 추가 | `feat` | MINOR (1.2.3 → 1.3.0) |
| 버그 수정 | `fix`, `perf` | PATCH (1.2.3 → 1.2.4) |
| 기타 변경 | `chore`, `docs` | 없음 (1.2.3 유지) |

**상세 가이드**: [guides/semantic-versioning.md](./guides/semantic-versioning.md)

---

## release-please 워크플로우

### 자동 릴리스 흐름

```
[개발]
커밋 (Conventional Commits 형식)
  ↓
PR 머지 (main 브랜치)

[자동화]
release-please: PR 생성 (chore(main): release 1.3.0)
  ↓
개발자: PR 승인 및 머지
  ↓
release-please: Git Tag (v1.3.0) + GitHub Release
  ↓
GitHub Actions: Docker 이미지 빌드 및 푸시
  ↓
Watchtower: 컨테이너 자동 재시작
```

### 주요 설정 파일

- **release-please-config.json**: 릴리스 설정 (버전 증가 규칙, CHANGELOG 형식)
- **.release-please-manifest.json**: 현재 버전 추적 (자동 업데이트)

**상세 가이드**: [guides/release-process.md](./guides/release-process.md)

---

## 브랜치 전략

| 브랜치 유형 | 네이밍 규칙 | 예시 |
|------------|------------|------|
| main | `main` | `main` (프로덕션) |
| feature | `feature/{service}-{name}` | `feature/watchlist-search` |
| bugfix | `bugfix/{service}-{name}` | `bugfix/stockprice-duplicate` |
| chore | `chore/{scope}` | `chore/docker-push` |
| docs | `docs/{topic}` | `docs/development-guide` |

**상세 가이드**: [`~/.claude/skills/git-branch-strategy/SKILL.md`](~/.claude/skills/git-branch-strategy/SKILL.md)

---

## 체크리스트

### 커밋 전
- [ ] Conventional Commits 형식 (`type(scope): description`)
- [ ] 제목 50자 이내, 명령형
- [ ] Type과 Scope가 명확한가?
- [ ] 테스트 통과 (`./gradlew test -q`)
- [ ] 빌드 성공 (`./gradlew build -q`)

### 릴리스 전
- [ ] release-please PR 버전 확인
- [ ] CHANGELOG.md 검토
- [ ] 테스트 통과
- [ ] 보안 스캔 통과 (Trivy)

---

## 빌드 명령어

```bash
# 빌드
./gradlew build -q

# 실행
./gradlew bootRun -q

# 전체 테스트
./gradlew test -q

# 특정 테스트 클래스
./gradlew test -q --tests "com.custom.trader.watchlist.service.WatchlistServiceTest"

# 클린 빌드
./gradlew clean build -q
```

---

## 참고 자료

### 상세 가이드
- [Conventional Commits 가이드](./guides/conventional-commits.md)
- [Semantic Versioning 가이드](./guides/semantic-versioning.md)
- [release-please 프로세스](./guides/release-process.md)

### 관련 문서
- [README.md](../README.md) - 프로젝트 개요 및 Quick Start
- [ADR-0020](./adr/0020-docker-cicd-automation.md) - Docker CI/CD 자동화 전략
- [MILESTONE.md](./MILESTONE.md) - Phase별 일정 및 진행 상황
- [TODO.md](./TODO.md) - 단기 작업 목록

### 외부 자료
- [Conventional Commits 표준](https://www.conventionalcommits.org/)
- [Semantic Versioning](https://semver.org/)
- [release-please 공식 문서](https://github.com/googleapis/release-please)
