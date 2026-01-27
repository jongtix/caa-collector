# CAA Collector Service - Technical Specification

> **개발자를 위한 상세 기술 명세**

---

## System Architecture

### High-Level Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    External Services                        │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐   ┌─────────────┐   ┌──────────────┐     │
│  │ KIS Open API │   │ AI Advisor  │   │  Notifier    │     │
│  │ (OAuth2)     │   │ (FastAPI)   │   │ (Spring Boot)│     │
│  └──────┬───────┘   └──────┬──────┘   └──────┬───────┘     │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │ REST             │ REST             │ REST
          ↓                  ↓                  ↓
┌─────────────────────────────────────────────────────────────┐
│              CAA Collector Service (본 프로젝트)              │
├─────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────┐     │
│  │  Scheduler Layer                                   │     │
│  │  ├── WatchlistScheduler (08:00, 18:00, 비활성화)   │     │
│  │  ├── StockPriceScheduler (03:00, 18:30)           │     │
│  │  ├── RealtimePriceScheduler (*/1 9-15, Phase 2)   │     │
│  │  └── WorkflowScheduler (18:35, Phase 2)           │     │
│  └────────────────────────────────────────────────────┘     │
│  ┌────────────────────────────────────────────────────┐     │
│  │  Service Layer                                     │     │
│  │  ├── KisAuthService (토큰 관리)                    │     │
│  │  ├── KisWatchlistService (관심종목 조회)          │     │
│  │  ├── KisStockPriceService (시세 조회)             │     │
│  │  ├── WatchlistService (3-way 동기화)               │     │
│  │  ├── StockPriceCollectionService (수집/백필)      │     │
│  │  ├── StockBackfillService (백필 전용)             │     │
│  │  ├── StockPricePersistenceService (저장)          │     │
│  │  ├── AdvisorClient (학습/예측 요청, Phase 2)      │     │
│  │  ├── NotifierClient (알림 요청, Phase 2)          │     │
│  │  ├── InvestmentService (상태 관리, Phase 2)       │     │
│  │  └── WorkflowOrchestrator (전체 흐름, Phase 2)    │     │
│  └────────────────────────────────────────────────────┘     │
│  ┌────────────────────────────────────────────────────┐     │
│  │  Repository Layer (Spring Data JPA)                │     │
│  │  ├── WatchlistGroupRepository                      │     │
│  │  ├── WatchlistStockRepository                      │     │
│  │  ├── DomesticStockDailyPriceRepository             │     │
│  │  ├── DomesticIndexDailyPriceRepository             │     │
│  │  ├── OverseasStockDailyPriceRepository             │     │
│  │  ├── OverseasIndexDailyPriceRepository             │     │
│  │  └── InvestmentDecisionRepository (Phase 2)        │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
          │                              │
          ↓                              ↓
