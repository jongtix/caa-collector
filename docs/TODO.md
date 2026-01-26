# CAA Collector Service - TODO

> **현재 Phase 2의 단기 작업 목록 및 우선순위 관리**

---

## Header

- **Last Updated**: 2026-01-24 (금)
- **Current Focus**: Phase 2 Week 1 준비 (문서화 + 관심종목 편집 반영)
- **Next Sprint**: 2026-01-26 (월) ~ 2026-02-01 (일)

---

## Priority 0 (P0) - Critical
> **Week 1: 2026-01-26 (월) ~ 02-01 (일) - 11시간**

### 📝 프로젝트 문서화 (3.5시간)
- [ ] `docs/README.md` 작성 (30분)
  - Project Overview (역할/책임 범위 명시)
  - Architecture Context (MSA 다이어그램)
  - Tech Stack
  - Quick Start (환경 변수, 빌드/실행 명령어)
  - Directory Structure
  - Key Features (구현 완료/미완료 구분)
- [ ] `docs/MILESTONE.md` 작성 (40분)
  - Vision Statement
  - Phase 1 완료 내역 (2026-01-12 ~ 01-25)
  - Phase 2 Week별 계획 (2026-01-26 ~ 02-22)
  - Phase 3 로드맵 (2026-02-23 ~ 03-29)
  - Progress Tracking 표
- [ ] `docs/TODO.md` 작성 (20분)
  - P0/P1/P2 우선순위 분류
  - Week별 작업 목록
  - 예상 시간
- [ ] `docs/PRD.md` 작성 (50분)
  - Executive Summary
  - User Stories (5가지 시나리오)
  - Functional Requirements (FR-1~4, 주문 실행 포함)
  - Non-Functional Requirements (NFR-1~4)
  - Constraints, Success Metrics
- [ ] `docs/TECHSPEC.md` 작성 (60분)
  - System Architecture 다이어그램
  - Database Schema (기존 + InvestmentDecision DDL)
  - API Specifications (KIS, AI Advisor, Notifier)
  - Scheduler 명세
  - Error Handling & Retry 전략
  - Configuration 예시

### 🔧 관심종목 편집 반영 (8시간)
- [ ] DB 스키마 변경 (1시간)
  - Migration 스크립트 작성 (필요 시)
  - `watchlist_group` 컬럼 검토
  - `watchlist_stock` 컬럼 검토
- [ ] Entity 수정 (1시간)
  - `WatchlistGroup.java`: 그룹명 변경 메서드
  - `WatchlistStock.java`: 종목 정보 업데이트 메서드
- [ ] Repository 메서드 추가 (1.5시간)
  - `WatchlistGroupRepository`: 그룹 삭제 메서드
  - `WatchlistStockRepository`: 종목 삭제 메서드
  - 배치 업데이트 쿼리 최적화
- [ ] WatchlistService 로직 구현 (3시간)
  - 그룹명 변경 감지 로직
  - 종목 추가/삭제 감지 로직
  - 백필 플래그 재설정 로직
  - 트랜잭션 처리
- [ ] 테스트 작성 (1.5시간)
  - 그룹명 변경 시나리오 테스트
  - 종목 추가 시나리오 테스트
  - 종목 삭제 시나리오 테스트
  - 백필 플래그 재설정 테스트

---

## Priority 1 (P1) - Important
> **Week 2: 2026-02-02 (월) ~ 02-08 (일) - 10시간**

### 📡 실시간 시세 조회 기능 (10시간)
- [ ] KIS API 연동 (3시간)
  - 국내 주식 실시간 시세 엔드포인트 조사
    - GET `/uapi/domestic-stock/v1/quotations/inquire-price`
  - 해외 주식 실시간 시세 엔드포인트 조사
    - GET `/uapi/overseas-price/v1/quotations/price`
  - KisStockPriceService에 실시간 조회 메서드 추가
- [ ] RealtimePrice Entity/Repository 설계 (2시간)
  - `RealtimeStockPrice` Entity 설계
    - `stock_code`, `current_price`, `change_rate`, `volume`, `timestamp`
  - Repository 구현 (upsert 로직)
  - DDL 작성
- [ ] RealtimePriceService 구현 (2.5시간)
  - 실시간 시세 조회 로직
  - 배치 처리 (한 번에 여러 종목)
  - 에러 핸들링 (개별 종목 실패 시 다음 종목 계속)
- [ ] RealtimePriceScheduler 구현 (1.5시간)
  - cron 설정: `*/1 9-15 * * MON-FRI` (장중 1분 간격)
  - ShedLock 적용
  - 로깅 및 모니터링
- [ ] 테스트 작성 (1시간)
  - 단위 테스트 (Mockito)
  - 통합 테스트 (WireMock)

---

## Priority 2 (P2) - Nice to Have
> **Week 3-4: 2026-02-09 (월) ~ 02-22 (일) - 42시간**

