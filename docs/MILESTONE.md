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
- **진행률: 100%**

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

### 잔여 작업
- ✅ 관심종목 편집 반영 (그룹명/종목 수정, 삭제 감지) → Phase 2 Week 1에서 완료

---

## Phase 2: 배포 및 운영 인프라 🚧 진행 중

**목표**: Docker 컨테이너화, CI/CD 파이프라인, 자동 배포 체계 구축

### 기간
- 시작: 2026-01-28 (화)
- 종료 예정: 2026-02-22 (일)
- **예상 시간: 55.5시간**
- **진행률: 20%** (Week 1 완료)

### Week 1: 문서화 + 관심종목 편집 반영 (2026-01-26 월 ~ 02-01 일)

#### ✅ 프로젝트 문서화 (100%)
- [x] `README.md` - 프로젝트 첫 진입자 가이드
- [x] `docs/MILESTONE.md` - 중장기 로드맵
- [x] `docs/TODO.md` - 단기 작업 목록
- [x] `docs/PRD.md` - 제품 요구사항 정의
- [x] `docs/TECHSPEC.md` - 기술 명세

**예상 시간**: 3.5시간 | **실제 시간**: 3.5시간

#### ✅ 관심종목 편집 반영 (100%)
- [x] DB 스키마 변경 (필요 시 Migration) - 스키마 변경 불필요
- [x] Entity 수정 (WatchlistGroup, WatchlistStock) - 기존 엔티티 활용
- [x] Repository 메서드 추가 (update, delete)
  - `WatchlistGroupRepository.deleteByGroupId()`
  - `WatchlistStockRepository.deleteByStockCode()`
- [x] WatchlistService 3-way 동기화 로직 구현
  - API 기준 삭제 전략 (API에 없으면 DB 삭제)
  - 종목 추가/업데이트 로직
  - backfillCompleted 플래그 보존
  - null/중복 stockCode 방어적 처리
- [x] 테스트 작성 (31개 통과)
  - 단위 테스트 26개
  - 통합 테스트 5개

**예상 시간**: 8시간 | **실제 시간**: 8시간

**Week 1 총합**: 약 11.5시간 (관심종목 편집 8시간 + 문서화 3.5시간)

### Week 2-3: 배포 자동화 전체 (2026-01-28 화 ~ 02-22 일)

> **⚠️ 참고**: 이 작업은 **MSA 공통 인프라**이지만, Collector 우선 적용합니다.
> - Docker Compose: MySQL, Redis 등 공통 인프라 포함
> - GitHub Actions: MSA 루트 레포지토리에 워크플로우 생성
> - 향후 AI Advisor, Notifier 추가 시 재사용 가능

#### ❌ 1. 컨테이너화 (0%, 8시간)
- [ ] Dockerfile 작성 (Multi-stage build)
  - Spring Boot 최적화
  - 레이어 캐싱 전략
- [ ] Docker Compose 구성
  - MySQL 8.0
  - Redis 7.0
  - Collector 서비스
- [ ] 환경 변수 설정
  - `.env` 파일 구조화
  - KIS API 인증 정보
  - DB 연결 정보
- [ ] 로컬 테스트 및 디버깅

**예상 시간**: 8시간

#### ❌ 2. CI/CD 파이프라인 (0%, 9시간)
- [ ] GitHub Actions 워크플로우 작성
  - 빌드 단계 (Gradle build)
  - 테스트 단계 (unit + integration)
  - 이미지 푸시 단계
- [ ] Docker Registry 설정
  - GitHub Container Registry (ghcr.io)
  - 인증 설정
- [ ] 자동 버전 태깅
  - semantic versioning
  - git tag 연동
- [ ] 빌드 성공/실패 알림
  - Discord/Slack 웹훅
- [ ] 통합 테스트 및 디버깅

**예상 시간**: 9시간

#### ❌ 3. 자동 배포 (0%, 10시간)
- [ ] Watchtower 설정 및 테스트
  - 이미지 갱신 감지
  - 자동 컨테이너 재시작
- [ ] NAS 환경 배포 스크립트 작성
  - 초기 환경 설정
  - 시크릿 관리 (.env 안전 관리)
  - 네트워크 구성
