# CAA Collector Service - Technical Specification

> **개발자를 위한 상세 기술 명세**

**작성자**: jongtix + Claude (backend-developer)
**Last Updated**: 2026-02-14

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
│   ├── constant/
│   │   └── DateFormatConstants.java  # 날짜 포맷 상수 (KST_ZONE_ID 등)
│   ├── entity/
│   │   └── BaseEntity.java           # 생성/수정 시간 자동 관리
│   ├── converter/
│   │   ├── MarketCodeConverter.java  # JPA Converter
│   │   └── AssetTypeConverter.java   # JPA Converter
│   ├── enums/
│   │   ├── MarketCode.java           # 시장 코드 Enum
│   │   └── AssetType.java            # 자산 유형 Enum
│   └── util/
│       ├── TokenEncryptor.java       # Redis 토큰 AES-256 암호화
│       ├── RedisKeyHasher.java       # 계정번호 SHA-256 해싱
│       └── LogMaskingUtil.java       # 민감 정보 로그 마스킹
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
    ├── mapper/
    │   └── WatchlistMapper.java      # API DTO → Entity 변환
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
    FOREIGN KEY (group_id) REFERENCES watchlist_group(id) ON DELETE CASCADE,
    INDEX idx_watchlist_stock_backfill_completed (backfill_completed, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Index 전략**:
- `idx_watchlist_stock_backfill_completed (backfill_completed, id)`: 백필 대상 종목 조회 최적화
  - `StockBackfillService`에서 `backfill_completed = FALSE` 조건으로 조회 시 성능 향상
  - Covering Index 효과 (id도 포함하여 테이블 접근 최소화)

---

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

## WebSocket Architecture (Phase 2 Week 2)

### Overview

실시간 시세 조회는 REST API 폴링 방식 대신 **WebSocket 기반 구독 방식**을 채택합니다.

**채택 이유**:
- **즉시성**: 가격 변화 시 즉시 푸시 수신 (ms 단위)
- **효율성**: Rate Limiter 우회 (한 번 연결 → 지속 수신)
- **정확성**: 1분 간격이 아닌 실시간 틱 데이터
- **네트워크 효율**: 불필요한 폴링 제거

### Connection Management

#### WebSocket Endpoints

```yaml
# 실전투자
ws://ops.koreainvestment.com:21000

# 모의투자
ws://ops.koreainvestment.com:31000
```

#### Authentication Flow

1. **승인키 발급** (REST API)
   ```http
   POST https://openapi.koreainvestment.com:9443/oauth2/Approval
   Content-Type: application/json

   {
     "grant_type": "client_credentials",
     "appkey": "{APP_KEY}",
     "secretkey": "{APP_SECRET}"
   }
   ```

   **Response**:
   ```json
   {
     "approval_key": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "expires_in": 86400
   }
   ```

2. **WebSocket 연결** (승인키 포함)
   ```java
   WebSocketStompClient client = new WebSocketStompClient(
       new StandardWebSocketClient()
   );
   client.connect(WS_URL, new MyWebSocketHandler());
   ```

#### Reconnection Strategy

- **지수 백오프**: 1s, 2s, 4s, 8s, 16s
- **최대 재시도**: 5회
- **재연결 시**: 기존 구독 목록 자동 복원
- **Heartbeat**: 30초 간격 Ping/Pong

```java
public class KisWebSocketManager {
    private static final int[] BACKOFF_DELAYS = {1000, 2000, 4000, 8000, 16000};
    private static final int MAX_RECONNECT_ATTEMPTS = 5;

    public void reconnect() {
        for (int attempt = 0; attempt < MAX_RECONNECT_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(BACKOFF_DELAYS[attempt]);
                connect();
                restoreSubscriptions(); // 재구독
                return;
            } catch (Exception e) {
                log.warn("Reconnect attempt {} failed", attempt + 1);
            }
        }
        notifyReconnectFailed();
    }
}
```

### Subscription Model

#### TR Codes (거래유형 코드)

| TR Code | Description | 용도 |
|---------|-------------|------|
| `H0STCNT0` | 실시간 주식 체결가 | 현재가, 체결량, 등락률 |
| `H0STASP0` | 실시간 호가 | 매수/매도 호가, 잔량 |
| `H0STCNI0` | 체결 통보 | 주문 체결 알림 |

#### Subscription Limits

- **H0STCNT0 + H0STASP0**: 최대 20개 종목
- **H0STCNI0**: 최대 1개
- **총합**: 최대 21개 동시 구독

#### Subscribe Message Format

```json
{
  "header": {
    "approval_key": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "custtype": "P",
    "tr_type": "1",
    "content-type": "utf-8"
  },
  "body": {
    "input": {
      "tr_id": "H0STCNT0",
      "tr_key": "005930"
    }
  }
}
```

**Parameters**:
- `custtype`: "P" (개인), "B" (법인)
- `tr_type`: "1" (구독), "2" (구독 해제)
- `tr_id`: 거래유형 코드
- `tr_key`: 종목 코드 (6자리)

#### Unsubscribe Message Format

```json
{
  "header": {
    "approval_key": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "custtype": "P",
    "tr_type": "2"
  },
  "body": {
    "input": {
      "tr_id": "H0STCNT0",
      "tr_key": "005930"
    }
  }
}
```

### Message Format

#### Response Message (H0STCNT0 - 실시간 체결가)

```json
{
  "header": {
    "tr_id": "H0STCNT0",
    "tr_key": "005930"
  },
  "body": {
    "rt_cd": "0",
    "msg_cd": "OPSP0000",
    "msg1": "정상처리되었습니다",
    "output": {
      "MKSC_SHRN_ISCD": "005930",
      "STCK_CNTG_HOUR": "103000",
      "STCK_PRPR": "74800",
      "PRDY_VRSS": "300",
      "PRDY_VRSS_SIGN": "2",
      "PRDY_CTRT": "0.40",
      "ACML_VOL": "12000000",
      "ACML_TR_PBMN": "897600000000"
    }
  }
}
```

**주요 필드**:
- `MKSC_SHRN_ISCD`: 종목 코드
- `STCK_CNTG_HOUR`: 체결 시간 (HHMMSS)
- `STCK_PRPR`: 현재가
- `PRDY_VRSS`: 전일 대비 증감
- `PRDY_CTRT`: 전일 대비율 (%)
- `ACML_VOL`: 누적 거래량
- `ACML_TR_PBMN`: 누적 거래대금

#### Response Parsing

```java
public record RealtimePriceMessage(
    String trId,
    String stockCode,
    String currentPrice,
    String changeRate,
    String volume,
    String timestamp
) {
    public static RealtimePriceMessage from(JsonNode body) {
        JsonNode output = body.get("output");
        return new RealtimePriceMessage(
            body.get("header").get("tr_id").asText(),
            output.get("MKSC_SHRN_ISCD").asText(),
            output.get("STCK_PRPR").asText(),
            output.get("PRDY_CTRT").asText(),
            output.get("ACML_VOL").asText(),
            output.get("STCK_CNTG_HOUR").asText()
        );
    }

    public RealtimeStockPrice toEntity(String marketCode) {
        return RealtimeStockPrice.builder()
            .stockCode(stockCode)
            .marketCode(marketCode)
            .currentPrice(new BigDecimal(currentPrice))
            .changeRate(new BigDecimal(changeRate))
            .volume(Long.parseLong(volume))
            .lastUpdatedAt(parseTimestamp(timestamp))
            .build();
    }
}
```

### Implementation Components

#### KisWebSocketManager

```java
@Component
public class KisWebSocketManager {
    private WebSocketSession session;
    private final ScheduledExecutorService heartbeatScheduler;

    public void connect() {
        String approvalKey = getApprovalKey();
        WebSocketClient client = new StandardWebSocketClient();
        session = client.doHandshake(
            new KisWebSocketHandler(),
            WS_URL
        ).get();
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage("{\"ping\": \"pong\"}"));
        } else {
            reconnect();
        }
    }
}
```

#### KisWebSocketHandler

```java
public class KisWebSocketHandler extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connected: {}", session.getId());
        subscriptionService.syncSubscriptions();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        messageHandler.handleMessage(message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket error", exception);
        webSocketManager.reconnect();
    }
}
```

#### RealtimeSubscriptionService

```java
@Service
public class RealtimeSubscriptionService {
    private final Map<String, SubscriptionInfo> activeSubscriptions = new ConcurrentHashMap<>();

    public void syncSubscriptions() {
        List<WatchlistStock> stocks = watchlistStockRepository.findAll();
        Set<String> targetCodes = stocks.stream()
            .map(WatchlistStock::getStockCode)
            .collect(Collectors.toSet());

        // 구독 추가
        targetCodes.stream()
            .filter(code -> !activeSubscriptions.containsKey(code))
            .forEach(this::subscribe);

        // 구독 해제
        activeSubscriptions.keySet().stream()
            .filter(code -> !targetCodes.contains(code))
            .forEach(this::unsubscribe);
    }

    private void subscribe(String stockCode) {
        String message = buildSubscribeMessage(stockCode, "H0STCNT0");
        webSocketSession.sendMessage(new TextMessage(message));
        activeSubscriptions.put(stockCode, new SubscriptionInfo(stockCode, Instant.now()));
        log.info("Subscribed: {}", stockCode);
    }
}
```

#### RealtimePriceMessageHandler

```java
@Component
public class RealtimePriceMessageHandler {
    private final ExecutorService messageExecutor = Executors.newFixedThreadPool(5);
    private final Semaphore processingLimit = new Semaphore(100);

    public void handleMessage(String message) {
        if (processingLimit.tryAcquire()) {
            messageExecutor.submit(() -> {
                try {
                    RealtimePriceMessage parsed = parseMessage(message);
                    realtimePriceSampler.buffer(parsed);
                } catch (Exception e) {
                    log.error("Message processing failed", e);
                } finally {
                    processingLimit.release();
                }
            });
        } else {
            log.warn("Back-pressure triggered, dropping message");
            metricsService.incrementDroppedMessages();
        }
    }
}
```

#### RealtimePriceSampler (5초 샘플링)

```java
@Component
public class RealtimePriceSampler {
    private final Map<String, RealtimeStockPrice> buffer = new ConcurrentHashMap<>();

    public void buffer(RealtimePriceMessage message) {
        RealtimeStockPrice entity = message.toEntity("KOSPI");
        buffer.put(message.stockCode(), entity);
    }

    @Scheduled(fixedRate = 5000)
    public void flushBuffer() {
        if (!buffer.isEmpty()) {
            List<RealtimeStockPrice> batch = new ArrayList<>(buffer.values());
            repository.saveAll(batch);
            buffer.clear();
            log.info("Flushed {} realtime prices", batch.size());
        }
    }
}
```

### Database Schema (RealtimeStockPrice)

```sql
CREATE TABLE realtime_stock_price (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    market_code VARCHAR(10) NOT NULL,
    current_price DECIMAL(15, 2) NOT NULL,
    change_rate DECIMAL(10, 4) NOT NULL,
    volume BIGINT NOT NULL,
    last_updated_at DATETIME(6) NOT NULL,
    bid_price DECIMAL(15, 2),
    ask_price DECIMAL(15, 2),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_realtime_stock_price UNIQUE (stock_code, market_code),
    INDEX idx_realtime_stock_price_updated (last_updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Trading Hours Management

```java
@Component
public class TradingHoursScheduler {

    @Scheduled(cron = "0 0 9 * * MON-FRI")
    public void onMarketOpen() {
        webSocketManager.connect();
        subscriptionService.syncSubscriptions();
        log.info("Market opened, WebSocket connected");
    }

    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void onMarketClose() {
        subscriptionService.unsubscribeAll();
        webSocketManager.disconnect();
        log.info("Market closed, WebSocket disconnected");
    }
}
```

### Error Handling

| Error Type | Strategy | Retry |
|-----------|----------|-------|
| **Connection Failure** | 재연결 (지수 백오프) | 5회 |
| **Connection Lost** | 즉시 재연결 + 재구독 | 5회 |
| **Parse Error** | 로그 + 메시지 버리기 | 없음 |
| **Heartbeat Timeout** | Ping 전송 → 응답 없으면 재연결 | 5회 |
| **Subscription Limit** | 구독 목록 정리 후 재시도 | 1회 |

### Configuration

```yaml
kis:
  base-url: https://openapi.koreainvestment.com:9443
  user-id: ${KIS_ID}
  accounts:
    - name: 연금저축
      account-number: ${KIS_ACCOUNT_PENSION_NUMBER}
      app-key: ${KIS_ACCOUNT_PENSION_APP_KEY}
      app-secret: ${KIS_ACCOUNT_PENSION_APP_SECRET}
  websocket:
    approval-url: /oauth2/Approval
    ws-url: ws://ops.koreainvestment.com:21000
    heartbeat-interval: 30000
    reconnect-max-attempts: 5
    reconnect-delay-ms: 5000
    message-executor:
      core-pool-size: 5
      max-pool-size: 10
      queue-capacity: 200
```

### Health Check

```java
@Component
public class KisWebSocketHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        if (webSocketManager.isConnected()) {
            return Health.up()
                .withDetail("state", webSocketManager.getConnectionState())
                .withDetail("subscriptions", subscriptionService.getActiveCount())
                .withDetail("lastHeartbeat", webSocketManager.getLastHeartbeatTime())
                .build();
        }
        return Health.down()
            .withDetail("state", webSocketManager.getConnectionState())
            .withDetail("lastError", webSocketManager.getLastError())
            .build();
    }
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

