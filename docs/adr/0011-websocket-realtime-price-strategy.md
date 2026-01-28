# ADR-0011: 실시간 시세 조회 방식 (REST API vs WebSocket)

## 상태
✅ **승인됨** (2026-01-27)

## 컨텍스트

Phase 2 Week 2에서 실시간 시세 조회 기능을 개발할 예정입니다. 초기 계획(TODO.md)은 REST API 폴링 방식으로 설계되어 있었습니다:

- **REST API 엔드포인트**:
  - 국내 주식: `GET /uapi/domestic-stock/v1/quotations/inquire-price`
  - 해외 주식: `GET /uapi/overseas-price/v1/quotations/price`
- **스케줄러**: 장중 1분 간격 (`*/1 9-15 * * MON-FRI`)
- **예상 시간**: 10시간

### REST API 폴링 방식의 문제점

1. **응답 지연**
   - API 호출 → 응답 대기 시간 (수백ms ~ 수초)
   - 100개 종목 조회 시 최소 5초 소요 (Rate Limiter 20/s)

2. **실시간성 부족**
   - 1분 간격으로는 급등/급락 대응 불가
   - 틱 단위 가격 변화 추적 불가능

3. **Rate Limiter 제약**
   - KIS API 초당 20회 제한
   - 관심종목이 많을수록 전체 조회 시간 증가

4. **불필요한 네트워크 부하**
   - 가격 변화가 없어도 매번 요청
   - 장중 6시간 × 60분 = 360회 요청/종목

### WebSocket 방식의 장점

1. **즉시성**
   - 가격 변화 시 즉시 푸시 수신 (ms 단위)
   - 틱 단위 데이터 수신 가능

2. **효율성**
   - 한 번 연결 후 지속적 수신 (Rate Limiter 우회)
   - 최대 20개 종목 동시 구독 가능

3. **정확성**
   - 1분 간격이 아닌 실시간 틱 데이터
   - 급등/급락 즉시 감지

4. **네트워크 효율**
   - 연결 유지만으로 지속 수신
   - 불필요한 폴링 제거

## 결정

**WebSocket 기반 실시간 구독 방식**을 채택합니다.

### 핵심 설계

#### 1. 승인키 발급 (REST API)
```http
POST https://openapi.koreainvestment.com:9443/oauth2/Approval
```

#### 2. WebSocket 연결
```
ws://ops.koreainvestment.com:21000 (실전투자)
ws://ops.koreainvestment.com:31000 (모의투자)
```

#### 3. 구독 관리
- TR 코드: `H0STCNT0` (실시간 주식 체결가)
- 최대 20개 종목 동시 구독
- 관심종목 기반 동적 구독/구독 해제

#### 4. 메시지 처리
- 비동기 처리 (ExecutorService, 스레드 풀 5개)
- Back-pressure 처리 (Semaphore 100)
- 5초 샘플링 (메모리 버퍼 → 배치 저장)

#### 5. 재연결 전략
- 지수 백오프: 1s, 2s, 4s, 8s, 16s
- 최대 5회 재시도
- 재연결 시 자동 재구독

## 대안 (고려했으나 채택하지 않음)

### 대안 1: REST API 폴링 방식
- **장점**: 구현 단순, 기존 RestClient 재사용
- **단점**: 실시간성 부족, Rate Limiter 제약, 네트워크 비효율
- **결정**: 거부 (실시간성 부족)

### 대안 2: Kafka 기반 스트리밍
- **장점**: 메시지 큐 기반 안정성, 스케일 아웃 용이
- **단점**: 인프라 복잡도 증가, KIS API가 Kafka 미지원
- **결정**: 보류 (Phase 4에서 고려)

### 대안 3: Server-Sent Events (SSE)
- **장점**: 단방향 스트리밍에 적합, 구현 간단
- **단점**: KIS API가 SSE 미지원
- **결정**: 불가능 (KIS API 제약)

## 결과

### 긍정적 영향

1. **실시간성 확보**
   - ms 단위 가격 변화 추적
   - 급등/급락 즉시 대응 가능

2. **효율성 향상**
   - Rate Limiter 제약 우회
   - 네트워크 부하 90% 감소 (360회 → 1회 연결)

3. **확장성**
   - 최대 20개 종목 동시 처리
   - AI Advisor 학습 데이터 품질 향상

### 부정적 영향

1. **개발 복잡도 증가**
   - 예상 시간: 10시간 → 15시간 (+50%)
   - WebSocket 연결 관리, 재연결 로직 필요

2. **테스트 복잡도 증가**
   - Mock WebSocket Server 구현 필요
   - 재연결 시나리오 테스트 필요

3. **운영 모니터링 필요**
   - 연결 상태 추적
   - Health Check 통합
   - 구독 상태 관리

### 트레이드오프 분석

| 항목 | REST API 폴링 | WebSocket 구독 |
|------|--------------|----------------|
| **개발 시간** | 10시간 | 15시간 (+50%) |
| **실시간성** | 1분 간격 | ms 단위 |
| **Rate Limiter** | 20/s 제약 | 제약 없음 |
| **네트워크 효율** | 360회/종목/일 | 1회 연결 |
| **복잡도** | 낮음 | 중간 |
| **테스트** | 단순 | 복잡 |
| **확장성** | 낮음 | 높음 |

**최종 판단**: 개발 시간 증가(+5시간)는 실시간성 확보와 효율성 향상으로 충분히 상쇄됩니다.

## 구현 계획

### Phase 2 Week 2 (2026-01-27 ~ 02-08)

1. **KIS WebSocket API 스펙 조사** (2시간)
   - 승인키 발급 절차
   - 메시지 포맷 파악

2. **WebSocketClient 구현** (4시간)
   - 연결 관리, 재연결 로직
   - Heartbeat 처리

3. **Entity/Repository 설계** (2시간)
   - `RealtimeStockPrice` Entity
   - Upsert 로직

4. **SubscriptionManager 구현** (2.5시간)
   - 구독/구독 해제
   - 관심종목 동기화

5. **MessageHandler 및 Sampler** (2시간)
   - 비동기 메시지 처리
   - 5초 샘플링 저장

6. **테스트 작성** (2.5시간)
   - Mock WebSocket Server
   - 재연결 시나리오

## 관련 문서

- [TECHSPEC.md - WebSocket Architecture](../TECHSPEC.md#websocket-architecture-phase-2-week-2)
- [TODO.md - Phase 2 Week 2](../TODO.md#priority-1-p1---important)
- [MILESTONE.md - Phase 2](../MILESTONE.md#phase-2-실시간-시세-및-외부-서비스-통신-🚧-진행-중)

## 참고 자료

- [KIS Developers Portal](https://apiportal.koreainvestment.com/intro)
- [GitHub - koreainvestment/open-trading-api](https://github.com/koreainvestment/open-trading-api)
- [Java 한국투자증권 OpenAPI 사용 (WebSocket)](https://velog.io/@seon7129/JAVA-%ED%95%9C%EA%B5%AD%ED%88%AC%EC%9E%90%EC%A6%9D%EA%B6%8C-OpenAPI-%EC%82%AC%EC%9A%A9-Websocket)
- [WikiDocs - 한국투자증권 Websocket 예제](https://wikidocs.net/book/7847)

## 검토 기록

- **2026-01-27**: pm 에이전트 및 backend-developer 에이전트 검토 완료
- **2026-01-27**: WebSocket 방식 최종 승인