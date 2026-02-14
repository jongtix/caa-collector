# CAA Collector Service - TODO

> **현재 Phase 2의 단기 작업 목록 및 우선순위 관리**

**작성자**: jongtix + Claude (pm)
**Last Updated**: 2026-02-14

---

## Header

- **Last Updated**: 2026-02-14 (토)
- **Current Phase**: Phase 2 Week 2-3 (진행률 78%)
- **Focus**: 배포 인프라 완성 (Docker → CI/CD → Watchtower)
- **Deadline**: 2026-02-22 (일)
- **Remaining**: 12.5시간 (8일)

---

## 🎯 지금 해야 할 일 (This Week: 2026-02-13 ~ 02-22)

### 배포 인프라 완성 (총 21.5시간)

- [x] **Dockerfile 최적화 완료** (0.5시간) ✅ 2026-02-13
  - [x] main 브랜치 머지 완료
  - [x] 국내 지수 수집 미작동 이슈 문서화 (domestic-index-collection-issue.md)
  - 참조: [ADR-0018: Dockerfile Shell Injection 완화](adr/0018-dockerfile-shell-injection-mitigation.md)

- [x] **CI/CD 파이프라인** (9시간) ✅ 2026-02-14
  - [x] Reusable Workflow CI/CD 전략 (ADR-0009) ✅ 2026-02-10
  - [x] Docker CI/CD 자동화 전략 (ADR-0020) ✅ 2026-02-13
  - [x] GitHub Actions 워크플로우 (release-please + Docker Hub 푸시) ✅ 2026-02-14

- [ ] **Watchtower 자동 배포** (10시간)
  - [ ] 이미지 갱신 감지 + 자동 재시작
  - [ ] Health Check 엔드포인트 확장
  - [ ] 롤백 전략 설계 및 테스트

- [ ] **모니터링 및 관리** (3.5시간)
  - [ ] JSON 로그 포맷 + 레벨 표준화
  - [ ] MySQL 백업 스크립트 + 복구 테스트
  - ~~Portainer 대시보드 구성~~ → Phase 3으로 연기

- [ ] **문서화** (6.5시간)
  - [x] 국내 지수 수집 이슈 분석 문서 (domestic-index-collection-issue.md) ✅ 2026-02-13
  - [x] ADR-0020 작성 (Docker CI/CD 자동화 전략) ✅ 2026-02-13
  - [x] DEVELOPMENT.md 작성 (Conventional Commits + release-please 가이드) ✅ 2026-02-13
  - [ ] DEPLOYMENT.md 작성 (배포 가이드, 트러블슈팅 FAQ)

- [ ] **스케줄 리팩터링** (1시간)
  - MA-19: DateFormatConstants.KST_ZONE_ID 전역 적용 확인
  - 참조: [ADR-0016: 글로벌 주식 시장 스케줄](adr/0016-global-market-schedule-architecture.md)

---

## 📅 다음 작업 (배포 완료 후, Phase 3 시작 전)

### 데이터 수집 안정성 (총 9시간)