┌─────────────────┐          ┌─────────────────────┐
│  MySQL Database │          │  Redis Cache        │
│  ├── watchlist_group     │  ├── kis:token:{user} │
│  ├── watchlist_stock     │  └── shedlock:*       │
│  ├── domestic_stock_*    │                       │
│  ├── overseas_stock_*    │                       │
│  └── investment_decision │                       │
└─────────────────┘          └─────────────────────┘
```

### Package Structure

```
com.custom.trader
├── config/
│   ├── RestClientConfig.java         # RestClient Bean 설정
│   ├── ShedLockConfig.java           # 분산 락 설정
│   └── RateLimiterConfig.java        # Rate Limiter 설정
├── common/
│   ├── entity/
│   │   └── BaseEntity.java           # 생성/수정 시간 자동 관리
│   ├── converter/
│   │   ├── MarketCodeConverter.java  # JPA Converter
│   │   └── AssetTypeConverter.java   # JPA Converter
│   └── enums/
│       ├── MarketCode.java           # 시장 코드 Enum
│       └── AssetType.java            # 자산 유형 Enum
├── kis/
│   ├── config/
│   │   ├── KisProperties.java        # KIS API 설정 (record)
│   │   └── KisAccountProperties.java # KIS 계정 설정 (record)
│   ├── dto/
│   │   ├── auth/                     # 토큰 발급 DTO
│   │   ├── watchlist/                # 관심종목 DTO
│   │   └── stockprice/               # 주식 시세 DTO
│   ├── exception/
│   │   └── KisApiException.java      # KIS API 예외
│   └── service/
│       ├── KisAuthService.java       # 토큰 발급/갱신
│       ├── KisWatchlistService.java  # 관심종목 조회
│       └── KisStockPriceService.java # 주식 시세 조회
├── stockprice/
│   ├── domestic/
│   │   ├── entity/
│   │   │   ├── DomesticStockDailyPrice.java  # 국내 주식 일간 가격
│   │   │   └── DomesticIndexDailyPrice.java  # 국내 지수 일간 가격
│   │   └── repository/
│   ├── overseas/
│   │   ├── entity/
│   │   │   ├── OverseasStockDailyPrice.java  # 해외 주식 일간 가격
│   │   │   └── OverseasIndexDailyPrice.java  # 해외 지수 일간 가격
│   │   └── repository/
│   ├── strategy/
│   │   ├── StockPriceStrategy.java           # Strategy 인터페이스
│   │   ├── DomesticStockStrategy.java        # 국내 주식 처리 전략
│   │   ├── DomesticIndexStrategy.java        # 국내 지수 처리 전략
│   │   ├── OverseasStockStrategy.java        # 해외 주식 처리 전략
│   │   ├── OverseasIndexStrategy.java        # 해외 지수 처리 전략
│   │   └── StockPriceStrategyFactory.java    # AssetType별 Strategy 제공
│   ├── service/
│   │   ├── StockPriceCollectorService.java   # 일간 수집, 백필
│   │   └── StockBackfillService.java         # 백필 전용
│   └── scheduler/
│       └── StockPriceScheduler.java          # 03:00 백필, 18:30 일간 수집
└── watchlist/
    ├── entity/
    │   ├── WatchlistGroup.java       # 관심종목 그룹
    │   └── WatchlistStock.java       # 관심종목
    ├── repository/
    │   ├── WatchlistGroupRepository.java
    │   └── WatchlistStockRepository.java
    ├── service/
    │   └── WatchlistService.java     # 동기화 로직
    └── scheduler/
        └── WatchlistScheduler.java   # 08:00, 18:00 (비활성화)
```

---

## Domain Model & Database Schema

### 기존 Entity (Phase 1 완료)

#### WatchlistGroup

```sql
CREATE TABLE watchlist_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(20) NOT NULL,
    group_code VARCHAR(10) NOT NULL,
    group_name VARCHAR(100),
    type VARCHAR(20),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_watchlist_group_user_code UNIQUE (user_id, group_code),
    INDEX idx_watchlist_group_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### WatchlistStock