## Security Architecture

### 보안 구성 요소 (Phase 2 완료)

#### 1. Token Encryption (TokenEncryptor)

**목적**: Redis에 저장되는 KIS API 액세스 토큰을 암호화하여 보호

**알고리즘**: AES-256-GCM (Galois/Counter Mode)

**구현**:
```java
@Component
public class TokenEncryptor {
    private final SecretKey secretKey;

    public String encrypt(String plainToken) {
        // AES-256-GCM 암호화
        // IV(Initialization Vector) 자동 생성
        // Base64 인코딩하여 반환
    }

    public String decrypt(String encryptedToken) {
        // Base64 디코딩 → AES-256-GCM 복호화
    }
}
```

**환경변수**:
- `ENCRYPTION_SECRET_KEY`: 32바이트 Base64 인코딩된 비밀키

**보안 고려사항**:
- 비밀키는 환경변수로 주입 (하드코딩 금지)
- IV는 암호화 시마다 랜덤 생성 (재사용 금지)
- GCM 모드로 무결성 검증 자동 수행

**참조**: [ADR-0012](adr/0012-spring-security-integration.md)

---

#### 2. Redis Key Hashing (RedisKeyHasher)

**목적**: Redis 키에 민감 정보(계정번호) 직접 노출 방지

**알고리즘**: SHA-256

