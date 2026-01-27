# CAA Collector Service - Product Requirements Document (PRD)

> **비즈니스 관점의 제품 요구사항 정의**

---

## Executive Summary

### 문제 정의

개인 투자자는 주식 시장에서 **수동 모니터링 부담**과 **감정적 판단 오류**로 인해 최적의 투자 시점을 놓치는 문제를 겪고 있습니다.

- 매일 수십 개 종목의 가격을 확인하고 매수/매도 시점을 판단해야 함
- 업무 중에는 시세 확인이 어려워 기회 손실 발생
- 감정에 의한 충동 매매로 손실 확대

### 해결책

**AI 자동 알림 시스템**을 통해 사용자는 관심종목만 등록하면, 시스템이 자동으로 데이터를 수집하고 AI가 학습/예측하여 투자 시점을 카카오톡으로 실시간 알림합니다.

### 제품 비전

**"관심종목만 등록하면 AI가 매수/매도 시점을 카카오톡으로 알림"**

---

## Product Overview

### Collector의 역할

CAA Collector Service는 **MSA 아키텍처의 컨트롤 타워**로서 다음 역할을 수행합니다:

1. **데이터 수집**: 한국투자증권 API를 통해 관심종목 및 주식 시세 수집
2. **워크플로우 오케스트레이션**: AI Advisor/Notifier와 통신하여 전체 프로세스 제어
3. **투자 상태 관리**: 이전 상태와 현재 상태 비교하여 변화 감지

### 타겟 사용자

- **개인 투자자**: 한국투자증권 계정 보유, 관심종목 10~100개 관리
- **사용 시나리오**: 업무 중 시세 확인 불가, AI 기반 자동 알림 수신 원함

### 서비스 경계

**Collector가 하는 것**:
- ✅ 데이터 수집 (KIS API → MySQL)
- ✅ AI Advisor에 학습/예측 요청 (REST)
- ✅ Notifier에 알림 발송 요청 (REST)
- ✅ 투자 상태 변화 감지
- ✅ 주문 실행 (Notifier 요청 수신 → KIS API 주문)

**Collector가 하지 않는 것**:
- ❌ 예측 알고리즘 개발 및 실행 → **AI Advisor 담당**
- ❌ 카카오톡 메시지 전송 → **Notifier 담당**

---

## User Stories & Scenarios

### 사용자 여정 (User Journey)

```
1. 사용자 관심종목 등록 (한국투자증권 앱)
   ↓
2. Collector가 자동 동기화 (08:00, 18:00)
   ↓
3. 데이터 수집 (일간 18:30, 백필 03:00, 실시간 장중 1분)
   ↓
4. AI Advisor 학습 (18:35)
   ↓
5. AI Advisor 예측 (18:40)
   ↓
6. 상태 변화 감지 (HOLD → BUY/SELL)
   ↓
7. Notifier 알림 발송 (카카오톡)
   ↓
8. 사용자 확인 및 행동 (매수/매도)
```

### Scenario 1: 신규 종목 추가 (백필 과정)

**사용자 액션**:
- 한국투자증권 앱에서 "삼성전자" 관심종목 추가

**시스템 동작**:
1. **18:00**: WatchlistScheduler 실행 → KIS API 동기화
2. **신규 종목 감지**: `WatchlistStock` 생성, `backfill_completed = false`
3. **03:00 (다음날)**: StockPriceScheduler 백필 실행
   - 과거 30일 데이터 수집 (`DomesticStockDailyPrice` 30개 행 저장)
   - `backfill_completed = true` 업데이트
4. **18:35**: AI Advisor 학습 요청 (삼성전자 포함)
5. **18:40**: AI Advisor 예측 요청 → `InvestmentDecision` 저장 (HOLD)

**예상 결과**:
- 사용자는 다음날부터 삼성전자 투자 알림 수신 가능

### Scenario 2: 관심종목 편집 (3-way 동기화) ✅ 구현 완료

**사용자 액션**:
- 한국투자증권 앱에서 그룹명 "테크주" → "반도체" 변경
- "SK하이닉스" 종목 삭제
- "NVIDIA" 종목 추가

**시스템 동작**:
1. **18:00**: WatchlistScheduler 실행 → KIS API 동기화
2. **3-way 동기화 로직**:
   - **그룹명 변경 감지**: `WatchlistGroup.groupName` 업데이트
   - **종목 삭제 감지**: API에 없으면 DB에서 삭제 (orphanRemoval)
   - **종목 추가 감지**: API에 있으면 DB에 추가, `backfillCompleted = false`