- [ ] Health Check 엔드포인트 구현
  - `/actuator/health` 확장
  - `/actuator/readiness` 구현
  - KIS API 연결 상태
  - Redis/MySQL 연결 상태
- [ ] 롤백 전략 설계 및 테스트
  - 이전 이미지 버전 복구 프로세스
  - 배포 실패 시 자동 롤백

**예상 시간**: 10시간

#### ❌ 4. 모니터링 및 관리 (0%, 10시간)
- [ ] Portainer 연동 및 대시보드 구성
  - 컨테이너 상태 시각화
  - 로그 뷰어 설정
- [ ] 로그 수집 전략 구현
  - JSON 포맷 로그 출력
  - 로그 레벨 표준화
  - 컨테이너 로그 수집
- [ ] 리소스 제한 설정
  - NAS 환경 (8GB RAM) 최적화
  - CPU 제한
  - 메모리 제한
- [ ] 백업 및 복구 프로세스
  - MySQL 백업 스크립트
  - 볼륨 백업 전략
  - 복구 테스트

**예상 시간**: 10시간

#### ❌ 5. 문서화 (0%, 7시간)
- [ ] DEPLOYMENT.md 작성
  - 배포 가이드
  - 환경 변수 설명
  - 트러블슈팅 FAQ
- [ ] ADR-001-deployment-automation.md
  - 배포 전략 결정 배경
  - Docker vs K8s 비교
  - Watchtower 선택 이유
- [ ] 로컬 개발 환경 가이드
  - Docker Compose로 전체 스택 실행
  - 개발 모드 vs 운영 모드

**예상 시간**: 7시간

**Week 2-3 총합**: 약 44시간

### Phase 2 총 예상 시간
- Week 1: 11.5시간 (완료)
- Week 2-3: 44시간 (진행 예정)
- **총합: 55.5시간**

---

## Phase 3: 실시간 데이터 수집 ❌ 계획

**목표**: WebSocket 기반 실시간 주가 데이터 수집

### 기간
- 시작 예정: 2026-02-23 (월)
- 종료 예정: 2026-03-01 (일)
- **예상 시간: 15시간**
- **진행률: 0%**

### 주요 작업

#### ❌ 실시간 시세 기능 (WebSocket, 0%)
- [ ] KIS WebSocket API 스펙 조사 및 승인키 발급
  - POST `/oauth2/Approval` (승인키 발급)
  - WebSocket 엔드포인트: `ws://ops.koreainvestment.com:21000`
  - TR 코드: `H0STCNT0` (실시간 체결가), `H0STASP0` (실시간 호가)
  - 구독 제한: 최대 20개
- [ ] WebSocketClient 구현
  - `KisWebSocketManager`: 연결 관리, 재연결 (지수 백오프)
  - `KisWebSocketHandler`: 메시지 핸들러
  - `KisWebSocketHealthIndicator`: Health Check
- [ ] RealtimePrice Entity/Repository 설계
  - `RealtimeStockPrice` Entity (upsert 방식)
  - 복합 유니크 제약: (stock_code, market_code)
- [ ] SubscriptionManager 및 MessageHandler 구현
  - `RealtimeSubscriptionService`: 구독 관리
  - `RealtimePriceMessageHandler`: 메시지 파싱 및 비동기 처리
- [ ] RealtimePriceService 통합 및 샘플링
  - `RealtimePriceSampler`: 5초 샘플링 (메모리 버퍼 → 배치 저장)
  - `TradingHoursScheduler`: 장 시작/종료 관리
- [ ] 테스트 작성
  - Mock WebSocket Server 구현
  - 재연결 시나리오 테스트
- [ ] ADR-0011 작성 완료 (REST vs WebSocket 결정 기록)

**예상 시간**: 15시간

---

## Phase 4: AI Advisor 개발 및 연동 ❌ 계획

**목표**: AI 기반 투자 판단 엔진 구축 및 Collector 연동

### 기간
- 시작 예정: 2026-03-02 (월)
- 종료 예정: 2026-03-22 (일)
- **예상 시간: 45시간**
- **진행률: 0%**

### Week 1: AI Advisor 서비스 기본 구조 (18시간)

