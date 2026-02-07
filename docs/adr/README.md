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
| [0006](0006-mapstruct-for-entity-mapping.md) | MapStruct를 이용한 Entity 매핑 | Accepted | 2026-01-24 |
| [0007](0007-pagination-for-bulk-data-query.md) | 대량 데이터 조회를 위한 페이징 처리 | Accepted | 2026-01-24 |
| [0008](0008-rate-limiter-centralization.md) | Rate Limiter 중앙화 | Accepted | 2026-01-25 |
| [0009](0009-stockprice-strategy-pattern.md) | StockPrice 도메인에 Strategy Pattern 도입 | Accepted | 2026-01-26 |
| [0010](0010-template-method-pattern-evaluation.md) | Template Method Pattern 도입 검토 및 보류 결정 | Deferred | 2026-01-26 |
| [0011](0011-websocket-realtime-price-strategy.md) | 실시간 시세 조회 방식 (REST API vs WebSocket) | Accepted | 2026-01-27 |
| [0012](0012-spring-security-integration.md) | Spring Security 통합 및 인증 체계 도입 | Accepted | 2026-02-01 |
| [0013](0013-test-resttemplate-for-security-test.md) | SecurityConfig 테스트에 TestRestTemplate 사용 | Accepted | 2026-02-01 |
| [0014](0014-security-scanning-strategy.md) | 보안 취약점 스캔 전략 변경 (OWASP → Dependabot + Trivy) | Accepted | 2026-02-02 |
| [0015](0015-https-enforcement-strategy.md) | HTTPS 강제 전략 - Phase 2 연기 결정 | Accepted | 2026-02-02 |
| [0016](0016-global-market-schedule-architecture.md) | 글로벌 주식 시장 스케줄 아키텍처 | Accepted | 2026-02-04 |
| [0017](0017-database-migration-strategy.md) | Database Migration Strategy (Flyway 도입 지연) | Accepted | 2026-02-05 |
| [0018](0018-dockerfile-shell-injection-mitigation.md) | Dockerfile CMD 쉘 인젝션 완화 전략 | Accepted | 2026-02-06 |
| [0019](0019-watchlist-api-retry-strategy.md) | Watchlist API N+1 호출 패턴 개선 (재시도 전략 채택) | Accepted | 2026-02-06 |

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