3. **백필 플래그 보존**: 기존 종목의 `backfillCompleted` 상태 유지
4. **방어적 처리**: null/중복 stockCode 필터링
5. **히스토리 유지**: 삭제된 종목의 `StockDailyPrice` 데이터는 유지

**실제 결과**:
- ✅ 그룹명 변경 반영
- ✅ SK하이닉스 알림 중단
- ✅ NVIDIA 백필 예약 (다음날 03:00 실행)

### Scenario 3: 실시간 시세 조회 (장중 1분 간격 현재가 수집)

**사용자 액션**:
- 없음 (자동 실행)

**시스템 동작**:
1. **09:00 ~ 15:30 (장중)**: RealtimePriceScheduler 실행 (1분 간격)
2. **관심종목 현재가 조회**: KIS API 호출 (배치 처리)
3. **RealtimeStockPrice 저장**: `current_price`, `change_rate`, `volume`, `timestamp`
4. **변동률 임계값 감지** (Phase 3):
   - 5% 이상 급등/급락 시 즉시 알림

**예상 결과**:
- 장중 실시간 데이터 축적
- 급등/급락 즉시 알림 (Phase 3)

### Scenario 4: 일간 예측 및 알림 (HOLD → BUY 상태 변화)

**사용자 액션**:
- 없음 (자동 실행)

**시스템 동작**:
1. **18:30**: StockPriceScheduler 일간 수집 완료
2. **18:35**: WorkflowOrchestrator 실행
   - AI Advisor 학습 요청 (`POST /v1/train`)
   - AI Advisor 예측 요청 (`POST /v1/predict`)
3. **AI Advisor 응답**:
   - 삼성전자: `BUY` (신뢰도 85%, 예측가 75,000원)
   - SK하이닉스: `HOLD` (신뢰도 60%, 예측가 150,000원)
4. **상태 변화 감지**:
   - 삼성전자: 이전 `HOLD` → 현재 `BUY` ✅ **알림 발송**
   - SK하이닉스: 이전 `HOLD` → 현재 `HOLD` ❌ **알림 스킵**
5. **Notifier 알림 요청** (`POST /v1/notify`):
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
6. **Notifier 카카오톡 전송** → 사용자 수신

**예상 결과**:
- 사용자는 19:00경 카카오톡으로 "삼성전자 매수" 알림 수신

### Scenario 5: 주문 실행 (Notifier 주문 요청 → KIS API 매수/매도)

**사용자 액션**:
- 카카오톡 알림에서 "매수" 버튼 클릭

**시스템 동작**:
1. **Notifier**: 사용자 승인 확인 → Collector에 주문 요청
   - `POST /collector/v1/order`
   ```json
   {
     "user_id": "user123",
     "stock_code": "005930",
     "order_type": "BUY",
     "quantity": 10,
     "price": 75000
   }
   ```
2. **Collector OrderService**:
   - 주문 검증 (잔고 확인, 수량 검증)
   - KIS API 주문 실행
     - `POST /uapi/domestic-stock/v1/trading/order-cash`
     ```json
     {
       "CANO": "계좌번호",
       "PDNO": "005930",
       "ORD_DVSN": "01",  // 시장가
       "ORD_QTY": "10",
       "ORD_UNPR": "0"
     }
     ```
3. **KIS API 응답**:
   - 주문 번호: `20260124-000123`
   - 주문 상태: `접수`
4. **OrderExecution 저장**:
   - 주문 이력 저장 (주문 번호, 체결 여부, 시간)
5. **Notifier 결과 알림**:
   - "삼성전자 10주 매수 주문 완료 (주문번호: 20260124-000123)"

**예상 결과**:
- 사용자는 자동 주문 완료 알림 수신
- 실제 계좌에 매수 주문 체결

---

## Functional Requirements

### FR-1: 데이터 수집

#### FR-1.1: KIS API 토큰 관리
- OAuth2 토큰 발급 (`POST /oauth2/tokenP`)
- Redis 캐싱 (TTL: 24시간)
- 자동 갱신 (만료 1시간 전)
- 계정별 토큰 락 (분산 환경 동시성 제어)

#### FR-1.2: 관심종목 동기화 ✅ 구현 완료
- KIS API 조회 (`GET /uapi/domestic-stock/v1/trading/inquire-psbl-order`)
- MySQL 저장 (`WatchlistGroup`, `WatchlistStock`)
- **3-way 동기화**: API 기준 삭제 전략 (API에 없으면 DB 삭제)
- 그룹/종목 자동 생성, 업데이트, 삭제
- 백필 상태 플래그 보존 (`backfillCompleted`)
- 방어적 프로그래밍 (null/중복 stockCode 처리)