### 📊 InvestmentDecision 엔티티 설계 및 구현 (4시간)
- [ ] Entity 설계 (2시간)
  - 컬럼 정의:
    - `id` (BIGINT, PK)
    - `stock_code` (VARCHAR, 종목 코드)
    - `trade_date` (DATE, 거래일)
    - `decision_type` (VARCHAR, BUY/SELL/HOLD)
    - `confidence_score` (DECIMAL, 신뢰도)
    - `predicted_price` (DECIMAL, 예측 가격)
    - `ai_model_version` (VARCHAR, AI 모델 버전)
    - `created_at`, `updated_at`
  - Enum 클래스: `InvestmentState` (BUY, SELL, HOLD, UNKNOWN)
  - DDL 작성
- [ ] Repository 구현 (1시간)
  - 최신 상태 조회 메서드
  - 날짜 범위 조회 메서드
- [ ] 테스트 작성 (1시간)

### 🤖 AI Advisor Client 구현 (6시간)
- [ ] Interface 정의 (0.5시간)
  - `AdvisorClient` Interface
  - `TrainRequest`, `TrainResponse` record
  - `PredictRequest`, `PredictResponse` record
- [ ] RestClient 구현 (2.5시간)
  - POST `/v1/train` 호출
  - POST `/v1/predict` 호출
  - Timeout 설정 (30초)
  - 재시도 로직 (지수 백오프: 1s, 2s, 4s)
- [ ] 설정 및 RateLimiter (1시간)
  - `AdvisorProperties` (base-url, timeout)
  - RateLimiter 설정 (10/s)
- [ ] 에러 핸들링 (1시간)
  - Timeout → UNKNOWN 상태 반환
  - 5xx 에러 재시도
  - Circuit Breaker (Phase 3로 이관)
- [ ] 테스트 작성 (1시간)
  - WireMock 기반 통합 테스트
  - Timeout 시나리오 테스트

### 💬 Notifier Client 구현 (6시간)
- [ ] Interface 정의 (0.5시간)
  - `NotifierClient` Interface
  - `NotifyRequest`, `NotifyResponse` record
- [ ] RestClient 구현 (2.5시간)
  - POST `/v1/notify` 호출
  - Timeout 설정 (10초)
  - 재시도 로직 (지수 백오프: 1s, 2s, 4s)
- [ ] 설정 및 RateLimiter (1시간)
  - `NotifierProperties` (base-url, timeout)
  - RateLimiter 설정 (5/s)
- [ ] 에러 핸들링 (1시간)
  - 3회 재시도 후 실패 로그
  - 실패 시 재시도 큐 추가 (Phase 3로 이관)
- [ ] 테스트 작성 (1시간)
  - WireMock 기반 통합 테스트
  - 재시도 시나리오 테스트

### 🔍 InvestmentService 구현 (8시간)
- [ ] 이전 상태 조회 로직 (2시간)
  - `InvestmentDecision` Repository 쿼리
  - 최신 상태 조회 (종목별)
- [ ] 상태 비교 로직 (2시간)
  - 이전 상태 vs 현재 상태 비교
  - 변화 감지 알고리즘
- [ ] 알림 조건 판단 (2시간)
  - HOLD → BUY: 매수 알림
  - HOLD → SELL: 매도 알림
  - BUY → SELL: 포지션 변경 알림
  - 변화 없음: 알림 스킵
- [ ] 테스트 작성 (2시간)
  - 상태 변화 시나리오 (10가지)
  - 알림 조건 검증

### 🎯 WorkflowOrchestrator 구현 (12시간)
- [ ] 일간 워크플로우 구현 (8시간)
  1. 가격 수집 완료 확인 (1시간)
     - StockPriceScheduler 완료 여부 확인
  2. AI Advisor 학습 요청 (1.5시간)
     - 전체 종목 데이터 전달
     - 학습 완료 대기
  3. AI Advisor 예측 요청 (2시간)
     - 종목별 예측 요청
     - 배치 처리 (10개씩)
  4. 상태 변화 감지 (1.5시간)
     - InvestmentService 호출
     - 변화 종목 필터링
  5. Notifier 알림 요청 (2시간)
     - 변화 종목별 알림 발송
     - 에러 핸들링
- [ ] WorkflowScheduler 구현 (2시간)
  - cron 설정: `0 35 18 * * ?` (18:35 실행)
  - ShedLock 적용
  - 각 단계별 로깅
- [ ] 에러 핸들링 (1시간)
  - 각 단계별 실패 시 중단 여부 결정
  - 실패 로그 및 알림
- [ ] 테스트 작성 (1시간)
  - 통합 시나리오 테스트
  - 에러 시나리오 테스트

### 📋 InvestmentDecision 상태 저장 (6시간)
- [ ] AI Advisor 응답 파싱 (1.5시간)
  - PredictResponse → InvestmentDecision 변환
  - Enum 매핑 (BUY/SELL/HOLD)
