# CAA Collector Service - Milestone

> **프로젝트 중장기 로드맵 및 Phase별 진행 상황 관리**

---

## Vision Statement

**"관심종목만 등록하면 AI가 매수/매도 시점을 카카오톡으로 알림"**

사용자는 한국투자증권 계정에 관심종목을 등록하기만 하면, 시스템이 자동으로 데이터를 수집하고 AI가 학습/예측하여 투자 시점을 카카오톡으로 실시간 알림합니다. Collector Service는 이 전체 프로세스의 **컨트롤 타워**로서 데이터 수집부터 워크플로우 오케스트레이션까지 담당합니다.

---

## Phase 1: 데이터 수집 인프라 구축 ✅ 완료

**목표**: KIS API 연동 및 데이터 수집 파이프라인 구축

### 기간
- 시작: 2026-01-12 (월)
- 종료: 2026-01-25 (일)
- **진행률: 95%**

### 주요 성과

#### ✅ KIS API 연동 (100%)
- OAuth2 토큰 발급 및 자동 갱신
- Redis 기반 토큰 캐싱 (TTL 관리)
- 계정별 토큰 락 (분산 환경 동시성 제어)
- Rate Limiter 구현 (초당 20회 제한)

#### ✅ Watchlist 도메인 (100%)
- Entity 설계: `WatchlistGroup`, `WatchlistStock`
- Repository 구현 (Spring Data JPA)
- WatchlistService 동기화 로직
  - KIS API → MySQL 동기화
  - 그룹/종목 자동 생성 및 업데이트
  - 백필 상태 플래그 관리
- WatchlistScheduler (08:00, 18:00 동기화, 현재 비활성화)

#### ✅ StockPrice 도메인 (100%)
- 4가지 Entity 설계:
  - `DomesticStockDailyPrice` (국내 주식)
  - `DomesticIndexDailyPrice` (국내 지수)
  - `OverseasStockDailyPrice` (해외 주식)
  - `OverseasIndexDailyPrice` (해외 지수)
- Repository 구현 (각 타입별)
- StockPriceCollectorService:
  - 일간 가격 수집
  - 과거 데이터 백필 (30일)
  - 중복 체크 및 배치 저장
- StockBackfillService (백필 전용 로직 분리)
- StockPriceScheduler:
  - 백필: 03:00 (현재 비활성화)
  - 일간 수집: 18:30 (활성화)

#### ✅ 공통 인프라 (100%)
- BaseEntity (생성/수정 시간 자동 관리)
- JPA Converter (MarketCode, AssetType)
- RestClient 설정 (타임아웃, 에러 핸들링)
- ShedLock 분산 락 설정
- RateLimiter 설정 (KIS API 20/s)

#### ✅ 테스트 (80% 커버리지)
- 단위 테스트: Mockito 기반 서비스 로직 검증
- 통합 테스트: WireMock 기반 KIS API 모킹

### 잔여 작업 (5%)
- ❌ 관심종목 편집 반영 (그룹명/종목 수정, 삭제 감지) → Phase 2 Week 1로 이관

---

## Phase 2: 실시간 시세 및 외부 서비스 통신 🚧 진행 중

**목표**: 실시간 데이터 수집, AI Advisor/Notifier 연동, 투자 상태 관리

### 기간
- 시작: 2026-01-26 (월)
- 종료 예정: 2026-02-22 (일)
- **진행률: 0%**

### Week 1: 문서화 + 관심종목 편집 반영 (2026-01-26 월 ~ 02-01 일)

#### ❌ 프로젝트 문서화 (0%)
- [ ] `docs/README.md` - 프로젝트 첫 진입자 가이드
- [ ] `docs/MILESTONE.md` - 중장기 로드맵
- [ ] `docs/TODO.md` - 단기 작업 목록
- [ ] `docs/PRD.md` - 제품 요구사항 정의
- [ ] `docs/TECHSPEC.md` - 기술 명세

**예상 시간**: 3.5시간

