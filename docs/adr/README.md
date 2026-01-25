# Architecture Decision Records (ADR)

이 디렉토리는 프로젝트의 주요 아키텍처 결정사항을 기록합니다.

## ADR 목록

| 번호 | 제목 | 상태 | 날짜 |
|------|------|------|------|
| [0001](0001-batch-query-optimization.md) | Watchlist 배치 쿼리 최적화 | Accepted | 2026-01-23 |
| [0002](0002-account-level-token-lock.md) | 계정별 토큰 갱신 락 세분화 | Accepted | 2026-01-23 |
| [0003](0003-stock-price-entity-separation.md) | StockPrice Entity 4종 분리 설계 | Accepted | 2026-01-24 |
| [0004](0004-restclient-timeout-configuration.md) | RestClient 타임아웃 설정 | Accepted | 2026-01-23 |
| [0005](0005-watchlist-group-indexing.md) | WatchlistGroup 인덱스 추가 | Accepted | 2026-01-23 |

## ADR 템플릿

새로운 ADR 작성 시 다음 형식을 따릅니다:

```markdown
# [번호]. [제목]

## 상태
[Proposed | Accepted | Deprecated | Superseded]

## 컨텍스트
결정이 필요한 배경과 문제 상황

## 결정
내린 결정과 그 이유

## 결과
이 결정으로 인한 영향 (긍정적/부정적)

## 대안
고려했던 다른 옵션들과 채택하지 않은 이유
```

## 작성 가이드

1. **번호**: 4자리 숫자로 시작 (0001, 0002, ...)
2. **파일명**: `[번호]-[kebab-case-제목].md`
3. **상태**:
   - Proposed: 제안됨
   - Accepted: 승인됨
   - Deprecated: 더 이상 사용 안 함
   - Superseded: 다른 ADR로 대체됨
4. **내용**: 미래의 팀원이 이해할 수 있도록 충분한 컨텍스트 제공