- [ ] 상태 저장 로직 (2시간)
  - 배치 저장 (여러 종목)
  - 중복 체크 (upsert)
- [ ] 히스토리 관리 (1.5시간)
  - 이전 상태 아카이빙
  - 30일 이상 데이터 정리
- [ ] 테스트 작성 (1시간)

---

## Backlog
> **Phase 3: 2026-02-23 (월) ~ 03-29 (일)**

### 📦 주문 실행 기능
- [ ] Notifier 주문 요청 수신 API
  - POST `/collector/v1/order` 엔드포인트
  - Request: `stock_code`, `order_type`, `quantity`, `price`
- [ ] KIS API 주문 실행
  - POST `/uapi/domestic-stock/v1/trading/order-cash` (국내 매수/매도)
  - POST `/uapi/overseas-stock/v1/trading/order` (해외 매수/매도)
- [ ] OrderExecution Entity (주문 이력 저장)
- [ ] OrderService 구현
  - 주문 검증 (잔고, 수량)
  - 주문 실행 및 결과 저장

### 🛡️ Circuit Breaker
- [ ] Resilience4j 통합
- [ ] AI Advisor Circuit Breaker 설정
  - Failure Rate: 50%
  - Wait Duration: 60s
- [ ] Notifier Circuit Breaker 설정
- [ ] Fallback 로직

### 📍 Distributed Tracing
- [ ] Spring Cloud Sleuth 통합
- [ ] Trace-ID 전파 (Collector → AI/Notifier)
- [ ] JSON 로그 포맷 개선

### ❤️ Health Check 개선
- [ ] `/health` 엔드포인트 확장
  - KIS API 연결 상태
  - Redis, MySQL 연결 상태
  - AI Advisor, Notifier 연결 상태
- [ ] Actuator 설정

### 🔁 알림 재시도 큐
- [ ] Redis 기반 실패 알림 큐
- [ ] 재시도 스케줄러 (5분 간격)
- [ ] 최대 재시도 3회

---

## Completed (This Week: 2026-01-26 월 ~ 02-01 일)

### ✅ StockPrice 도메인 리팩터링 (2026-01-26)
- [x] Strategy Pattern 도입 (6개 파일 생성)
  - `StockPriceStrategy.java` 인터페이스
  - `DomesticStockStrategy.java` / `DomesticIndexStrategy.java`
  - `OverseasStockStrategy.java` / `OverseasIndexStrategy.java`
  - `StockPriceStrategyFactory.java`
- [x] 함수형 인터페이스 추가 (2개)
  - `TriFunction.java` / `QuadFunction.java`
- [x] Date Parsing 중앙화
  - `StockPriceConstants.parseDate()` 도입
- [x] Generic 메서드 도입
  - Backfill/Persistence 메서드 통합
- [x] StockPriceFetchService 제거
  - 불필요한 위임 레이어 제거
- [x] ADR 0009 작성
  - Strategy Pattern 도입 결정 기록
- [x] 문서 업데이트
  - `TECHSPEC.md` (strategy 패키지 추가)
  - `TODO.md` (리팩터링 작업 완료 기록)
  - `CLAUDE.md` (Architecture 섹션 업데이트)

## Completed (Previous Week: 2026-01-19 월 ~ 01-25 일)

### ✅ Phase 1 완료 항목
- [x] KIS API OAuth2 토큰 발급/갱신
- [x] Redis 토큰 캐싱 (TTL 관리)
- [x] 계정별 토큰 락 (분산 환경 동시성 제어)
- [x] Rate Limiter 구현 (KIS 20/s)
- [x] WatchlistGroup/WatchlistStock Entity 설계
- [x] Watchlist 동기화 로직 (KIS API → MySQL)
- [x] 4가지 StockDailyPrice Entity 설계
  - DomesticStockDailyPrice
  - DomesticIndexDailyPrice
  - OverseasStockDailyPrice
  - OverseasIndexDailyPrice
- [x] 일간 가격 수집 (18:30)
- [x] 과거 데이터 백필 (03:00)
- [x] StockPriceScheduler 구현 (ShedLock)
- [x] 단위/통합 테스트 작성 (커버리지 80%)
- [x] BaseEntity, JPA Converter, RestClient 설정

---

## Notes

- **우선순위 기준**:
  - P0: 현재 주차 필수 작업
  - P1: 다음 주차 중요 작업
  - P2: 다다음 주차 이후 작업
- **예상 시간**:
  - Week 1 (P0): 11시간
  - Week 2 (P1): 10시간
  - Week 3-4 (P2): 42시간
- **작업 원칙**:
  - 한 번에 하나의 Priority에 집중
  - 각 작업 완료 후 즉시 테스트 작성
  - 문서화 먼저, 코딩은 나중