#### ❌ AI Advisor 프로젝트 초기화 (0%)
- [ ] FastAPI 프로젝트 구조 생성
- [ ] Poetry 의존성 관리 설정
- [ ] Dockerfile 및 docker-compose 구성
- [ ] 환경 변수 설정

#### ❌ 데이터 로딩 로직 (0%)
- [ ] Collector MySQL 연결 (SQLAlchemy)
- [ ] 주가 데이터 조회 API
- [ ] 데이터 전처리 파이프라인

#### ❌ 간단한 투자 판단 로직 (0%)
- [ ] Rule-based MVP (이동평균선 기반)
- [ ] BUY/SELL/HOLD 판단 로직
- [ ] 신뢰도 점수 계산

#### ❌ REST API 엔드포인트 (0%)
- [ ] POST `/v1/train` - 학습 요청
- [ ] POST `/v1/predict` - 예측 요청
- [ ] GET `/health` - Health Check

#### ❌ 단위 테스트 (0%)
- [ ] pytest 기반 테스트 작성
- [ ] Mock 데이터 테스트

**Week 1 총합**: 18시간

### Week 2: Collector ↔ AI Advisor 통신 (15시간)

#### ❌ AdvisorClient 구현 (Collector, 0%)
- [ ] AdvisorClient Interface 정의
- [ ] AdvisorRestClient 구현
  - POST `/v1/train` - 학습 요청
  - POST `/v1/predict` - 예측 요청
- [ ] AdvisorProperties 설정 (base-url, timeout)
- [ ] RateLimiter 설정 (10/s)
- [ ] 에러 핸들링 (Timeout → UNKNOWN)
- [ ] 재시도 로직 (지수 백오프)

**예상 시간**: 6시간

#### ❌ InvestmentDecision Entity 설계 (0%)
- [ ] Entity 설계 (투자 상태 저장)
  - `stock_code`, `trade_date`, `decision_type` (BUY/SELL/HOLD)
  - `confidence_score`, `predicted_price`, `ai_model_version`
- [ ] Repository 구현
- [ ] DDL 작성

**예상 시간**: 4시간

#### ❌ WorkflowOrchestrator 구현 (0%)
- [ ] 일간 워크플로우 구현:
  1. 가격 수집 완료 확인
  2. AI Advisor 학습 요청
  3. AI Advisor 예측 요청
  4. 상태 변화 감지 (이전 vs 현재)
- [ ] WorkflowScheduler (18:35 실행)
- [ ] 에러 핸들링 (각 단계별 실패 처리)

**예상 시간**: 3시간

#### ❌ 통합 테스트 (0%)
- [ ] WireMock 기반 AI Advisor Mock
- [ ] End-to-End 시나리오 테스트

**예상 시간**: 2시간

**Week 2 총합**: 15시간

### Week 3: 통합 테스트 및 문서화 (12시간)

#### ❌ End-to-End 테스트 (0%)
- [ ] 전체 워크플로우 통합 테스트
- [ ] 에러 시나리오 테스트

**예상 시간**: 4시간

#### ❌ AI Advisor 문서화 (0%)
- [ ] README.md (AI Advisor 개요)
- [ ] TECHSPEC.md (기술 명세)
- [ ] API 문서 (Swagger/OpenAPI)

**예상 시간**: 4시간

#### ❌ 배포 설정 (0%)
- [ ] Dockerfile 최적화
- [ ] docker-compose 통합
- [ ] CI/CD 파이프라인 추가

**예상 시간**: 3시간

#### ❌ ADR-0012 작성 (0%)
- [ ] AI Advisor 통신 스펙 결정 기록

**예상 시간**: 1시간

**Week 3 총합**: 12시간

### Phase 4 총 예상 시간
- **총합: 45시간**

---

## Phase 5: Notifier 개발 및 연동 ❌ 계획 **(🎉 MVP)**

**목표**: 카카오톡 알림 기능 구축 및 전체 MSA 워크플로우 완성

### 기간
- 시작 예정: 2026-03-23 (월)
- 종료 예정: 2026-04-05 (일)
- **예상 시간: 30시간**
- **진행률: 0%**

### Week 1: Notifier 서비스 기본 구조 (15시간)