**구현**:
```java
@Component
public class RedisKeyHasher {
    private final String salt;

    public String hash(String accountNumber) {
        // SHA-256(accountNumber + salt)
        // 16진수 문자열로 반환
    }
}
```

**환경변수**:
- `REDIS_KEY_SALT`: 해싱용 솔트 값

**보안 효과**:
- Redis 키 유출 시에도 원본 계정번호 역추적 불가
- 무지개 테이블 공격 방어 (솔트 사용)

**예시**:
```
기존: kis:token:1234567890  (계정번호 노출)
개선: kis:token:a3f5c8d9... (SHA-256 해시)
```

---

#### 3. Log Masking (LogMaskingUtil)

**목적**: 로그에 민감 정보가 평문으로 기록되는 것을 방지

**마스킹 대상**:
- 사용자 ID (이메일)
- 계정번호
- 액세스 토큰
- API 키

**구현**:
```java
public class LogMaskingUtil {
    public static String maskUserId(String userId) {
        // user@example.com → u***@example.com
    }

    public static String maskAccountNumber(String accountNumber) {
        // 1234567890 → 123***7890
    }

    public static String maskToken(String token) {
        // eyJhbGciOi... → eyJ***
    }
}
```

**적용 예시**:
```java
// 변경 전
log.info("Fetching watchlist for user: {}", userId);

// 변경 후
log.info("Fetching watchlist for user: {}", LogMaskingUtil.maskUserId(userId));
```

