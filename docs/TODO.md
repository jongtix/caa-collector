# CAA Collector Service - TODO

> **현재 Phase 2의 단기 작업 목록 및 우선순위 관리**

---

## Header

- **Last Updated**: 2026-01-27 (화) 21:00
- **Current Focus**: Phase 2 Week 1 완료 (100%)
- **Next Sprint**: 2026-02-02 (월) ~ 2026-02-08 (일)

---

## Priority 0 (P0) - Critical
> **Week 1: 2026-01-26 (월) ~ 02-01 (일) - 11시간**

### 📝 프로젝트 문서화 (3.5시간, 완료)
- [x] `README.md` 업데이트 (30분)
  - ✅ 현재 구현 상태 업데이트 (Phase 2 Week 1 완료 반영)
  - ✅ Directory Structure 업데이트 (strategy 패키지 추가)
  - ✅ Key Features 업데이트 (관심종목 동기화 개선)
- [x] `docs/MILESTONE.md` 업데이트 (40분)
  - ✅ Phase 1 진행률 업데이트 (95% → 100%)
  - ✅ Phase 2 Week 1 진행률 업데이트 (70% → 100%)
  - ✅ 관심종목 편집 반영 완료 체크
- [x] `docs/TODO.md` 업데이트 (20분)
  - ✅ P0/P1/P2 우선순위 분류
  - ✅ Week별 작업 목록
  - ✅ 예상 시간
- [x] `docs/PRD.md` 업데이트 (50분)
  - ✅ Executive Summary
  - ✅ User Stories (5가지 시나리오, Scenario 2 완료 반영)
  - ✅ Functional Requirements (FR-1~4, 주문 실행 포함)
  - ✅ Non-Functional Requirements (NFR-1~4)
  - ✅ Constraints, Success Metrics
- [x] `docs/TECHSPEC.md` 업데이트 (60분)
  - ✅ System Architecture 다이어그램
  - ✅ Database Schema (기존 + InvestmentDecision DDL)
  - ✅ API Specifications (KIS, AI Advisor, Notifier)
  - ✅ Scheduler 명세 (Strategy Pattern 적용)
  - ✅ WatchlistService 3-Way Sync 구현 상세
  - ✅ Error Handling & Retry 전략
  - ✅ Configuration 예시


---

## Priority 1 (P1) - Important
> **Week 2-3: 2026-01-28 (화) ~ 02-22 (일) - 44시간**

### 🐳 배포 자동화 전체 (44시간)

> **범위**: MSA 공통 인프라 (Collector 우선 적용)
> - Docker Compose로 Collector + MySQL + Redis 통합 배포
> - GitHub Actions는 MSA 루트에 워크플로우 생성
> - 향후 서비스 추가 시 `docker-compose.yml` 확장

#### 1️⃣ 컨테이너화 (8시간)
- [ ] Dockerfile 작성 (Multi-stage build)
  - Spring Boot 최적화 (JAR 레이어 분리)
  - 레이어 캐싱 전략 (의존성 → 애플리케이션)
  - JRE 경량화 (eclipse-temurin:21-jre-alpine)
- [ ] Docker Compose 구성
  - MySQL 8.0 컨테이너
  - Redis 7.0 컨테이너
  - Collector 서비스 컨테이너
  - 네트워크 구성 (bridge)
  - 볼륨 마운트 (데이터 영속성)
- [ ] 환경 변수 설정
  - `.env` 파일 구조화
  - KIS API 인증 정보 (APP_KEY, APP_SECRET)
  - DB 연결 정보 (URL, USERNAME, PASSWORD)
  - Redis 연결 정보
- [ ] 로컬 테스트 및 디버깅
  - `docker-compose up` 전체 스택 실행
  - 컨테이너 간 통신 검증
  - 스케줄러 동작 확인

#### 2️⃣ CI/CD 파이프라인 (9시간)
- [ ] GitHub Actions 워크플로우 작성
  - 빌드 단계: `./gradlew build -q`
  - 테스트 단계: `./gradlew test -q`
  - 이미지 빌드 단계: `docker build`
  - 이미지 푸시 단계: `docker push`
- [ ] Docker Registry 설정
  - GitHub Container Registry (ghcr.io) 사용
  - `GITHUB_TOKEN` 인증
  - 이미지 태그 전략 (latest, version)
- [ ] 자동 버전 태깅
  - Semantic Versioning (v1.0.0)
  - Git Tag 연동 (`git tag -a v1.0.0 -m "..."`)
  - 태그 푸시 트리거
- [ ] 빌드 성공/실패 알림
  - Discord/Slack 웹훅 연동
  - 빌드 상태 메시지 전송
