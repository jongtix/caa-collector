# Conventional Commits 상세 가이드

> **커밋 메시지 작성 규칙 및 실무 예시**

**작성자**: jongtix + Claude (pm)
**Last Updated**: 2026-02-14

---

## Table of Contents

- [기본 형식](#기본-형식)
- [Type 종류](#type-종류)
- [Scope 가이드](#scope-가이드)
- [실무 예시](#실무-예시)
- [작성 팁](#작성-팁)

---

## 기본 형식

```
type(scope): description

[optional body]

[optional footer]
```

**구성 요소**:
- **type**: 커밋 유형 (필수)
- **scope**: 변경 범위 (선택, 예: `watchlist`, `stockprice`, `kis`)
- **description**: 간결한 설명 (필수, 명령형, 50자 이내)
- **body**: 상세 설명 (선택, 변경 이유, 영향 등)
- **footer**: 이슈 참조, BREAKING CHANGE (선택)

---

## Type 종류

| Type | 의미 | 버전 증가 | 사용 시점 |
|------|------|----------|----------|
| `feat` | 새 기능 추가 | MINOR (1.2.0 → 1.3.0) | API 엔드포인트 추가, 도메인 추가 |
| `fix` | 버그 수정 | PATCH (1.2.0 → 1.2.1) | 오류 수정, 예외 처리 개선 |
| `chore` | 설정, 빌드 변경 | 없음 | Gradle 의존성 업데이트, CI 설정 |
| `docs` | 문서 변경 | 없음 | README, ADR, 주석 추가/수정 |
| `refactor` | 리팩터링 | 없음 | 코드 구조 개선, 성능 최적화 |
| `test` | 테스트 추가/수정 | 없음 | 단위 테스트, 통합 테스트 추가 |
| `style` | 코드 스타일 변경 | 없음 | 포맷팅, 세미콜론 추가 (로직 변화 없음) |
| `perf` | 성능 개선 | PATCH | 쿼리 최적화, 캐싱 개선 |
| `ci` | CI/CD 변경 | 없음 | GitHub Actions 워크플로우 수정 |
| `build` | 빌드 시스템 변경 | 없음 | Gradle 스크립트, Dockerfile 수정 |
| `revert` | 커밋 되돌리기 | 없음 | 이전 커밋 취소 |

---

## Scope 가이드

| Scope | 설명 | 예시 |
|-------|------|------|
| `watchlist` | 관심종목 도메인 | `feat(watchlist): 검색 기능 추가` |
| `stockprice` | 주식 가격 수집 도메인 | `fix(stockprice): 국내 지수 수집 오류 수정` |
| `kis` | KIS API 연동 | `feat(kis): WebSocket 실시간 시세 추가` |
| `config` | 전역 설정 | `chore(config): Rate Limiter 설정 변경` |
| `ci` | CI/CD | `ci(ci): Docker 이미지 캐싱 개선` |
| `docs` | 문서 | `docs(adr): ADR-0020 작성` |

---

## 실무 예시

### 1. Feature 추가
```bash
feat(watchlist): 관심종목 검색 API 추가

- GET /api/v1/watchlist/search 엔드포인트 추가
- 종목명, 종목코드 검색 지원
- 페이지네이션 적용
```

### 2. Bug Fix
```bash
fix(stockprice): DomesticIndexStrategy 실행 누락 해결

AssetType.DOMESTIC_INDEX 매핑 오류로 인해 국내 지수 수집이
실행되지 않는 문제 수정.

Fixes #123
```

### 3. Breaking Change (MAJOR 버전)
```bash
feat(watchlist): 관심종목 API 응답 구조 변경

BREAKING CHANGE: WatchlistResponse DTO 구조 변경으로
하위 호환성 깨짐. 클라이언트는 새 응답 형식에 맞춰 수정 필요.

Before:
{
  "stocks": [...]
}

After:
{
  "data": {
    "stocks": [...]
  },
  "meta": {...}
}
```

### 4. Chore (버전 증가 없음)
```bash
chore(deps): Gradle 의존성 업데이트

- Spring Boot 3.5.9 → 3.5.10
- Testcontainers 1.20.4 → 1.20.5
```

### 5. Refactor
```bash
refactor(watchlist): 3-way 동기화 로직 개선

- 배치 쿼리로 N+1 문제 해결
- Map 기반 매칭으로 시간 복잡도 O(n) 개선
```

### 6. Test
```bash
test(stockprice): StockPriceCollectionService 단위 테스트 추가

- 일간 수집 로직 테스트
- 백필 로직 테스트
- 커버리지 80% → 90% 향상
```

### 7. Docs
```bash
docs(adr): ADR-0020 Docker CI/CD 자동화 전략 추가

release-please 도입 결정 배경 및 구현 방법 문서화
```

### 8. Hybrid 방식 (이모지 + Type)

기존 프로젝트 스타일을 유지하면서 Conventional Commits를 사용할 수 있습니다.

**형식**:
```
:emoji: type(scope): description
```

**예시**:
```bash
✨ feat(watchlist): 관심종목 검색 기능 추가
🐛 fix(stockprice): 국내 지수 수집 오류 수정
🔧 chore(ci): release-please 워크플로우 추가
📝 docs(readme): 커밋 규칙 섹션 추가
```

**release-please 파싱 규칙**:
- `feat:` 또는 `✨ feat:` 모두 인식
- Type 위치: 라인 시작 또는 이모지 뒤

### 9. 다중 Scope
```bash
feat(watchlist,stockprice): 관심종목 기반 가격 수집 연동

- WatchlistService에서 StockPriceCollectionService 호출
- 관심종목 변경 시 자동 수집 트리거
```

### 10. Footer 활용
```bash
fix(stockprice): 백필 중복 저장 방지

중복 키 예외 발생 시 업데이트 로직 추가

Closes #456
Reviewed-by: PM
```

---

## 작성 팁

1. **제목은 명령형으로 작성**: "추가했음" (X) → "추가" (O)
2. **제목 50자 이내**: 간결하게 요약
3. **본문은 72자 줄바꿈**: 가독성 향상
4. **Why를 설명**: "무엇을" 했는지는 코드에서 보임, "왜" 했는지 설명
5. **이슈 번호 연결**: `Closes #123`, `Fixes #456`
6. **BREAKING CHANGE 명시**: Footer에 명확히 작성

---

## 참고 자료

- [Conventional Commits 공식 사이트](https://www.conventionalcommits.org/)
- [DEVELOPMENT.md](../DEVELOPMENT.md) - 전체 개발 가이드
- [ADR-0020](../adr/0020-docker-cicd-automation.md) - Docker CI/CD 자동화 전략