**보안 효과**:
- 로그 파일 유출 시 민감 정보 보호
- 개발/운영 환경 로그 안전성 향상

---

#### 4. Spring Security 통합

**인증 방식**: HTTP Basic Authentication

**보호 대상**:
- `/actuator/**` 엔드포인트 (health 제외)

**공개 엔드포인트**:
- `/actuator/health`: 헬스 체크 (Docker, Watchtower용)

**설정**:
```yaml
spring:
  security:
    user:
      name: ${ACTUATOR_USERNAME}
      password: ${ACTUATOR_PASSWORD}
      roles: ACTUATOR
```

**CSRF 보호**:
- Stateless REST API로 CSRF 비활성화

**보안 헤더**:
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `Strict-Transport-Security` (HTTPS 적용 시)
- `Content-Security-Policy`

**참조**:
- [ADR-0012: Spring Security Integration](adr/0012-spring-security-integration.md)
- [ADR-0013: TestRestTemplate for Security Test](adr/0013-test-resttemplate-for-security-test.md)

---

#### 5. Redis 토큰 저장 방식 변경

**변경 전** (Phase 1):
```
Redis Key: kis:token:1234567890
Redis Value: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**변경 후** (Phase 2):
```
Redis Key: kis:token:a3f5c8d9e2b7f1c4d6a8e9f3b5c7d2a1... (SHA-256 해시)
Redis Value: AES256_GCM_ENCRYPTED_BASE64_STRING... (AES-256-GCM 암호화)
```

**보안 강화 효과**:
1. **키 보호**: 계정번호 해싱으로 원본 정보 보호
2. **값 보호**: 토큰 암호화로 Redis 메모리 덤프 공격 방어
3. **무결성**: GCM 모드로 변조 탐지

**성능 영향**:
- 암호화/복호화 오버헤드: ~1ms (negligible)
- Redis 조회 패턴 변경 없음 (여전히 O(1))

---

## Configuration

### application.yml 예시

```yaml
spring:
  application:
    name: caa-collector
  profiles:
    group:
      prod: log-prod, db-prod

