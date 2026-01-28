# CAA Collector Service

> **Custom Algorithmic Advisor - Data Collection & Workflow Orchestration Service**

## Project Overview

CAA Collector Service는 **사용자 맞춤형 알고리즘 투자 조언 시스템(CAA)**의 MSA 아키텍처 중 **데이터 수집 및 워크플로우 오케스트레이션**을 담당하는 핵심 서비스입니다.

### 역할 및 책임 범위

**✅ Collector의 책임**:
- 한국투자증권 Open API를 통한 데이터 수집 및 저장 (관심종목, 주식 시세)
- AI Advisor에 학습 요청 및 예측 요청
- Notifier에 알림 발송 요청
- 투자 상태 변화 감지 및 워크플로우 오케스트레이션

**❌ Collector의 책임 밖** (외부 서비스 의존):
- 예측 알고리즘 개발 및 실행 → **AI Advisor 담당**
- 카카오톡 메시지 전송 → **Notifier 담당**

### 현재 구현 상태

- ✅ **Phase 1 완료 (100%)**: KIS API 연동, Watchlist/StockPrice 도메인, 스케줄러, 테스트
- 🚧 **Phase 2 진행 중 (20%)**: 문서화 및 관심종목 편집 반영 완료, 배포 자동화 예정
- ❌ **Phase 3 미구현**: 실시간 시세 (WebSocket), 주문 실행, 모니터링

---

## Architecture Context

### MSA 전체 구조

```
┌──────────────┐
│ KIS Open API │ (한국투자증권)
└──────┬───────┘
       │ REST
       ↓
┌─────────────────────┐
│ Collector Service   │ ← 본 프로젝트
│ (데이터 수집 + 오케스트레이션) │
└──────┬──────┬───────┘
       │      │
       │ REST │ REST
       ↓      ↓
┌─────────┐ ┌─────────┐
│AI Advisor│ │Notifier │
│(예측 엔진)│ │(알림 전송)│
└─────────┘ └────┬────┘
                  │
                  ↓
           ┌──────────────┐
           │ Kakao API    │
           │ (카카오톡 전송) │
           └──────────────┘
```

### 서비스 간 통신

| Sender    | Receiver   | Endpoint       | Method | Description                |
|-----------|------------|----------------|--------|----------------------------|
| Collector | AI Advisor | `/v1/train`    | POST   | 모델 학습 요청             |
| Collector | AI Advisor | `/v1/predict`  | POST   | 실시간 투자 판단 요청       |
| Collector | Notifier   | `/v1/notify`   | POST   | 카카오톡 알림 발송 요청     |
| Notifier  | Collector  | `/collector/v1/order` | POST | 주문 실행 요청 (Phase 3) |

---

## Tech Stack

- **Language & Framework**: Java 21, Spring Boot 3.5.9
- **Build Tool**: Gradle
- **Persistence**: Spring Data JPA
- **Database**: H2 (개발), MySQL (운영)
- **Cache & Lock**: Redis (토큰 캐싱, ShedLock 분산 락)
- **Scheduler**: Spring Scheduling + ShedLock
- **External APIs**:
  - 한국투자증권 Open API (OAuth2)
  - AI Advisor Service (REST)
  - Notifier Service (REST)

---

## Quick Start

### Prerequisites

- Java 21 이상
- Docker (Redis, MySQL 실행용)
- 한국투자증권 Open API 계정 (App Key, App Secret)

### 환경 변수 설정

프로젝트 루트에 `.env` 파일 생성:

```bash
# KIS API 계정
KIS_APP_KEY=your_app_key
KIS_APP_SECRET=your_app_secret
KIS_ACCOUNT_NUMBER=your_account_number
KIS_ACCOUNT_PRODUCT_CODE=01

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# MySQL (운영)
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/caa_collector
SPRING_DATASOURCE_USERNAME=collector_user
SPRING_DATASOURCE_PASSWORD=collector_password
```

### 빌드 및 실행

```bash
# 빌드
./gradlew build -q

# 실행 (환경변수 .env에서 로드)
./gradlew bootRun -q

# 전체 테스트 실행
./gradlew test -q

# 특정 테스트 클래스 실행
./gradlew test -q --tests "com.custom.trader.watchlist.service.WatchlistServiceTest"

# 특정 테스트 메서드 실행
./gradlew test -q --tests "com.custom.trader.watchlist.service.WatchlistServiceTest.syncWatchlist"

# 클린 빌드
./gradlew clean build -q
```

---

## Directory Structure