```sql
CREATE TABLE watchlist_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100),
    market_code VARCHAR(10),         -- MarketCode Enum (KOSPI, KOSDAQ, NASDAQ, etc.)
    asset_type VARCHAR(20),          -- AssetType Enum (STOCK, INDEX, ETF, etc.)
    backfill_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_watchlist_stock_group_code UNIQUE (group_id, stock_code),
    FOREIGN KEY (group_id) REFERENCES watchlist_group(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### DomesticStockDailyPrice (국내 주식)

```sql
CREATE TABLE domestic_stock_daily_price (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    open_price DECIMAL(15, 2) NOT NULL,
    high_price DECIMAL(15, 2) NOT NULL,
    low_price DECIMAL(15, 2) NOT NULL,
    close_price DECIMAL(15, 2) NOT NULL,
    volume BIGINT NOT NULL,
    trading_value DECIMAL(20, 2),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_domestic_stock_daily_price UNIQUE (stock_code, trade_date),
    INDEX idx_domestic_stock_daily_price_code_date (stock_code, trade_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### DomesticIndexDailyPrice (국내 지수)

```sql
CREATE TABLE domestic_index_daily_price (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    index_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    open_price DECIMAL(15, 2) NOT NULL,
    high_price DECIMAL(15, 2) NOT NULL,
    low_price DECIMAL(15, 2) NOT NULL,
    close_price DECIMAL(15, 2) NOT NULL,
    volume BIGINT NOT NULL,
    trading_value DECIMAL(20, 2),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_domestic_index_daily_price UNIQUE (index_code, trade_date),
    INDEX idx_domestic_index_daily_price_code_date (index_code, trade_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### OverseasStockDailyPrice (해외 주식)

```sql
CREATE TABLE overseas_stock_daily_price (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    exchange_code VARCHAR(10) NOT NULL,  -- NASDAQ, NYSE, HKEX, etc.
    trade_date DATE NOT NULL,
    open_price DECIMAL(15, 2) NOT NULL,
    high_price DECIMAL(15, 2) NOT NULL,
    low_price DECIMAL(15, 2) NOT NULL,
    close_price DECIMAL(15, 2) NOT NULL,
    volume BIGINT NOT NULL,
    trading_value DECIMAL(20, 2),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_overseas_stock_daily_price UNIQUE (stock_code, exchange_code, trade_date),
    INDEX idx_overseas_stock_daily_price_code_date (stock_code, trade_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### OverseasIndexDailyPrice (해외 지수)

```sql
CREATE TABLE overseas_index_daily_price (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    index_code VARCHAR(20) NOT NULL,
    exchange_code VARCHAR(10) NOT NULL,  -- NASDAQ, NYSE, etc.
    trade_date DATE NOT NULL,
    open_price DECIMAL(15, 2) NOT NULL,
    high_price DECIMAL(15, 2) NOT NULL,
    low_price DECIMAL(15, 2) NOT NULL,
    close_price DECIMAL(15, 2) NOT NULL,
    volume BIGINT NOT NULL,
    trading_value DECIMAL(20, 2),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_overseas_index_daily_price UNIQUE (index_code, exchange_code, trade_date),
    INDEX idx_overseas_index_daily_price_code_date (index_code, trade_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 신규 Entity (Phase 2 Week 3-4)

#### InvestmentDecision

```sql
CREATE TABLE investment_decision (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(20) NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    decision_type VARCHAR(10) NOT NULL,  -- BUY, SELL, HOLD, UNKNOWN
    confidence_score DECIMAL(5, 4),      -- 0.0000 ~ 1.0000
    predicted_price DECIMAL(15, 2),
    ai_model_version VARCHAR(50),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_investment_decision UNIQUE (user_id, stock_code, trade_date),
    INDEX idx_investment_decision_user_code_date (user_id, stock_code, trade_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### InvestmentState Enum

```java
public enum InvestmentState {
    BUY("매수"),
    SELL("매도"),
    HOLD("보류"),
    UNKNOWN("알 수 없음");

    private final String description;

    InvestmentState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

### ER Diagram

```
┌──────────────────┐
│ watchlist_group  │
├──────────────────┤
│ id (PK)          │
│ user_id          │
│ group_code       │
│ group_name       │
│ type             │
└────────┬─────────┘
         │ 1
         │
         │ N
┌────────┴─────────┐
│ watchlist_stock  │
├──────────────────┤
│ id (PK)          │
│ group_id (FK)    │
│ stock_code       │
│ stock_name       │
│ market_code      │
│ asset_type       │
│ backfill_completed│
└──────────────────┘

┌──────────────────────────┐
│ domestic_stock_daily_price│
├──────────────────────────┤
│ id (PK)                  │
│ stock_code               │
│ trade_date               │
│ OHLCV (open/high/low/close/volume) │
│ trading_value            │
└──────────────────────────┘

┌──────────────────────┐
│ investment_decision  │
├──────────────────────┤
│ id (PK)              │
│ user_id              │
│ stock_code           │
│ trade_date           │
│ decision_type        │
│ confidence_score     │
│ predicted_price      │
│ ai_model_version     │
└──────────────────────┘
```

---

## API Specifications

### KIS API (한국투자증권 Open API)

#### 현재 구현 완료 (Phase 1)

##### 1. 토큰 발급

- **Endpoint**: `POST /oauth2/tokenP`
- **Request**:
  ```json
  {
    "grant_type": "client_credentials",
    "appkey": "{KIS_APP_KEY}",
    "appsecret": "{KIS_APP_SECRET}"
  }
  ```
- **Response**:
  ```json
  {
    "access_token": "eyJ0eXAi...",
    "token_type": "Bearer",
    "expires_in": 86400
  }
  ```

##### 2. 일간 가격 조회 (국내 주식)

- **Endpoint**: `GET /uapi/domestic-stock/v1/quotations/inquire-daily-price`
- **Query Params**:
  - `FID_COND_MRKT_DIV_CODE`: J (주식)
  - `FID_INPUT_ISCD`: 005930 (종목 코드)
  - `FID_PERIOD_DIV_CODE`: D (일간)
  - `FID_ORG_ADJ_PRC`: 0 (수정주가 미적용)
- **Response**:
  ```json
  {
    "output": [
      {
        "stck_bsop_date": "20260124",
        "stck_oprc": "74500",
        "stck_hgpr": "75000",
        "stck_lwpr": "74000",
        "stck_clpr": "74800",
        "acml_vol": "12000000",
        "acml_tr_pbmn": "897600000000"
      }
    ]
  }
  ```

#### 미래 구현 예정

##### 3. 실시간 시세 조회 (Phase 2 Week 2)

- **Endpoint**: `GET /uapi/domestic-stock/v1/quotations/inquire-price`
- **Query Params**:
  - `FID_COND_MRKT_DIV_CODE`: J
  - `FID_INPUT_ISCD`: 005930
- **Response**:
  ```json
  {
    "output": {
      "stck_prpr": "74800",      // 현재가
      "prdy_ctrt": "0.40",       // 전일 대비율
      "acml_vol": "12000000"     // 누적 거래량
    }
  }
  ```

##### 4. 주문 실행 - 국내 주식 매수/매도 (Phase 3)

- **Endpoint**: `POST /uapi/domestic-stock/v1/trading/order-cash`
- **Request**:
  ```json
  {
    "CANO": "12345678",           // 계좌번호
    "ACNT_PRDT_CD": "01",         // 계좌 상품 코드
    "PDNO": "005930",             // 종목 코드
    "ORD_DVSN": "01",             // 주문 구분 (01: 시장가, 00: 지정가)
    "ORD_QTY": "10",              // 주문 수량
    "ORD_UNPR": "0"               // 주문 단가 (시장가는 0)
  }
  ```
- **Response**:
  ```json
  {
    "rt_cd": "0",                 // 성공 코드
    "msg_cd": "MCA00000",
    "msg1": "주문이 완료되었습니다.",
    "output": {
      "KRX_FWDG_ORD_ORGNO": "91252",
      "ODNO": "0000123456",       // 주문 번호
      "ORD_TMD": "153000"         // 주문 시각
    }
  }
  ```

##### 5. 주문 실행 - 해외 주식 매수/매도 (Phase 3)

- **Endpoint**: `POST /uapi/overseas-stock/v1/trading/order`
- **Request**:
  ```json
  {
    "CANO": "12345678",
    "ACNT_PRDT_CD": "01",
    "OVRS_EXCG_CD": "NASD",       // 거래소 (NASDAQ)
    "PDNO": "AAPL",               // 종목 코드
    "ORD_DVSN": "00",             // 지정가
    "ORD_QTY": "10",
    "OVRS_ORD_UNPR": "175.50"     // 주문 단가 (USD)
  }
  ```

### AI Advisor API (미구현, Phase 2 Week 3-4)

#### 1. 학습 요청

- **Endpoint**: `POST /v1/train`
- **Request**:
  ```json
  {
    "user_id": "user123",
    "stock_codes": ["005930", "000660"],
    "start_date": "2026-01-01",
    "end_date": "2026-01-24"
  }
  ```
- **Response**:
  ```json
  {
    "status": "SUCCESS",
    "model_version": "v1.2.3",
    "training_duration_seconds": 1200,
    "accuracy": 0.82,
    "message": "모델 학습이 완료되었습니다."
  }
  ```

#### 2. 예측 요청

- **Endpoint**: `POST /v1/predict`
- **Request**:
  ```json
  {
    "user_id": "user123",
    "stock_code": "005930",
    "current_price": 74500,
    "volume": 12000000
  }
  ```
- **Response**:
  ```json
  {
    "stock_code": "005930",
    "decision_type": "BUY",
    "confidence_score": 0.85,
    "predicted_price": 75000,
    "model_version": "v1.2.3"
  }
  ```

### Notifier API (미구현, Phase 2 Week 3-4)

#### 1. 알림 발송 요청

- **Endpoint**: `POST /v1/notify`
- **Request**:
  ```json
  {
    "user_id": "user123",
    "stock_code": "005930",
    "stock_name": "삼성전자",
    "decision_type": "BUY",
    "confidence_score": 0.85,
    "predicted_price": 75000,
    "message": "삼성전자 매수 시점입니다. 예측 가격: 75,000원 (신뢰도 85%)"
  }
  ```
- **Response**:
  ```json
  {
    "status": "SUCCESS",
    "message_id": "kakao-20260124-001",
    "sent_at": "2026-01-24T19:00:00"
  }
  ```

#### 2. 주문 요청 (Phase 3)

- **Endpoint**: `POST /collector/v1/order` (Collector가 수신)
- **Request**:
  ```json
  {
    "user_id": "user123",
    "stock_code": "005930",
    "order_type": "BUY",
    "quantity": 10,
    "price": 75000
  }
  ```
- **Response**:
  ```json
  {
    "status": "SUCCESS",
    "order_number": "20260124-000123",
    "message": "주문이 접수되었습니다."
  }
  ```

---

## Scheduler Specifications

### WatchlistScheduler (비활성화)

- **cron**: `0 0 8,18 * * ?` (08:00, 18:00)
- **ShedLock**: `@SchedulerLock(name = "syncWatchlist")`
- **동작**:
  1. KIS API 관심종목 조회
  2. **3-way 동기화 로직**:
     - API 기준 삭제 (API에 없으면 DB 삭제)
     - 그룹/종목 생성, 업데이트
     - backfillCompleted 플래그 보존
     - null/중복 stockCode 방어적 처리
  3. 백필 플래그 초기화 (신규 종목)
- **현재 상태**: 주석 처리 (수동 실행)
- **구현 완료**: ✅ Phase 2 Week 1

### StockPriceScheduler (활성화)

#### 백필 작업 (비활성화)

- **cron**: `0 0 3 * * ?` (03:00)
- **ShedLock**: `@SchedulerLock(name = "backfillHistoricalPrices")`
- **동작**:
  1. `backfill_completed = false` 종목 조회
  2. 과거 30일 데이터 수집
  3. 배치 저장 (중복 체크)
  4. `backfill_completed = true` 업데이트
- **현재 상태**: 주석 처리 (1회 실행용)

#### 일간 수집 (활성화)

- **cron**: `0 30 18 * * ?` (18:30)
- **ShedLock**: `@SchedulerLock(name = "collectDailyPrices")`
- **동작**:
  1. 전체 관심종목 조회
  2. 당일 가격 수집 (4가지 타입)
     - **Strategy Pattern 사용**: AssetType별 처리 전략
     - DomesticStockStrategy, DomesticIndexStrategy
     - OverseasStockStrategy, OverseasIndexStrategy
  3. 배치 저장 (중복 체크)
- **현재 상태**: 활성화
- **구현 완료**: ✅ Phase 1

### RealtimePriceScheduler (Phase 2 Week 2)

- **cron**: `*/1 9-15 * * MON-FRI` (장중 1분 간격)
- **ShedLock**: `@SchedulerLock(name = "collectRealtimePrices")`
- **동작**:
  1. 전체 관심종목 조회
  2. 현재가 조회 (배치 처리)
  3. `RealtimeStockPrice` 저장 (upsert)
- **현재 상태**: 미구현

### WorkflowScheduler (Phase 2 Week 3-4)

- **cron**: `0 35 18 * * ?` (18:35)
- **ShedLock**: `@SchedulerLock(name = "executeWorkflow")`
- **동작**:
  1. 가격 수집 완료 확인
  2. AI Advisor 학습 요청
  3. AI Advisor 예측 요청
  4. 상태 변화 감지
  5. Notifier 알림 요청
- **현재 상태**: 미구현

---

## Implementation Details

### WatchlistService 3-Way Sync ✅ 구현 완료

#### 동기화 전략

**API 기준 삭제 전략**: API 응답을 Single Source of Truth로 간주

```java
@Transactional
public void syncWatchlist() {
    // 1. API에서 모든 그룹 조회
    List<GroupItem> apiGroups = kisWatchlistService.getWatchlistGroups();

    // 2. API 그룹 코드 추출
    List<String> apiGroupCodes = apiGroups.stream()
        .map(GroupItem::interGrpCode)
        .toList();

    // 3. API에 없는 그룹 삭제 (Cascade로 종목도 함께 삭제)
    watchlistGroupRepository.deleteByUserIdAndGroupCodeNotIn(userId, apiGroupCodes);

    // 4. 각 그룹 처리
    for (GroupItem apiGroup : apiGroups) {
        WatchlistGroup group = findOrCreateGroup(apiGroup);
        syncStocks(group, apiGroup);
    }
}
```

#### 그룹 동기화 로직

- **신규 그룹**: DB에 없으면 생성
- **기존 그룹**: 그룹명 업데이트 (변경 감지)
- **삭제 그룹**: API에 없으면 DB에서 삭제

#### 종목 동기화 로직

```java
private void syncStocks(WatchlistGroup group, GroupItem apiGroup) {
    // 1. API 종목 조회 (그룹별)
    List<StockItem> apiStocks = kisWatchlistService.getWatchlistStocks(apiGroup.interGrpCode());

    // 2. 방어적 필터링
    List<StockItem> validStocks = apiStocks.stream()
        .filter(stock -> stock.stockCode() != null)  // null 제거
        .collect(Collectors.toSet())                 // 중복 제거
        .stream().toList();

    // 3. API에 없는 종목 삭제
    List<String> apiStockCodes = validStocks.stream()
        .map(StockItem::stockCode)
        .toList();
    group.getStocks().removeIf(stock -> !apiStockCodes.contains(stock.getStockCode()));

    // 4. 각 종목 처리
    for (StockItem apiStock : validStocks) {
        WatchlistStock stock = findOrCreateStock(group, apiStock);
        // backfillCompleted 플래그는 보존 (덮어쓰지 않음)
    }
}
```

#### 백필 플래그 보존

- **신규 종목**: `backfillCompleted = false` (백필 필요)
- **기존 종목**: 기존 플래그 값 유지 (덮어쓰지 않음)

#### 방어적 프로그래밍

- **null stockCode 필터링**: API 응답에 null이 포함될 경우 대비
- **중복 stockCode 제거**: Set을 사용하여 중복 제거
- **빈 리스트 처리**: API 응답이 빈 리스트일 경우 전체 삭제 방지

---

## Error Handling & Retry Strategy

### KIS API

#### 에러 코드 처리

- **401 Unauthorized**: 토큰 갱신 후 재시도
- **429 Too Many Requests**: 1초 대기 후 재시도
- **5xx Server Error**: 다음 배치에서 재시도 (당일은 스킵)

#### 재시도 로직

- **재시도 횟수**: 1회
- **재시도 간격**: 1초
- **Timeout**: 10초

### AI Advisor

#### 에러 처리

- **Timeout (30초)**: `UNKNOWN` 상태 반환
- **5xx Server Error**: 재시도
- **4xx Client Error**: 실패 로그, 재시도 없음

#### 재시도 로직

- **재시도 횟수**: 3회
- **재시도 간격**: 지수 백오프 (1s, 2s, 4s)
- **최종 실패 시**: 이전 상태 유지 + 알림 스킵

### Notifier

#### 에러 처리

- **Timeout (10초)**: 재시도
- **5xx Server Error**: 재시도
- **4xx Client Error**: 실패 로그, 재시도 없음

#### 재시도 로직

- **재시도 횟수**: 3회
- **재시도 간격**: 지수 백오프 (1s, 2s, 4s)
- **최종 실패 시**: 재시도 큐 추가 (Phase 3)

---

## Configuration

### application.yml 예시

```yaml
spring:
  application:
    name: caa-collector

  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        show_sql: false

  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST}
      port: ${SPRING_DATA_REDIS_PORT}

kis:
  base-url: https://openapi.koreainvestment.com:9443
  app-key: ${KIS_APP_KEY}
  app-secret: ${KIS_APP_SECRET}
  account:
    number: ${KIS_ACCOUNT_NUMBER}
    product-code: ${KIS_ACCOUNT_PRODUCT_CODE}

advisor:
  base-url: ${ADVISOR_BASE_URL:http://localhost:8081}
  timeout: 30s

notifier:
  base-url: ${NOTIFIER_BASE_URL:http://localhost:8082}
  timeout: 10s

shedlock:
  default-lock-at-most-for: 10m
  default-lock-at-least-for: 5s
```

### RateLimiter 설정

```java
@Configuration
public class RateLimiterConfig {

    @Bean
    public RateLimiter kisRateLimiter() {
        return RateLimiter.create(20.0);  // 초당 20회
    }

    @Bean
    public RateLimiter advisorRateLimiter() {
        return RateLimiter.create(10.0);  // 초당 10회
    }

    @Bean
    public RateLimiter notifierRateLimiter() {
        return RateLimiter.create(5.0);   // 초당 5회
    }
}
```

---

## Testing Strategy

### 단위 테스트 (Mockito) ✅ 구현 완료

- **Service Layer**: Mockito로 Repository 모킹
- **커버리지 목표**: 80% 이상
- **Phase 1 달성**: 26개 단위 테스트 작성 및 통과
- **예시**:
  ```java
  @Test
  void syncWatchlist_shouldCreateNewGroup() {
      // given
      when(kisWatchlistService.fetchWatchlist(userId))
          .thenReturn(mockWatchlistResponse);
      when(watchlistGroupRepository.findByUserIdAndGroupCode(userId, groupCode))
          .thenReturn(Optional.empty());

      // when
      watchlistService.syncWatchlist();

      // then
      verify(watchlistGroupRepository, times(1)).save(any(WatchlistGroup.class));
  }
  ```

### 통합 테스트 (WireMock, Testcontainers) ✅ 구현 완료

- **KIS API 모킹**: WireMock으로 HTTP 응답 시뮬레이션
- **Database**: Testcontainers MySQL
- **Redis**: Testcontainers Redis
- **Phase 1 달성**: 5개 통합 테스트 작성 및 통과
- **예시**:
  ```java
  @Test
  void fetchWatchlist_shouldReturnWatchlistResponse() {
      // given
      stubFor(get(urlPathEqualTo("/uapi/domestic-stock/v1/trading/inquire-psbl-order"))
          .willReturn(aResponse()
              .withStatus(200)
              .withBody(mockResponseJson)));

      // when
      var response = kisWatchlistService.fetchWatchlist(userId);

      // then
      assertThat(response.groups()).hasSize(2);
  }
  ```

---

## Deployment & Operations

### 빌드 및 실행

```bash
# 빌드
./gradlew build -q

# 실행 (환경변수 .env에서 로드)
./gradlew bootRun -q

# Docker 이미지 빌드
docker build -t caa-collector:latest .

# Docker 컨테이너 실행
docker run -d \
  --name caa-collector \
  -p 8080:8080 \
  --env-file .env \
  caa-collector:latest
```

### 환경 변수 (Kubernetes Secret)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: caa-collector-secret
type: Opaque
stringData:
  KIS_APP_KEY: "your_app_key"
  KIS_APP_SECRET: "your_app_secret"
  KIS_ACCOUNT_NUMBER: "your_account_number"
  SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/caa_collector"
  SPRING_DATASOURCE_USERNAME: "collector_user"
  SPRING_DATASOURCE_PASSWORD: "collector_password"
  SPRING_DATA_REDIS_HOST: "redis"
  ADVISOR_BASE_URL: "http://ai-advisor:8081"
  NOTIFIER_BASE_URL: "http://notifier:8082"
```

### Health Check 엔드포인트

- **URL**: `GET /actuator/health`
- **Response**:
  ```json
  {
    "status": "UP",
    "components": {
      "db": { "status": "UP" },
      "redis": { "status": "UP" },
      "kis": { "status": "UP" },
      "advisor": { "status": "UP" },
      "notifier": { "status": "UP" }
    }
  }
  ```

### Logging

- **포맷**: JSON (구조화된 로그)
- **레벨**: INFO (운영), DEBUG (개발)
- **예시**:
  ```json
  {
    "timestamp": "2026-01-24T18:30:00.123Z",
    "level": "INFO",
    "logger": "com.custom.trader.stockprice.scheduler.StockPriceScheduler",
    "message": "Starting scheduled daily price collection",
    "thread": "scheduling-1",
    "trace_id": "abc123"
  }
  ```

---

## Security

- **KIS 토큰**: Redis 메모리 저장 (디스크 저장 없음)
- **환경 변수**: Kubernetes Secret 관리
- **HTTPS**: KIS API, AI Advisor, Notifier 모두 HTTPS 강제
- **Rate Limiter**: DDoS 방지 (KIS 20/s, AI 10/s, Notifier 5/s)

---

## References

### MSA 전체 문서
- [README.md](../README.md) - MSA 프로젝트 개요
- [BLUEPRINT.md](../BLUEPRINT.md) - MSA 아키텍처 설계
- [MILESTONE.md](../MILESTONE.md) - MSA 전체 일정
- [CLAUDE.md](../CLAUDE.md) - MSA 작업 지침

### Collector 문서
- [CLAUDE.md](../CLAUDE.md) - Collector 코드 구조 및 빌드 명령어

### 코딩 규칙
- `.claude/skills/` - 코딩 규칙 및 스타일 가이드
  - `code-style-guide/SKILL.md`
  - `persistence-strategy/SKILL.md`
  - `error-handling-master/SKILL.md`
  - `test-code-generator/SKILL.md`