#### ❌ 관심종목 편집 반영 (0%)
- [ ] DB 스키마 변경 (필요 시 Migration)
- [ ] Entity 수정 (WatchlistGroup, WatchlistStock)
- [ ] Repository 메서드 추가 (update, delete)
- [ ] WatchlistService 편집 감지 로직
  - 그룹명 변경 감지
  - 종목 추가/삭제 감지
  - 백필 플래그 재설정
- [ ] 테스트 작성 (편집 시나리오)

**예상 시간**: 8시간

**Week 1 총합**: 약 11시간

### Week 2: 실시간 시세 조회 (2026-02-02 월 ~ 02-08 일)

#### ❌ 실시간 시세 기능 (0%)
- [ ] KIS API 실시간 엔드포인트 연동
  - GET `/uapi/domestic-stock/v1/quotations/inquire-price` (국내 주식)
  - GET `/uapi/overseas-price/v1/quotations/price` (해외 주식)
- [ ] RealtimePrice Entity/Repository 설계
- [ ] RealtimePriceService 구현
- [ ] RealtimePriceScheduler 구현
  - cron: `*/1 9-15 * * MON-FRI` (장중 1분 간격)
- [ ] 테스트 작성

**예상 시간**: 10시간

### Week 3-4: AI Advisor/Notifier 통신 + 투자 상태 관리 (2026-02-09 월 ~ 02-22 일)

#### ❌ InvestmentDecision 엔티티 설계 (0%)
- [ ] Entity 설계 (투자 상태 저장)
  - `stock_code`, `trade_date`, `decision_type` (BUY/SELL/HOLD)
  - `confidence_score`, `predicted_price`, `ai_model_version`
- [ ] Repository 구현
- [ ] DDL 작성

**예상 시간**: 4시간

#### ❌ AI Advisor Client 구현 (0%)
- [ ] AdvisorClient Interface 정의
- [ ] AdvisorRestClient 구현
  - POST `/v1/train` - 학습 요청
  - POST `/v1/predict` - 예측 요청
- [ ] AdvisorProperties 설정 (base-url, timeout)
- [ ] RateLimiter 설정 (10/s)
- [ ] 에러 핸들링 (Timeout → UNKNOWN)
- [ ] 재시도 로직 (지수 백오프)
- [ ] 테스트 작성 (WireMock)

**예상 시간**: 6시간

#### ❌ Notifier Client 구현 (0%)
- [ ] NotifierClient Interface 정의
- [ ] NotifierRestClient 구현
  - POST `/v1/notify` - 알림 발송 요청
- [ ] NotifierProperties 설정 (base-url, timeout)
- [ ] RateLimiter 설정 (5/s)
- [ ] 에러 핸들링 및 재시도 로직
- [ ] 테스트 작성 (WireMock)

**예상 시간**: 6시간

#### ❌ InvestmentService 구현 (0%)
- [ ] 이전 상태 조회 로직
- [ ] 현재 상태와 비교 로직
- [ ] 상태 변화 감지 알고리즘
- [ ] 알림 조건 판단 (HOLD → BUY/SELL)
- [ ] 테스트 작성

**예상 시간**: 8시간

#### ❌ WorkflowOrchestrator 구현 (0%)
- [ ] 일간 워크플로우 구현:
  1. 가격 수집 완료 확인
  2. AI Advisor 학습 요청
  3. AI Advisor 예측 요청
  4. 상태 변화 감지
  5. Notifier 알림 요청
- [ ] WorkflowScheduler (18:35 실행)
- [ ] 에러 핸들링 (각 단계별 실패 처리)
- [ ] 테스트 작성 (통합 시나리오)

**예상 시간**: 12시간

**Week 3-4 총합**: 약 42시간

### Phase 2 총 예상 시간
- Week 1: 11시간
- Week 2: 10시간
- Week 3-4: 42시간
- **총합: 63시간**

---

## Phase 3: 주문 실행 및 안정성 고도화 ❌ 계획

**목표**: 자동 주문 실행, Circuit Breaker, Distributed Tracing