management:
  endpoints.web.exposure.include: health,info
  endpoint.health.show-details: when-authorized

security:
  actuator:
    username: ${ACTUATOR_USERNAME}
    password: ${ACTUATOR_PASSWORD}
  redis:
    hmac-secret: ${REDIS_KEY_HMAC_SECRET}
  token:
    encryption-key: ${TOKEN_ENCRYPTION_KEY}

kis:
  base-url: https://openapi.koreainvestment.com:9443
  user-id: ${KIS_ID}
  accounts:
    - name: 연금저축
      account-number: ${KIS_ACCOUNT_PENSION_NUMBER}
      app-key: ${KIS_ACCOUNT_PENSION_APP_KEY}
      app-secret: ${KIS_ACCOUNT_PENSION_APP_SECRET}

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

### Docker Compose 환경 변수 (프로덕션)

```yaml
# docker-compose.yml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  # MySQL SSL 활성화 (Phase 2 Week 2 조기 구현)
  - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/caa_db?useSSL=true&requireSSL=true&verifyServerCertificate=false&allowPublicKeyRetrieval=false&serverTimezone=Asia/Seoul
  - SPRING_DATASOURCE_USERNAME=${MYSQL_USER}
  - SPRING_DATASOURCE_PASSWORD=${MYSQL_PASSWORD}
  - SPRING_DATA_REDIS_HOST=redis
  - SPRING_DATA_REDIS_PORT=6379
  - SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}
  - KIS_ID=${KIS_ID}
  - KIS_ACCOUNT_PENSION_*=${KIS_ACCOUNT_PENSION_*}
  - ACTUATOR_USERNAME=${ACTUATOR_USERNAME}
  - ACTUATOR_PASSWORD=${ACTUATOR_PASSWORD}
  - REDIS_KEY_HMAC_SECRET=${REDIS_KEY_HMAC_SECRET}
  - TOKEN_ENCRYPTION_KEY=${TOKEN_ENCRYPTION_KEY}
```

**로컬 개발 환경**: `docker-compose.override.yml`에서 `useSSL=false`로 오버라이드 (개발 편의성)

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

#### Testcontainers 기반 통합 테스트 (Phase 2 추가)

**구성**:
- **MySQL**: `testcontainers:mysql:8.0` - 실제 DB 환경 시뮬레이션
- **Redis**: `testcontainers-redis:2.2.2` - Redis 캐시 동작 검증
- **MockWebServer**: OkHttp 기반 KIS API Mock 서버

**의존성**:
```gradle
testImplementation 'org.testcontainers:testcontainers:1.19.8'
testImplementation 'org.testcontainers:junit-jupiter:1.19.8'
testImplementation 'com.redis:testcontainers-redis:2.2.2'
testImplementation 'com.squareup.okhttp3:mockwebserver:4.12.0'
testImplementation 'org.awaitility:awaitility:4.2.0'
testImplementation 'io.github.hakky54:logcaptor:2.9.0'
```

