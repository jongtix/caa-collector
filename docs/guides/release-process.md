# release-please 릴리스 프로세스 가이드

> **자동 릴리스 워크플로우 상세 설명**

**작성자**: jongtix + Claude (pm)
**Last Updated**: 2026-02-14

---

## Table of Contents

- [전체 흐름](#전체-흐름)
- [단계별 상세 가이드](#단계별-상세-가이드)
- [커밋 메시지 → 버전 증가 매핑](#커밋-메시지--버전-증가-매핑)
- [설정 파일](#설정-파일)
- [체크리스트](#체크리스트)

---

## 전체 흐름

```
[1. 개발 단계]
개발자: feature 브랜치 작업
  ↓
개발자: Conventional Commits 형식으로 커밋
  ↓
개발자: PR 생성 및 머지 (main 브랜치)

[2. release-please 자동 PR 생성]
release-please: 커밋 메시지 분석
  ↓
release-please: 버전 증가 계산 (MAJOR/MINOR/PATCH)
  ↓
release-please: PR 자동 생성
  - 제목: "chore(main): release 1.3.0"
  - 변경사항:
    - build.gradle: version = '1.3.0'
    - CHANGELOG.md: 릴리스 노트 추가
    - .release-please-manifest.json: 버전 업데이트

[3. 릴리스 승인]
개발자: release-please PR 검토
  ↓
개발자: PR 승인 및 머지

[4. 자동 릴리스 및 배포]
release-please: Git Tag 생성 (v1.3.0)
  ↓
release-please: GitHub Release 생성 (CHANGELOG 포함)
  ↓
GitHub Actions: Docker 이미지 빌드 및 푸시
  - jongtix/caa-collector:v1.3.0
  - jongtix/caa-collector:latest
  ↓
Watchtower: 이미지 갱신 감지 → 자동 재시작
```

---

## 단계별 상세 가이드

### Step 1: 개발 및 커밋

```bash
# 1. Feature 브랜치 생성
git checkout -b feature/watchlist-search

# 2. 개발 작업
# 코드 작성, 테스트 추가

# 3. Conventional Commits 형식으로 커밋
git add .
git commit -m "feat(watchlist): 관심종목 검색 API 추가"

# 4. Push
git push origin feature/watchlist-search
```

**주의사항**:
- 커밋 메시지는 반드시 Conventional Commits 형식 준수
- 여러 변경 사항은 Atomic Commit으로 분리

---

### Step 2: PR 생성 및 머지

```bash
# 1. GitHub에서 PR 생성
# 2. 코드 리뷰 요청
# 3. CI 통과 확인 (Build & Test, Security Scan)
# 4. PR 승인 및 main 브랜치로 머지
```

**PR 템플릿 예시**:
```markdown
## 변경 사항
- 관심종목 검색 API 추가
- GET /api/v1/watchlist/search 엔드포인트

## 테스트
- WatchlistServiceTest 단위 테스트 추가
- 통합 테스트 통과 확인

## 체크리스트
- [x] Conventional Commits 형식 준수
- [x] 테스트 통과
- [x] 빌드 성공
```

---

### Step 3: release-please PR 자동 생성

**release-please가 자동으로 수행**:
1. main 브랜치의 모든 커밋 분석
2. 버전 증가 계산:
   - `feat` 커밋 발견 → MINOR 증가 (1.2.0 → 1.3.0)
3. PR 생성:
   - **제목**: `chore(main): release 1.3.0`
   - **변경 파일**:
     - `build.gradle`: `version = '1.3.0'`
     - `CHANGELOG.md`: 릴리스 노트 추가
     - `.release-please-manifest.json`: `{".": "1.3.0"}`

**CHANGELOG.md 예시**:
```markdown
## [1.3.0](https://github.com/jongtix/caa-collector/compare/v1.2.0...v1.3.0) (2026-02-13)

### Features

* **watchlist**: 관심종목 검색 API 추가 ([abc1234](https://github.com/jongtix/caa-collector/commit/abc1234))

### Bug Fixes

* **stockprice**: 국내 지수 수집 오류 수정 ([def5678](https://github.com/jongtix/caa-collector/commit/def5678))
```

---

### Step 4: 릴리스 PR 검토 및 승인

```bash
# 1. GitHub에서 release-please PR 확인
# 2. 버전 증가 확인 (1.2.0 → 1.3.0)
# 3. CHANGELOG.md 검토 (릴리스 노트 확인)
# 4. build.gradle 버전 확인
# 5. PR 승인 및 머지
```

**검토 체크리스트**:
- [ ] 버전 증가가 올바른가? (feat → MINOR, fix → PATCH)
- [ ] CHANGELOG.md에 모든 변경 사항이 포함되었는가?
- [ ] 누락된 중요 변경 사항은 없는가?
- [ ] BREAKING CHANGE가 있다면 명확히 표시되었는가?

---

### Step 5: 자동 릴리스 및 Docker 배포

**release-please가 자동으로 수행**:
1. **Git Tag 생성**: `v1.3.0`
2. **GitHub Release 생성**: CHANGELOG.md 내용 포함

**GitHub Actions (docker-publish.yml) 자동 실행**:
1. **이벤트 트리거**: Git Tag 푸시 이벤트 감지 (`v1.3.0`)
2. **버전 추출**: `v1.3.0`
3. **Docker 이미지 빌드**:
   ```bash
   docker build -t jongtix/caa-collector:1.3.0 .
   ```
4. **Docker Hub Push**:
   ```bash
   docker push jongtix/caa-collector:1.3.0
   docker push jongtix/caa-collector:latest
   ```
5. **Watchtower 자동 배포**:
   - Watchtower가 `jongtix/caa-collector:latest` 이미지 갱신 감지
   - 컨테이너 자동 재시작

---

## 커밋 메시지 → 버전 증가 매핑

### 시나리오 1: Feature만 있는 경우
```bash
# 커밋 히스토리
feat(watchlist): 검색 추가
feat(stockprice): 백필 개선

# 버전 변화
v1.2.0 → v1.3.0 (MINOR 증가)
```

### 시나리오 2: Fix만 있는 경우
```bash
# 커밋 히스토리
fix(watchlist): 동기화 오류 수정
fix(stockprice): 중복 저장 방지

# 버전 변화
v1.2.0 → v1.2.1 (PATCH 증가)
```

### 시나리오 3: Feature + Fix 혼합
```bash
# 커밋 히스토리
feat(watchlist): 검색 추가
fix(stockprice): 백필 오류 수정

# 버전 변화
v1.2.0 → v1.3.0 (MINOR 우선, PATCH 포함)
```

### 시나리오 4: BREAKING CHANGE 포함
```bash
# 커밋 히스토리
feat(watchlist): API 구조 변경

BREAKING CHANGE: 응답 구조 변경

# 버전 변화
v1.2.0 → v2.0.0 (MAJOR 증가)
```

### 시나리오 5: Chore만 있는 경우
```bash
# 커밋 히스토리
chore(deps): Gradle 의존성 업데이트
docs(readme): 문서 수정

# 버전 변화
v1.2.0 (유지, release-please PR 생성 안 됨)
```

---

## 설정 파일

### release-please-config.json

**위치**: `caa-collector/release-please-config.json`

**주요 기능**:
- Java 프로젝트 설정 (`release-type: java`)
- 1.x 버전에서 MINOR 증가 (`bump-minor-pre-major: true`)
- build.gradle 버전 자동 업데이트
- CHANGELOG.md 섹션 구조 정의 (feat, fix, perf만 노출)

**실제 파일 예시**:
```json
{
  "release-type": "java",
  "bump-minor-pre-major": true,
  "bump-patch-for-minor-pre-major": false,
  "changelog-sections": [
    {"type": "feat", "section": "Features"},
    {"type": "fix", "section": "Bug Fixes"},
    {"type": "perf", "section": "Performance Improvements"}
  ],
  "extra-files": [
    "build.gradle"
  ]
}
```

### .release-please-manifest.json

**위치**: `caa-collector/.release-please-manifest.json`

```json
{
  ".": "0.1.0"
}
```

**역할**: 현재 버전 추적 (release-please가 자동 업데이트)

---

## 체크리스트

### 커밋 전
- [ ] Conventional Commits 형식 준수 (`type(scope): description`)
- [ ] 제목 50자 이내, 명령형으로 작성
- [ ] Type이 변경 사항과 일치하는가? (feat, fix, chore 등)
- [ ] Scope가 명확한가? (watchlist, stockprice 등)
- [ ] BREAKING CHANGE가 있다면 Footer에 명시했는가?
- [ ] 테스트가 통과하는가? (`./gradlew test -q`)
- [ ] 빌드가 성공하는가? (`./gradlew build -q`)

### 릴리스 전
- [ ] release-please PR의 버전 증가가 올바른가?
- [ ] CHANGELOG.md에 모든 변경 사항이 포함되었는가?
- [ ] build.gradle의 버전이 올바른가?
- [ ] 테스트가 모두 통과하는가?
- [ ] 보안 스캔이 통과하는가? (Trivy)
- [ ] Docker 이미지 빌드가 성공하는가?

---

## 참고 자료

- [release-please 공식 문서](https://github.com/googleapis/release-please)
- [ADR-0020: Docker CI/CD 자동화 전략](../adr/0020-docker-cicd-automation.md)
- [DEVELOPMENT.md](../DEVELOPMENT.md) - 전체 개발 가이드
- [Conventional Commits 가이드](./conventional-commits.md)