#### ❌ Notifier 프로젝트 초기화 (0%)
- [ ] Spring Boot 프로젝트 생성
- [ ] Gradle 빌드 설정
- [ ] Dockerfile 및 docker-compose 구성

**예상 시간**: 2시간

#### ❌ 카카오 API 연동 (0%)
- [ ] 카카오 OAuth2 인증
- [ ] 카카오 메시지 API 연동
- [ ] 메시지 전송 테스트

**예상 시간**: 5시간

#### ❌ REST API 엔드포인트 (0%)
- [ ] POST `/v1/notify` - 알림 발송 요청
- [ ] 요청 DTO 설계 (stock_code, decision_type, message)
- [ ] 응답 DTO 설계

**예상 시간**: 3시간

#### ❌ 알림 템플릿 설계 (0%)
- [ ] BUY 알림 템플릿
- [ ] SELL 알림 템플릿
- [ ] HOLD 알림 템플릿 (선택적)

**예상 시간**: 2시간

#### ❌ 단위 테스트 (0%)
- [ ] Mockito 기반 서비스 로직 테스트
- [ ] 카카오 API Mock 테스트

**예상 시간**: 3시간

**Week 1 총합**: 15시간

### Week 2: Collector ↔ Notifier 통신 (15시간)

#### ❌ NotifierClient 구현 (Collector, 0%)
- [ ] NotifierClient Interface 정의
- [ ] NotifierRestClient 구현
  - POST `/v1/notify` - 알림 발송 요청
- [ ] NotifierProperties 설정 (base-url, timeout)
- [ ] RateLimiter 설정 (5/s)
- [ ] 에러 핸들링 및 재시도 로직

**예상 시간**: 6시간

#### ❌ InvestmentService 구현 (0%)
- [ ] 이전 상태 조회 로직
- [ ] 현재 상태와 비교 로직
- [ ] 상태 변화 감지 알고리즘
- [ ] 알림 조건 판단 (HOLD → BUY/SELL)

**예상 시간**: 4시간

#### ❌ WorkflowOrchestrator 확장 (0%)
- [ ] 알림 단계 추가
  1. 상태 변화 감지
  2. Notifier 알림 요청
- [ ] 에러 핸들링 (알림 실패 시 로그)

**예상 시간**: 2시간

#### ❌ 통합 테스트 (0%)
- [ ] WireMock 기반 Notifier Mock
- [ ] End-to-End 시나리오 테스트
- [ ] 전체 워크플로우 테스트 (데이터 수집 → AI 판단 → 알림)

**예상 시간**: 2시간

#### ❌ 문서화 및 배포 (0%)
- [ ] Notifier README.md
- [ ] Notifier TECHSPEC.md
- [ ] docker-compose 통합
- [ ] ADR-0013 작성 (Notifier 통신 스펙)

**예상 시간**: 1시간

**Week 2 총합**: 15시간

### Phase 5 총 예상 시간
- **총합: 30시간**

**마일스톤**: **🎉 MVP 완성 - 데이터 수집 → AI 판단 → 알림 발송 전체 워크플로우 작동**

---

## Phase 6: 주문 실행 ❌ 계획

**목표**: 자동 주문 실행 기능 (안전장치 포함)

### 기간
- 시작 예정: 2026-04-06 (월)
- 종료 예정: 2026-04-19 (일)
- **예상 시간: 28시간**
- **진행률: 0%**

### Week 1: 주문 API 및 안전장치 (16시간)

#### ❌ 주문 실행 기능 (0%)
- [ ] Notifier 주문 요청 수신 API
  - POST `/collector/v1/order` (주문 실행 요청)
  - Request: `stock_code`, `order_type` (BUY/SELL), `quantity`, `price`
- [ ] KIS API 주문 실행
  - POST `/uapi/domestic-stock/v1/trading/order-cash` (국내 주식 매수/매도)
  - POST `/uapi/overseas-stock/v1/trading/order` (해외 주식 매수/매도)
- [ ] OrderExecution Entity (주문 이력 저장)
  - `order_id`, `stock_code`, `order_type`, `quantity`, `price`
  - `execution_status`, `execution_time`, `error_message`