**장점**:
- **실제 환경 근접**: H2 인메모리 DB 대신 실제 MySQL 사용
- **격리**: 각 테스트가 독립적인 컨테이너에서 실행
- **CI/CD 친화적**: GitHub Actions에서 동일한 환경으로 테스트 가능

**예시 - KIS API Mock**:
```java
@Test
void fetchWatchlist_shouldReturnWatchlistResponse() {
    // given - MockWebServer로 KIS API 응답 모킹
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(mockResponseJson)
        .addHeader("Content-Type", "application/json"));

    // when
    var response = kisWatchlistService.fetchWatchlist(userId);

    // then
    assertThat(response.groups()).hasSize(2);
}
```

**예시 - Testcontainers MySQL**:
```java
@SpringBootTest
@Testcontainers
class WatchlistServiceIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Test
    void syncWatchlist_shouldPersistToRealDatabase() {
        // 실제 MySQL 컨테이너에 저장 및 조회 검증
    }
}
```

**Phase 1 달성**: 5개 통합 테스트 + Phase 2에서 Testcontainers 기반 추가 테스트 작성

---

### 보안 테스트 (Phase 2 추가)

#### Spring Security 통합 테스트

**TestRestTemplate 사용 이유**:
- MockMvc는 서블릿 컨테이너 없이 테스트 (Spring Security 필터 체인 일부만 적용)
- TestRestTemplate은 실제 서버를 띄우고 HTTP 요청 (완전한 보안 설정 검증)

**참조**: [ADR-0013: TestRestTemplate for Security Test](adr/0013-test-resttemplate-for-security-test.md)

**예시**:
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SecurityConfigTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void actuatorHealth_shouldBePublic() {
        // /actuator/health는 인증 없이 접근 가능
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/health",
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void actuatorMetrics_shouldRequireAuth() {
        // /actuator/metrics는 인증 필요
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/metrics",
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void actuatorMetrics_withBasicAuth_shouldSucceed() {
        // HTTP Basic 인증으로 접근
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("actuator", "password")
            .getForEntity("/actuator/metrics", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

**검증 항목**:
- ✅ Public 엔드포인트 (health) 인증 없이 접근 가능
- ✅ Protected 엔드포인트 (metrics, env) 인증 필요
- ✅ HTTP Basic 인증 성공 시 접근 허용
- ✅ 잘못된 자격 증명 시 401 Unauthorized
- ✅ 보안 헤더 검증 (X-Frame-Options, CSP, HSTS)

---

#### 암호화/해싱 단위 테스트

**TokenEncryptor 테스트**:
```java
@Test
void encrypt_decrypt_shouldReturnOriginalValue() {
    String original = "test-access-token";

    String encrypted = tokenEncryptor.encrypt(original);
    String decrypted = tokenEncryptor.decrypt(encrypted);

    assertThat(decrypted).isEqualTo(original);
    assertThat(encrypted).isNotEqualTo(original); // 암호화 확인
}

@Test
void encrypt_shouldGenerateUniqueIV() {
    String plainText = "same-token";

    String encrypted1 = tokenEncryptor.encrypt(plainText);
    String encrypted2 = tokenEncryptor.encrypt(plainText);

    assertThat(encrypted1).isNotEqualTo(encrypted2); // IV 다름
}
```

**RedisKeyHasher 테스트**:
```java
@Test
void hash_shouldReturnConsistentHash() {
    String accountNumber = "1234567890";

    String hash1 = redisKeyHasher.hash(accountNumber);
    String hash2 = redisKeyHasher.hash(accountNumber);

    assertThat(hash1).isEqualTo(hash2);
}

@Test
void hash_shouldBeDifferentWithDifferentSalt() {
    // 솔트가 다르면 해시도 달라야 함
}
```

**LogMaskingUtil 테스트**:
```java
@Test
void maskUserId_shouldHideMiddlePart() {
    String userId = "user@example.com";
    String masked = LogMaskingUtil.maskUserId(userId);

    assertThat(masked).isEqualTo("u***@example.com");
}

@Test
void maskAccountNumber_shouldShowOnlyFirstAndLast() {
    String accountNumber = "1234567890";
    String masked = LogMaskingUtil.maskAccountNumber(accountNumber);

    assertThat(masked).isEqualTo("123***7890");
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