```
com.custom.trader
├── config/                      # 전역 설정
│   ├── RestClientConfig.java    # RestClient Bean 설정
│   ├── ShedLockConfig.java      # 분산 락 설정
│   └── RateLimiterConfig.java   # Rate Limiter 설정
├── common/
│   ├── entity/                  # BaseEntity (생성/수정 시간 자동 관리)
│   ├── converter/               # JPA Converter (MarketCode, AssetType)
│   └── enums/                   # Enum 클래스 (MarketCode, AssetType)
├── kis/                         # 한국투자증권 API 연동
│   ├── config/                  # KisProperties, KisAccountProperties (record)
│   ├── dto/                     # API 요청/응답 record
│   │   ├── auth/                # 토큰 발급 DTO
│   │   ├── watchlist/           # 관심종목 DTO
│   │   └── stockprice/          # 주식 시세 DTO
│   ├── exception/               # KisApiException
│   └── service/                 # KIS API 호출 서비스
│       ├── KisAuthService       # 토큰 발급/갱신
│       ├── KisWatchlistService  # 관심종목 조회
│       └── KisStockPriceService # 주식 시세 조회
├── stockprice/                  # 주식 가격 수집 도메인
│   ├── domestic/                # 국내 주식/지수 일간 가격
│   │   ├── entity/              # DomesticStockDailyPrice, DomesticIndexDailyPrice
│   │   └── repository/          # Spring Data JPA Repository
│   ├── overseas/                # 해외 주식/지수 일간 가격
│   │   ├── entity/              # OverseasStockDailyPrice, OverseasIndexDailyPrice
│   │   └── repository/          # Spring Data JPA Repository
│   ├── strategy/                # AssetType별 처리 전략 (Strategy Pattern)
│   │   ├── StockPriceStrategy            # Strategy 인터페이스
│   │   ├── DomesticStockStrategy         # 국내 주식 처리 전략
│   │   ├── DomesticIndexStrategy         # 국내 지수 처리 전략
│   │   ├── OverseasStockStrategy         # 해외 주식 처리 전략
│   │   ├── OverseasIndexStrategy         # 해외 지수 처리 전략
│   │   └── StockPriceStrategyFactory     # AssetType별 Strategy 제공
│   ├── service/                 # 가격 수집 서비스
│   │   ├── StockPriceCollectionService   # 일간 수집, 백필 로직
│   │   ├── StockBackfillService          # 백필 전용 서비스
│   │   └── StockPricePersistenceService  # 저장 로직
│   └── scheduler/               # StockPriceScheduler (03:00 백필, 18:30 일간 수집)
└── watchlist/                   # 관심종목 도메인
    ├── entity/                  # WatchlistGroup, WatchlistStock (JPA)
    ├── repository/              # Spring Data JPA Repository
    ├── service/                 # WatchlistService (동기화 로직)
    └── scheduler/               # WatchlistScheduler (08:00, 18:00 동기화, 현재 비활성화)
```

---

## Key Features

### ✅ 구현 완료 (Phase 1: 100% + Phase 2 Week 1: 100%)

1. **KIS API 연동**
   - OAuth2 토큰 발급 및 자동 갱신 (Redis 캐싱)
   - 계정별 토큰 락 (분산 환경 동시성 제어)
   - Rate Limiter (초당 20회 제한)

2. **관심종목 동기화**
   - KIS API → MySQL 3-way 동기화
   - 그룹/종목 자동 생성, 업데이트, 삭제
   - API 기준 삭제 전략 (API에 없으면 DB 삭제)
   - 백필 상태 플래그 보존
   - 방어적 프로그래밍 (null/중복 stockCode 처리)

3. **주식 가격 수집**
   - 4가지 타입 지원: 국내/해외 주식/지수
   - Strategy Pattern 적용 (AssetType별 처리 전략)
   - 일간 수집 (18:30)
   - 과거 데이터 백필 (03:00, 30일)

4. **스케줄러**
   - ShedLock 분산 락 (중복 실행 방지)
   - WatchlistScheduler (비활성화)
   - StockPriceScheduler (활성화)

5. **테스트**
   - 단위 테스트 (Mockito) - 26개
   - 통합 테스트 (WireMock) - 5개
   - 총 31개 테스트 통과
   - 커버리지 80% 이상

### ❌ 미구현 (Phase 2-3)

1. **배포 자동화** (Phase 2 Week 2-3)
   - Docker 컨테이너화
   - GitHub Actions CI/CD 파이프라인
   - Watchtower 자동 배포

2. **실시간 시세 조회** (Phase 3)
   - KIS WebSocket API 연동 (승인키 발급, 구독 관리)
   - 비동기 메시지 처리 및 5초 샘플링
   - RealtimePrice Entity/Repository 설계

3. **AI Advisor 통신** (Phase 4)
   - REST Client 구현
   - 학습/예측 요청 API 호출

4. **Notifier 통신** (Phase 4)
   - REST Client 구현
   - 알림 발송 요청 API 호출

5. **투자 상태 관리** (Phase 4)
   - InvestmentDecision 엔티티 설계
   - 상태 변화 감지 로직
   - 워크플로우 오케스트레이션

6. **주문 실행** (Phase 3)
   - Notifier 주문 요청 수신
   - KIS API 주문 실행 (매수/매도)

7. **안정성 고도화** (Phase 3)
   - Circuit Breaker
   - Distributed Tracing
   - Health Check 개선

---

## Related Documentation

### Collector 문서
- [MILESTONE.md](./docs/MILESTONE.md) - Collector 일정 및 Phase별 진행 상황
- [TODO.md](./docs/TODO.md) - Collector 단기 작업 목록 및 우선순위
- [PRD.md](./docs/PRD.md) - Collector 제품 요구사항 정의
- [TECHSPEC.md](./docs/TECHSPEC.md) - Collector 기술 명세

### MSA 전체 문서
- [README.md](../README.md) - MSA 프로젝트 첫 진입점
- [BLUEPRINT.md](../BLUEPRINT.md) - MSA 아키텍처 설계
- [MILESTONE.md](../MILESTONE.md) - MSA 전체 일정 및 서비스 간 의존성
- [CLAUDE.md](../CLAUDE.md) - MSA 전체 작업 지침

---

## Contact & Support

- 프로젝트 이슈: GitHub Issues
- 개발자: jongtix
- 참조: `.claude/skills/` 디렉토리의 코딩 규칙 및 스타일 가이드