#### FR-1.3: 일간 가격 수집 ✅ 구현 완료
- 4가지 타입 지원: 국내/해외 주식/지수
- **Strategy Pattern 적용**: AssetType별 처리 전략
- 스케줄러: 18:30 (장 마감 후)
- KIS API 조회 (`GET /uapi/domestic-stock/v1/quotations/inquire-daily-price`)
- 중복 체크 및 배치 저장

#### FR-1.4: 과거 데이터 백필 ✅ 구현 완료
- 신규 종목 감지 시 과거 30일 데이터 수집
- 스케줄러: 03:00 (새벽)
- 백필 완료 후 `backfillCompleted = true`
- 중복 데이터 자동 스킵

#### FR-1.5: 실시간 시세 조회 (Phase 2 Week 2)
- 장중 1분 간격 (09:00 ~ 15:30)
- KIS API 조회 (`GET /uapi/domestic-stock/v1/quotations/inquire-price`)
- `RealtimeStockPrice` 저장

### FR-2: 외부 서비스 통신

#### FR-2.1: AI Advisor 통신 (Phase 2 Week 3-4)
- **학습 요청** (`POST /v1/train`):
  ```json
  {
    "user_id": "user123",
    "stock_codes": ["005930", "000660"],
    "start_date": "2026-01-01",
    "end_date": "2026-01-24"
  }
  ```
- **예측 요청** (`POST /v1/predict`):
  ```json
  {
    "user_id": "user123",
    "stock_code": "005930",
    "current_price": 74500,
    "volume": 12000000
  }
  ```
- **응답**:
  ```json
  {
    "stock_code": "005930",
    "decision_type": "BUY",
    "confidence_score": 0.85,
    "predicted_price": 75000,
    "model_version": "v1.2.3"
  }
  ```
- **에러 핸들링**:
  - Timeout (30초) → `UNKNOWN` 상태 반환
  - 재시도 3회 (지수 백오프: 1s, 2s, 4s)

#### FR-2.2: Notifier 통신 (Phase 2 Week 3-4)
- **알림 발송 요청** (`POST /v1/notify`):
  ```json
  {
    "user_id": "user123",
    "stock_code": "005930",
    "stock_name": "삼성전자",
    "decision_type": "BUY",
    "confidence_score": 0.85,
    "predicted_price": 75000,
    "message": "삼성전자 매수 시점입니다."
  }
  ```
- **응답**:
  ```json
  {
    "status": "SUCCESS",
    "message_id": "kakao-20260124-001"
  }
  ```
- **에러 핸들링**:
  - 재시도 3회 (지수 백오프)
  - 실패 시 재시도 큐 추가 (Phase 3)

#### FR-2.3: Notifier 주문 요청 수신 (Phase 3)
- **주문 요청 수신** (`POST /collector/v1/order`):
  ```json
  {
    "user_id": "user123",
    "stock_code": "005930",
    "order_type": "BUY",
    "quantity": 10,
    "price": 75000
  }
  ```

#### FR-2.4: KIS API 주문 실행 (Phase 3)
- **국내 주식 매수** (`POST /uapi/domestic-stock/v1/trading/order-cash`):
  ```json
  {
    "CANO": "계좌번호",
    "PDNO": "005930",
    "ORD_DVSN": "01",  // 시장가
    "ORD_QTY": "10",
    "ORD_UNPR": "0"
  }
  ```
- **국내 주식 매도**: 동일 엔드포인트, `ORD_DVSN` 변경
- **해외 주식 주문** (`POST /uapi/overseas-stock/v1/trading/order`):
  - 미국, 홍콩, 일본 시장 지원

#### FR-2.5: 타임아웃 및 재시도
- **AI Advisor**: Timeout 30초, 재시도 3회
- **Notifier**: Timeout 10초, 재시도 3회
- **KIS API**: Timeout 10초, 재시도 1회 (주문은 재시도 없음)

#### FR-2.6: Circuit Breaker (Phase 3)
- Failure Rate Threshold: 50%
- Wait Duration: 60초
- Fallback: 이전 상태 유지 + 알림

### FR-3: 투자 상태 관리 (Phase 2 Week 3-4)

#### FR-3.1: InvestmentDecision 저장
- 컬럼: `stock_code`, `trade_date`, `decision_type`, `confidence_score`, `predicted_price`, `ai_model_version`
- Enum: `InvestmentState` (BUY, SELL, HOLD, UNKNOWN)