- [ ] **국내 지수 수집 미작동 조사** (2시간, Phase 3 시작 전)
  - 현상: 실제 운영 시 국내 지수(KOSPI, KOSDAQ 등) 일간 가격 수집이 안 됨
  - 추정 원인:
    1. 관심종목 데이터의 AssetType 매핑 오류 (fidMrktClsCode ≠ "U"일 가능성)
    2. DomesticIndexStrategy 실행 누락 (StockPriceStrategyFactory 매핑 확인)
    3. KIS API 엔드포인트 또는 권한 문제
  - 확인 필요:
    - [ ] DB에 저장된 국내 지수의 assetType 값 확인 (SELECT * FROM watchlist_stock WHERE stock_code IN ('0001', '1001'))
    - [ ] 실제 API 응답의 fidMrktClsCode 값 로그 확인 (WatchlistMapper.toWatchlistStock)
    - [ ] StockPriceScheduler 실행 로그 확인 (collectDailyPrices, backfillHistoricalPrices)
    - [ ] KisStockPriceService.getDomesticIndexDailyPrices 호출 여부 확인
  - 관련 파일:
    - AssetType.java: FID_MARKET_CLASS_MAP ("U" → DOMESTIC_INDEX)
    - WatchlistMapper.java: L45 (assetType 매핑)
    - DomesticIndexStrategy.java
    - StockPriceStrategyFactory.java: L36 (DOMESTIC_INDEX → domesticIndexStrategy)
  - 참조: [TECHSPEC.md - 주식 가격 수집](TECHSPEC.md#주식-가격-수집-stock-price-collection)

- [ ] **MA-01: Watchlist API 재시도 로직** (7시간)
  - N+1 호출 패턴 개선 (재시도 전략)
  - 참조: [ADR-0019](adr/0019-watchlist-api-retry-strategy.md)

---

## 💭 나중에 (Later: Phase 3+)

### Phase 3: 실시간 데이터 수집 (2026-02-23 ~ 03-01, 15시간)
- [ ] Discord/Slack 빌드 알림 (Phase 2에서 연기)
- [ ] WebSocket 기반 실시간 시세 (KIS API 승인키 발급)
- [ ] RealtimePrice Entity/Repository 설계
- [ ] 5초 샘플링 + 배치 저장

### Phase 4: AI Advisor 개발 및 연동 (2026-03-02 ~ 03-22, 45시간)
- [ ] AdvisorClient 구현 (학습/예측 API)
- [ ] InvestmentDecision Entity 설계
- [ ] WorkflowOrchestrator 구현 (가격 수집 → AI 판단)

### Phase 5: Notifier 개발 및 연동 - MVP (2026-03-23 ~ 04-05, 30시간)
- [ ] NotifierClient 구현 (알림 발송 API)
- [ ] InvestmentService 상태 변화 감지
- [ ] 카카오톡 알림 템플릿 설계

---

## 📌 참고

### 기술 결정 (ADR)
- [MSA/ADR-0011: Docker Hub 공개 배포 전략](../../docs/adr/ADR-0011-docker-hub-public-deployment.md)
- [ADR-0016: 글로벌 시장 스케줄 아키텍처](adr/0016-global-market-schedule-architecture.md)
- [ADR-0017: Database Migration Strategy](adr/0017-database-migration-strategy.md)
- [ADR-0018: Dockerfile Shell Injection 완화](adr/0018-dockerfile-shell-injection-mitigation.md)
- [ADR-0019: Watchlist API 재시도 전략](adr/0019-watchlist-api-retry-strategy.md)

### 보안 처리 방침
- **Critical/High 등급**: ✅ 완료 (H-01, H-02, H-04)
- **Medium/Low 등급**: Phase 3 시작 전 (2026-02-22~23) 일괄 정리
- 상세: [MILESTONE.md - Phase 2 Week 1](MILESTONE.md#week-1-문서화--관심종목-편집-반영-2026-01-26-월--02-01-일)

### 진행 상황
- Phase 2 진행률: **76%** (43.3h / 57.3h)
- Week 1 완료: 문서화 (3.5h) + 관심종목 편집 (8h) + 보안 (1.8h) = 13.3h ✅
- Week 2 완료: Docker Hub (0.5h) + 컨테이너화/보안/성능 (12.5h) + 문서화 (5.5h) = 18.5h ✅
- Week 2-3 완료: CI/CD 보안 스캔 (1.5h) + Dockerfile 최적화 (1.0h) + CI/CD 파이프라인 (9.0h) = 11.5h ✅
- Week 2-3 남은 작업: 12.5시간 (배포 우선)
  - Watchtower 자동 배포 (10h)
  - 모니터링 및 관리 (3.5h)
  - 문서화 (6.5h)
  - 스케줄 리팩터링 (1h)
- 배포 후 작업: 9시간 (Phase 3 시작 전)
  - 국내 지수 수집 조사 (2h)
  - Watchlist API 재시도 (7h)
- MVP 목표: 2026-04-05 (Phase 5 완료)

---

## Notes

- **완료된 작업**: [MILESTONE.md](MILESTONE.md) 참조
- **상세 기술 명세**: [TECHSPEC.md](TECHSPEC.md) 참조
- **작업 원칙**: 한 번에 하나의 Priority에 집중, 문서화 먼저