- [ ] 통합 테스트 및 디버깅
  - 워크플로우 실행 검증
  - 에러 핸들링 확인

#### 3️⃣ 자동 배포 (10시간)
- [ ] Watchtower 설정 및 테스트
  - Watchtower 컨테이너 추가
  - 이미지 갱신 감지 (Poll interval: 300s)
  - 자동 컨테이너 재시작
- [ ] NAS 환경 배포 스크립트 작성
  - 초기 환경 설정 스크립트
  - 시크릿 관리 (.env 안전 관리)
  - 네트워크 구성 스크립트
- [ ] Health Check 엔드포인트 구현
  - `/actuator/health` 확장
  - `/actuator/readiness` 구현
  - KIS API 연결 상태 체크
  - Redis/MySQL 연결 상태 체크
- [ ] 롤백 전략 설계 및 테스트
  - 이전 이미지 버전 복구 프로세스
  - 배포 실패 시 자동 롤백
  - 롤백 시나리오 테스트

#### 4️⃣ 모니터링 및 관리 (10시간)
- [ ] Portainer 연동 및 대시보드 구성
  - 컨테이너 상태 시각화
  - 로그 뷰어 설정
  - 컨테이너 재시작 관리
- [ ] 로그 수집 전략 구현
  - JSON 포맷 로그 출력 (Logback 설정)
  - 로그 레벨 표준화 (INFO, WARN, ERROR)
  - 컨테이너 로그 수집 (`docker logs`)
- [ ] 리소스 제한 설정
  - NAS 환경 (8GB RAM) 최적화
  - CPU 제한 (cpus: 2.0)
  - 메모리 제한 (mem_limit: 2G)
- [ ] 백업 및 복구 프로세스
  - MySQL 백업 스크립트 (mysqldump)
  - 볼륨 백업 전략 (docker volume)
  - 복구 테스트 (restore 검증)

#### 5️⃣ 문서화 (7시간)
- [ ] DEPLOYMENT.md 작성
  - 배포 가이드 (단계별 설명)
  - 환경 변수 설명 (필수/선택)
  - 트러블슈팅 FAQ
- [ ] ADR-0012-deployment-automation.md
  - 배포 전략 결정 배경
  - Docker vs K8s 비교
  - Watchtower 선택 이유
- [ ] 로컬 개발 환경 가이드
  - Docker Compose로 전체 스택 실행
  - 개발 모드 vs 운영 모드 차이
  - 컨테이너 디버깅 팁

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

### ✅ 프로젝트 문서화 (2026-01-27)
- [x] `README.md` 업데이트 (30분)
  - 현재 구현 상태 업데이트 (Phase 2 Week 1 완료 반영)
  - Directory Structure 업데이트 (strategy 패키지 추가)
  - Key Features 업데이트 (관심종목 동기화 개선)
- [x] `docs/MILESTONE.md` 업데이트 (40분)
  - Phase 1 진행률 업데이트 (95% → 100%)
  - Phase 2 Week 1 진행률 업데이트 (90% → 100%)
  - Progress Tracking 테이블 업데이트
- [x] `docs/TODO.md` 업데이트 (20분)
  - P0/P1/P2 우선순위 분류
  - Week별 작업 목록
  - Completed 섹션 업데이트
- [x] `docs/PRD.md` 업데이트 (50분)
  - Scenario 2 (관심종목 편집) 완료 반영
  - Phase 1 Success Metrics 달성 현황 업데이트
- [x] `docs/TECHSPEC.md` 업데이트 (60분)
  - WatchlistService 3-Way Sync 구현 상세 추가
  - Strategy Pattern 아키텍처 반영
  - Testing Strategy 업데이트 (31개 테스트 통과)
- [x] `CLAUDE.md` 업데이트 (10분)
  - Documentation 섹션 최신화
  - Architecture 섹션 strategy 패키지 반영

### ✅ 관심종목 동기화 개선 (2026-01-26)
- [x] 3-way 동기화 로직 구현 (API 기준 삭제 전략)
  - API에 없으면 DB에서 삭제 (그룹/종목)
  - API에 있으면 DB에 추가/업데이트
  - backfillCompleted 플래그 보존
- [x] 방어적 프로그래밍 적용
  - null stockCode 필터링
  - 중복 stockCode 제거 (Set 사용)
- [x] Repository 메서드 추가
  - `WatchlistGroupRepository.deleteByGroupId()`
  - `WatchlistStockRepository.deleteByStockCode()`
- [x] 테스트 작성 (총 31개)
  - 단위 테스트 26개 (모두 통과)
  - 통합 테스트 5개 (모두 통과)
- [x] 코드 리뷰 승인 완료
- [x] 4개 커밋 완료

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