- [ ] OrderService 구현
  - 주문 검증 (잔고 확인, 수량 검증)
  - 주문 실행 및 결과 저장

**예상 시간**: 10시간

#### ❌ 안전장치 구현 (0%)
- [ ] 일일 주문 한도 설정
  - Redis 기반 카운터
  - 초과 시 주문 거부
- [ ] 손절매 룰
  - 손실 비율 임계값 설정
  - 임계값 초과 시 자동 매도
- [ ] 테스트 모드 (시뮬레이션)
  - 실제 주문 없이 시뮬레이션
  - 로그로만 기록

**예상 시간**: 4시간

#### ❌ 단위 테스트 (0%)
- [ ] Mockito 기반 서비스 로직 테스트
- [ ] KIS API Mock 테스트

**예상 시간**: 2시간

**Week 1 총합**: 16시간

### Week 2: Notifier 주문 요청 연동 (12시간)

#### ❌ Notifier → Collector 주문 요청 API (0%)
- [ ] Notifier에서 Collector 주문 요청
- [ ] 주문 결과 응답 처리

**예상 시간**: 4시간

#### ❌ 주문 결과 알림 기능 (0%)
- [ ] 주문 성공 알림
- [ ] 주문 실패 알림
- [ ] 손절매 실행 알림

**예상 시간**: 3시간

#### ❌ 통합 테스트 (0%)
- [ ] 소액 실제 주문 테스트
- [ ] 주문 실패 시나리오 테스트

**예상 시간**: 3시간

#### ❌ 문서화 (0%)
- [ ] 주문 매뉴얼 작성
- [ ] 안전 가이드 작성
- [ ] ADR-0014 작성 (주문 실행 전략)

**예상 시간**: 2시간

**Week 2 총합**: 12시간

### Phase 6 총 예상 시간
- **총합: 28시간**

---

## Phase 7: AI 고도화 및 안정성 ❌ 계획

**목표**: AI 정확도 향상 및 시스템 안정성 강화

### 기간
- 시작 예정: 2026-04-20 (월)
- 종료 예정: 2026-05-17 (일)
- **예상 시간: 50시간**
- **진행률: 0%**

### Week 1: AI 모델 고도화 (16시간)

#### ❌ AI 모델 고도화 (0%)
- [ ] LSTM/Transformer 모델 도입
- [ ] 백테스팅 프레임워크 구축
- [ ] 하이퍼파라미터 튜닝
- [ ] 모델 버전 관리

**예상 시간**: 16시간

### Week 2: Circuit Breaker & Retry (12시간)

#### ❌ Circuit Breaker 구현 (0%)
- [ ] Resilience4j 통합
- [ ] AI Advisor Circuit Breaker 설정
  - Failure Rate Threshold: 50%
  - Wait Duration: 60s
- [ ] Notifier Circuit Breaker 설정
- [ ] Fallback 로직 (Circuit Open 시 알림)

**예상 시간**: 6시간

#### ❌ 알림 재시도 큐 (0%)
- [ ] Redis 기반 실패 알림 큐
- [ ] 재시도 스케줄러 (5분 간격)
- [ ] 최대 재시도 횟수 설정 (3회)

**예상 시간**: 6시간

### Week 3: Distributed Tracing (10시간)

#### ❌ Distributed Tracing (0%)
- [ ] Spring Cloud Sleuth 통합
- [ ] Trace-ID 전파 (Collector → AI Advisor/Notifier)
- [ ] 로그 포맷 개선 (JSON, 구조화)

**예상 시간**: 10시간

### Week 4: Health Check & 모니터링 (12시간)

#### ❌ Health Check 개선 (0%)
- [ ] `/health` 엔드포인트 확장
  - KIS API 연결 상태
  - Redis 연결 상태
  - MySQL 연결 상태
  - AI Advisor/Notifier 연결 상태
- [ ] Actuator 설정

**예상 시간**: 4시간

#### ❌ 백업 및 복구 프로세스 개선 (0%)
- [ ] 자동 백업 스크립트
- [ ] 백업 검증 프로세스
- [ ] 복구 테스트

**예상 시간**: 4시간