### 기간
- 시작 예정: 2026-02-23 (월)
- 종료 예정: 2026-03-29 (일)
- **진행률: 0%**

### 주요 작업

#### ❌ 주문 실행 기능 (0%)
- [ ] Notifier 주문 요청 수신 API
  - POST `/collector/v1/order` (주문 실행 요청)
  - Request: `stock_code`, `order_type` (BUY/SELL), `quantity`, `price`
- [ ] KIS API 주문 실행
  - POST `/uapi/domestic-stock/v1/trading/order-cash` (국내 주식 매수/매도)
  - POST `/uapi/overseas-stock/v1/trading/order` (해외 주식 매수/매도)
- [ ] OrderExecution Entity (주문 이력 저장)
- [ ] OrderService 구현
  - 주문 검증 (잔고 확인, 수량 검증)
  - 주문 실행 및 결과 저장
- [ ] 테스트 작성

**예상 시간**: 16시간

#### ❌ Circuit Breaker 구현 (0%)
- [ ] Resilience4j 통합
- [ ] AI Advisor Circuit Breaker 설정
  - Failure Rate Threshold: 50%
  - Wait Duration: 60s
- [ ] Notifier Circuit Breaker 설정
- [ ] Fallback 로직 (Circuit Open 시 알림)

**예상 시간**: 6시간

#### ❌ Distributed Tracing (0%)
- [ ] Spring Cloud Sleuth 통합
- [ ] Trace-ID 전파 (Collector → AI Advisor/Notifier)
- [ ] 로그 포맷 개선 (JSON, 구조화)

**예상 시간**: 8시간

#### ❌ Health Check 개선 (0%)
- [ ] `/health` 엔드포인트 확장
  - KIS API 연결 상태
  - Redis 연결 상태
  - MySQL 연결 상태
  - AI Advisor/Notifier 연결 상태
- [ ] Actuator 설정

**예상 시간**: 4시간

#### ❌ 알림 재시도 큐 (0%)
- [ ] Redis 기반 실패 알림 큐
- [ ] 재시도 스케줄러 (5분 간격)
- [ ] 최대 재시도 횟수 설정 (3회)

**예상 시간**: 8시간

### Phase 3 총 예상 시간
- **총합: 42시간**

---

## Phase 4: 비동기 통신 전환 ❌ 미래

**목표**: REST → Kafka/RabbitMQ 전환, 이벤트 기반 아키텍처

### 기간
- 시작 예정: 2026-04 이후
- **진행률: 0%**

### 주요 작업

- [ ] Kafka/RabbitMQ 도입
- [ ] 이벤트 스키마 정의
- [ ] Producer/Consumer 구현
- [ ] Dead Letter Queue 설정
- [ ] 이벤트 소싱 아키텍처 검토

**예상 시간**: TBD

---

## Progress Tracking

| Phase | 기간 | 진행률 | 상태 |
|-------|------|--------|------|
| Phase 1: 데이터 수집 인프라 | 2026-01-12 ~ 01-25 (2주) | 95% | ✅ 완료 |
| Phase 2 Week 1: 문서화 + 관심종목 편집 | 2026-01-26 ~ 02-01 (1주) | 0% | 🚧 진행 예정 |
| Phase 2 Week 2: 실시간 시세 조회 | 2026-02-02 ~ 02-08 (1주) | 0% | ❌ 미시작 |
| Phase 2 Week 3-4: AI/Notifier 통신 | 2026-02-09 ~ 02-22 (2주) | 0% | ❌ 미시작 |
| Phase 3: 주문 실행 및 안정성 고도화 | 2026-02-23 ~ 03-29 (5주) | 0% | ❌ 미시작 |
| Phase 4: 비동기 통신 전환 | 2026-04 이후 | 0% | ❌ 미시작 |

---

## Notes

- Phase 1 완료 후 문서화 작업을 최우선으로 진행
- Phase 2는 4주간 집중 개발 (주당 15-20시간)
- Phase 3는 5주간 안정성 고도화 (주당 8-10시간)
- Phase 4는 아키텍처 전환으로 장기 계획 (3개월 이상)