#### FR-3.2: 상태 변화 감지
- 이전 상태 조회 (최신 `InvestmentDecision`)
- 현재 상태와 비교
- 변화 감지 로직:
  - `HOLD → BUY`: 매수 알림
  - `HOLD → SELL`: 매도 알림
  - `BUY → SELL`: 포지션 변경 알림
  - `BUY → BUY`: 알림 스킵 (상태 유지)

#### FR-3.3: 알림 조건
- 상태 변화 발생 시에만 알림 발송
- 신뢰도 임계값: 70% 이상 (설정 가능)

### FR-4: 워크플로우 오케스트레이션 (Phase 2 Week 3-4)

#### FR-4.1: 일간 워크플로우
- **18:35 실행**:
  1. 가격 수집 완료 확인
  2. AI Advisor 학습 요청
  3. AI Advisor 예측 요청 (배치 10개씩)
  4. 상태 변화 감지
  5. Notifier 알림 요청

#### FR-4.2: 에러 핸들링
- 각 단계별 실패 시 로그 기록
- AI Advisor 실패 시 워크플로우 중단
- Notifier 실패 시 재시도 큐 추가

---

## Non-Functional Requirements

### NFR-1: 성능

- **일간 수집**: 100개 종목 기준 10분 이내 완료
- **백필**: 종목당 30일 데이터 30분 이내 완료
- **실시간 시세**: 1분 간격 정확도 ±5초
- **워크플로우 실행**: 전체 프로세스 (학습 요청 → 예측 → 알림) 30분 이내 완료

### NFR-2: 안정성

- **데이터 수집 성공률**: 95% 이상
- **AI Advisor 통신 성공률**: 95% 이상
- **Notifier 알림 성공률**: 98% 이상
- **ShedLock**: 중복 실행 방지
- **토큰 락**: 계정별 동시성 제어
- **Circuit Breaker**: Failure Rate 50% 시 차단 (Phase 3)

### NFR-3: 확장성

- **동시 사용자**: 10명 지원 (초기)
- **관심종목**: 사용자당 100개 지원
- **데이터 저장**: MySQL (100GB 용량)
- **Redis 메모리**: 1GB (토큰 + ShedLock)

### NFR-4: 모니터링

- **Health Check**: `/health` 엔드포인트 (KIS, Redis, MySQL, AI/Notifier 상태)
- **Logging**: 구조화된 JSON 로그 (INFO 레벨)
- **Distributed Tracing**: Trace-ID 전파 (Phase 3)
- **에러 알림**: ERROR 레벨 로그 발생 시 슬랙 알림 (Phase 3)

---

## Constraints & Assumptions

### Constraints

- **KIS API 제한**: 초당 20회 호출 제한
- **AI Advisor**: Python FastAPI 서버 (별도 운영)
- **Notifier**: Spring Boot 서버 (별도 운영)
- **데이터베이스**: MySQL 8.0 이상

### Assumptions

- 사용자는 한국투자증권 계정 보유
- 관심종목은 한국투자증권 앱에서 관리
- AI Advisor는 학습 완료 후 예측 가능
- Notifier는 카카오톡 API 토큰 보유

---

## Success Metrics

### Phase 1 달성 현황 ✅
- **데이터 수집 성공률**: 목표 95% → **100% 달성** (31개 테스트 모두 통과)
- **관심종목 동기화 정확도**: 목표 95% → **100% 달성** (3-way sync 구현)
- **백필 완료율**: 목표 90% → **100% 달성** (30일 데이터 수집)
- **테스트 커버리지**: 목표 80% → **80% 이상 달성**

### Phase 2-3 목표
- **알림 정확도**: AI 예측 신뢰도 80% 이상
- **알림 지연 시간**: 예측 완료 후 5분 이내 발송
- **사용자 만족도**: 알림 수신 후 매수/매도 승인율 60% 이상
- **주문 실행 성공률**: 98% 이상 (Phase 3)

---

## Out of Scope (초기 버전)

- ❌ 웹 UI (관심종목 관리, 투자 현황 대시보드)
- ❌ 자동 주문 실행 (사용자 수동 승인 필요)
- ❌ 포트폴리오 최적화 (종목 비중 조절)
- ❌ 백테스팅 UI (AI 모델 성능 시각화)

---

## References

### MSA 전체 문서
- [README.md](../README.md) - MSA 프로젝트 개요
- [BLUEPRINT.md](../BLUEPRINT.md) - MSA 아키텍처 설계
- [MILESTONE.md](../MILESTONE.md) - MSA 전체 일정 및 의존성
- [CLAUDE.md](../CLAUDE.md) - MSA 작업 지침

### Collector 문서
- [TECHSPEC.md](./TECHSPEC.md) - Collector 기술 명세
- [MILESTONE.md](./MILESTONE.md) - Collector 일정