#### ❌ 성능 테스트 및 최적화 (0%)
- [ ] JMeter 부하 테스트
- [ ] 병목 지점 분석
- [ ] 쿼리 최적화
- [ ] 인덱스 추가

**예상 시간**: 4시간

### Phase 7 총 예상 시간
- **총합: 50시간**

---

## Phase 8: 비동기 통신 전환 ❌ 미래

**목표**: REST → Kafka/RabbitMQ 전환, 이벤트 기반 아키텍처

### 기간
- 시작 예정: 2026-05-18 (월) 이후
- **예상 시간: TBD**
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
| Phase 1: 데이터 수집 인프라 | 2026-01-12 ~ 01-25 (2주) | 100% | ✅ 완료 |
| Phase 2: 배포 및 운영 인프라 | 2026-01-28 ~ 02-22 (3.5주) | 4% | 🚧 진행 중 |
| Phase 3: 실시간 데이터 수집 | 2026-02-23 ~ 03-01 (1주) | 0% | ⏳ 대기 |
| Phase 4: AI Advisor 개발 및 연동 | 2026-03-02 ~ 03-22 (3주) | 0% | ⏳ 대기 |
| Phase 5: Notifier 개발 및 연동 **(MVP)** | 2026-03-23 ~ 04-05 (2주) | 0% | ⏳ 대기 |
| Phase 6: 주문 실행 | 2026-04-06 ~ 04-19 (2주) | 0% | ⏳ 대기 |
| Phase 7: AI 고도화 및 안정성 | 2026-04-20 ~ 05-17 (4주) | 0% | ⏳ 대기 |
| Phase 8: 비동기 통신 전환 | 2026-05-18 이후 | 0% | ⏳ 미래 |

---

## Key Milestones

| 날짜 | 요일 | 마일스톤 | 상태 |
|------|------|----------|------|
| 2026-01-25 | 일 | Phase 1 완료 (데이터 수집 인프라) | ✅ |
| 2026-02-01 | 일 | 문서화 완료 | ✅ |
| 2026-02-22 | 일 | Phase 2 완료 (배포 자동화) | 🔜 |
| 2026-03-01 | 일 | Phase 3 완료 (실시간 데이터 수집) | 🔜 |
| 2026-03-22 | 일 | Phase 4 완료 (AI Advisor 연동) | 🔜 |
| **2026-04-05** | **일** | **Phase 5 완료 (MVP - 전체 워크플로우 작동)** | **🔜** |
| 2026-04-19 | 일 | Phase 6 완료 (주문 실행) | 🔜 |
| 2026-05-17 | 일 | Phase 7 완료 (AI 고도화 & 안정성) | 🔜 |

---

## Notes

### 1인 개발 최적화
- Phase 2는 3주 목표였으나 일요일 종료를 위해 3.5주(25일)로 조정
- Phase 단위로 집중하여 한 번에 하나의 작업만 수행
- 점진적 통합 전략: AI Advisor 연동 → Notifier 연동 (순차적)

### MVP 정의
**Phase 5 완료 시점 = MVP**
- 사용자가 관심종목 등록
- 시스템이 자동으로 데이터 수집 (일간 + 실시간)
- AI가 투자 판단 (매수/매도/보류)
- **카카오톡으로 알림 발송** ← 핵심 가치 전달 완료

Phase 6 이후는 추가 자동화(주문 실행) 및 개선(AI 고도화)입니다.

### 총 예상 시간 (Phase 1-7)
- Phase 1: 약 14일 (완료)
- Phase 2: 55.5시간
- Phase 3: 15시간
- Phase 4: 45시간
- Phase 5: 30시간
- Phase 6: 28시간
- Phase 7: 50시간
- **총합: 223.5시간** (약 140일, 주당 15-20시간 기준)

---

## Related Documentation

- **MSA 전체 일정**: [/CAA/MILESTONE.md](../../MILESTONE.md)
- **MSA 아키텍처**: [/CAA/BLUEPRINT.md](../../BLUEPRINT.md)
- **Collector 기술 명세**: [TECHSPEC.md](./TECHSPEC.md)
- **Collector 작업 목록**: [TODO.md](./TODO.md)
- **Collector ADR**: [adr/](./adr/)
