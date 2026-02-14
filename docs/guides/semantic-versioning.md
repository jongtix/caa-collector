# Semantic Versioning 상세 가이드

> **버전 관리 규칙 및 증가 시나리오**

**작성자**: jongtix + Claude (pm)
**Last Updated**: 2026-02-14

---

## Table of Contents

- [버전 형식](#버전-형식)
- [버전 증가 규칙](#버전-증가-규칙)
- [실무 시나리오](#실무-시나리오)
- [Pre-release 버전](#pre-release-버전)

---

## 버전 형식

```
MAJOR.MINOR.PATCH
```

**예시**: `1.2.3`
- **MAJOR**: 1 (하위 호환 깨지는 변경)
- **MINOR**: 2 (새 기능 추가, 하위 호환 유지)
- **PATCH**: 3 (버그 수정, 하위 호환 유지)

---

## 버전 증가 규칙

| 변경 유형 | 커밋 Type | 버전 증가 | 예시 |
|----------|----------|----------|------|
| **하위 호환 깨짐** | `BREAKING CHANGE` | MAJOR | 1.2.3 → 2.0.0 |
| **새 기능 추가** | `feat` | MINOR | 1.2.3 → 1.3.0 |
| **버그 수정** | `fix`, `perf` | PATCH | 1.2.3 → 1.2.4 |
| **기타 변경** | `chore`, `docs`, `refactor` | 없음 | 1.2.3 (유지) |

---

## 실무 시나리오

### 시나리오 1: MAJOR 증가
```bash
# 커밋 메시지
feat(watchlist): API 응답 구조 변경

BREAKING CHANGE: WatchlistResponse DTO 구조 변경

# 버전 변화
v1.2.3 → v2.0.0
```

**언제 사용**:
- API 응답 구조 변경 (하위 호환 깨짐)
- 엔드포인트 경로 변경 (`/api/v1` → `/api/v2`)
- 필수 파라미터 추가 또는 제거
- 데이터베이스 스키마 변경 (마이그레이션 필요)

---

### 시나리오 2: MINOR 증가
```bash
# 커밋 메시지
feat(kis): WebSocket 실시간 시세 추가

# 버전 변화
v1.2.3 → v1.3.0
```

**언제 사용**:
- 새로운 API 엔드포인트 추가
- 새로운 도메인 추가 (예: RealtimePrice)
- 선택적 파라미터 추가
- 새로운 기능 추가 (하위 호환 유지)

---

### 시나리오 3: PATCH 증가
```bash
# 커밋 메시지
fix(stockprice): 국내 지수 수집 오류 수정

# 버전 변화
v1.2.3 → v1.2.4
```

**언제 사용**:
- 버그 수정
- 예외 처리 개선
- 성능 최적화 (기능 변경 없음)
- 로깅 개선

---

### 시나리오 4: 버전 유지
```bash
# 커밋 메시지
docs(readme): 커밋 규칙 섹션 추가

# 버전 변화
v1.2.3 (유지)
```

**언제 사용**:
- 문서 수정 (`docs`)
- 빌드 설정 변경 (`chore`)
- 코드 리팩터링 (`refactor`, 기능 변경 없음)
- 테스트 추가 (`test`)

---

### 시나리오 5: Feature + Fix 혼합
```bash
# 커밋 히스토리
feat(watchlist): 검색 추가
fix(stockprice): 백필 오류 수정

# 버전 변화
v1.2.0 → v1.3.0 (MINOR 우선, PATCH 포함)
```

**규칙**: MAJOR > MINOR > PATCH 우선순위 적용

---

### 시나리오 6: Chore만 있는 경우
```bash
# 커밋 히스토리
chore(deps): Gradle 의존성 업데이트
docs(readme): 문서 수정

# 버전 변화
v1.2.0 (유지, release-please PR 생성 안 됨)
```

**규칙**: `chore`, `docs`, `refactor`만 있으면 릴리스 PR 생성 안 됨

---

## Pre-release 버전

개발 중인 기능을 테스트할 때 사용합니다.

**형식**: `MAJOR.MINOR.PATCH-alpha.N`, `MAJOR.MINOR.PATCH-beta.N`

**예시**:
- `v1.3.0-alpha.1`: 알파 버전 (내부 테스트)
- `v1.3.0-beta.1`: 베타 버전 (공개 테스트)
- `v1.3.0-rc.1`: Release Candidate (릴리스 후보)
- `v1.3.0`: 정식 릴리스

**사용 예**:
```bash
# 알파 버전 릴리스
git tag v1.3.0-alpha.1
git push origin v1.3.0-alpha.1

# 베타 버전 릴리스
git tag v1.3.0-beta.1
git push origin v1.3.0-beta.1

# 정식 릴리스
# release-please PR 머지 → v1.3.0 자동 생성
```

---

## 버전 증가 흐름도

```
커밋 분석
  ↓
BREAKING CHANGE 있음?
  ├── Yes → MAJOR 증가 (v1.2.3 → v2.0.0)
  └── No → feat 있음?
      ├── Yes → MINOR 증가 (v1.2.3 → v1.3.0)
      └── No → fix/perf 있음?
          ├── Yes → PATCH 증가 (v1.2.3 → v1.2.4)
          └── No → 버전 유지 (v1.2.3)
```

---

## 참고 자료

- [Semantic Versioning 공식 사이트](https://semver.org/)
- [DEVELOPMENT.md](../DEVELOPMENT.md) - 전체 개발 가이드
- [Conventional Commits 가이드](./conventional-commits.md)
