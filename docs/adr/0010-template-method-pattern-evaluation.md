# 0010. Template Method Pattern 도입 검토 및 보류 결정

## 상태
Deferred (2026-01-26)

## 컨텍스트

`StockPriceCollectionService`의 `collectDailyPrices()`와 `backfillHistoricalPrices()` 메서드에서 페이징 로직이 85% 중복되어 있습니다.

### 문제 상황

```java
// StockPriceCollectionService.java
public void collectDailyPrices() {
    Pageable pageable = PageRequest.of(0, PAGE_SIZE);
    Page<WatchlistStock> page;

    do {
        page = watchlistStockRepository.findByBackfillCompleted(true, pageable);
        log.info("Collecting daily prices for {} stocks (page {}/{})",
                page.getNumberOfElements(), page.getNumber() + 1, page.getTotalPages());

        page.getContent().forEach(stock -> {
            try {
                var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
                collectDailyPriceByAssetType(stock, today, today);
            } catch (Exception e) {
                log.error("Failed to collect daily price for stock: {}", stock.getStockCode(), e);
            }
        });

        pageable = page.nextPageable();
    } while (page.hasNext());
}

public void backfillHistoricalPrices() {
    Pageable pageable = PageRequest.of(0, PAGE_SIZE);
    Page<WatchlistStock> page;

    do {
        page = watchlistStockRepository.findByBackfillCompleted(false, pageable);
        log.info("Backfilling historical prices for {} stocks (page {}/{})",
                page.getNumberOfElements(), page.getNumber() + 1, page.getTotalPages());

        page.getContent().forEach(stock -> {
            try {
                var endDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
                stockBackfillService.backfillSingleStock(stock, DEFAULT_START_DATE, endDate);
            } catch (Exception e) {
                log.error("Failed to backfill prices for stock: {}", stock.getStockCode(), e);
            }
        });

        pageable = page.nextPageable();
    } while (page.hasNext());
}
```

**중복 코드 (85%)**:
- 페이징 초기화 로직
- `do-while` 페이징 반복 구조
- 로깅 패턴
- 예외 처리 구조
- `page.nextPageable()` 호출

**차이점 (15%)**:
- Repository 쿼리 메서드 (`findByBackfillCompleted(true)` vs `findByBackfillCompleted(false)`)
- 로그 메시지 ("Collecting" vs "Backfilling")
- 처리 메서드 (`collectDailyPriceByAssetType()` vs `stockBackfillService.backfillSingleStock()`)
- 날짜 파라미터 (`today, today` vs `DEFAULT_START_DATE, endDate`)

### 세 에이전트 분석 결과

#### code-refactor-master 의견
- **초기 분석**: Template Method Pattern 적용 가능
- **최종 판단**: Rule of Three 미충족 (2개만 존재)
- **권장사항**: Critical 문제 우선 해결 후 3번째 유사 메서드 추가 시 재검토

#### code-refactorer 의견
- **초기 분석**: 중복 85%, Template Method Pattern으로 제거 가능
- **최종 판단**: 현재 시점에서는 과도한 설계
- **권장사항**: Phase 2(3번째 메서드 추가 시) 재검토

#### code-reviewer 의견
- **보안 관점**: Critical 트랜잭션 경계 오류가 더 심각
- **우선순위**: 트랜잭션 버그 > 중복 코드
- **권장사항**: Critical 문제 해결 우선

### Critical 문제 (우선 해결 대상)

#### 1. Transaction Boundary Mismatch 🔴 CRITICAL
**Location**: `StockBackfillService.java:38-51`

**Problem**:
```java
@Transactional(propagation = Propagation.REQUIRED)
public void backfillSingleStock(WatchlistStock stock, ...) {
    strategy.backfillHistoricalPrices(stock, startDate, endDate); // REQUIRES_NEW 트랜잭션
    stock.markBackfillCompleted(); // ❌ 엔티티 변경이 DB에 저장되지 않음
}
```

**Impact**: 가격 데이터는 저장되지만 `backfillCompleted` 플래그가 false로 유지 → 중복 수집 발생

#### 2. Inadequate Exception Handling 🟡 MAJOR
**Location**: `StockPriceCollectionService.java:64-66`

**Problem**:
- 모든 예외를 동일하게 처리 (재시도 가능/불가능 구분 없음)
- 성공률/실패율 추적 불가
- 운영 가시성 부족

#### 3. Timezone Not Specified 🟡 MAJOR
**Locations**:
- `StockPriceCollectionService.java:62, 89`
- `KisAuthService.java:92`

**Problem**: JVM 기본 시간대에 의존 → 서버 위치에 따라 날짜 불일치 발생 가능

## 결정

**Template Method Pattern 도입을 현재 시점에서 보류하고, Critical 문제 해결을 우선합니다.**

### 보류 이유

#### 1. Rule of Three 미충족
- **현재 상황**: 유사한 메서드가 2개만 존재
- **Rule of Three**: 3번째 중복이 발생할 때 추상화 도입
- **판단**: 조기 최적화를 피하고 실제 필요 시점까지 대기

#### 2. Critical 문제의 심각성
- **트랜잭션 버그**: 데이터 무결성 위협 (중복 수집 발생)
- **예외 처리 부재**: 운영 가시성 부족
- **시간대 미지정**: 날짜 불일치 가능성

#### 3. 변화 가능성
- Phase 2에서 추가 메서드 도입 시 요구사항이 달라질 가능성
- 현재 시점에서 추상화 도입 시 불필요한 복잡도 증가 우려

### 대신 적용한 개선사항

#### Phase 1: Timezone Fix ✅
```java
// Before
var today = LocalDate.now();

// After
var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
```

**적용 위치**:
- `StockPriceCollectionService.java:62, 89`
- `KisAuthService.java:92`

#### Phase 2: BatchStatistics ✅
```java
public class BatchStatistics {
    private int total;
    private int success;
    private int recoverableFailure;  // KisApiException
    private int criticalFailure;     // DataAccessException
    private int unexpectedFailure;   // Other exceptions

    public String getSummary() {
        return String.format(
            "Total: %d, Success: %d (%.2f%%), Recoverable: %d, Critical: %d, Unexpected: %d",
            total, success, getSuccessRate(),
            recoverableFailure, criticalFailure, unexpectedFailure
        );
    }
}
```

**예외 분류**:
- `KisApiException`: 재시도 가능한 API 오류
- `DataAccessException`: Critical DB 오류
- `Exception`: 예상하지 못한 오류

**로깅 개선**:
```java
log.info("Daily price collection completed. {}", stats.getSummary());

if (stats.getCriticalFailure() > 0) {
    log.error("ALERT: {} critical database failures detected!", stats.getCriticalFailure());
}
```

#### Phase 3: Transaction Fix ✅
```java
// Before
@Transactional(propagation = Propagation.REQUIRED)
public void backfillSingleStock(WatchlistStock stock, ...) {
    strategy.backfillHistoricalPrices(stock, startDate, endDate);
    stock.markBackfillCompleted(); // ❌ 저장되지 않음
}

// After
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void backfillSingleStock(WatchlistStock stock, ...) {
    strategy.backfillHistoricalPrices(stock, startDate, endDate);
    stock.markBackfillCompleted();
    watchlistStockRepository.save(stock); // ✅ 명시적 저장
}
```

**개선 효과**:
- 독립적인 트랜잭션으로 실행 (다른 종목 실패에 영향 없음)
- `backfillCompleted` 플래그가 확실히 저장됨
- 데이터 무결성 보장

## 결과

### 긍정적 영향

#### Critical 문제 해결
- ✅ 트랜잭션 경계 오류 수정 → 데이터 무결성 보장
- ✅ 예외 처리 개선 → 운영 가시성 확보
- ✅ 시간대 명시 → 날짜 불일치 방지

#### 코드 품질
- 예외 유형별 분류로 장애 대응 시간 단축
- BatchStatistics로 성공률 추적 가능
- Critical 장애 알림 자동화

#### 유지보수성
- 명시적 트랜잭션 경계로 의도 명확화
- 시간대 명시로 서버 환경 독립성 확보

### 부정적 영향

- 중복 코드 85% 여전히 존재 (추후 개선 필요)

## 대안

### 대안 1: Template Method Pattern 즉시 적용

```java
public abstract class StockPricePagedProcessor {
    public final void processPaged() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<WatchlistStock> page;

        do {
            page = fetchPage(pageable);
            log.info(getLogMessage(), page.getNumberOfElements(), page.getNumber() + 1, page.getTotalPages());

            page.getContent().forEach(this::processStock);

            pageable = page.nextPageable();
        } while (page.hasNext());
    }

    protected abstract Page<WatchlistStock> fetchPage(Pageable pageable);
    protected abstract String getLogMessage();
    protected abstract void processStock(WatchlistStock stock);
}
```

- 장점: 중복 코드 완전 제거
- 단점:
  - Rule of Three 미충족 (조기 최적화)
  - 간접성 증가 (추상 클래스 1개, 구체 클래스 2개 추가)
  - 요구사항 변화 시 템플릿 수정 필요

### 대안 2: 함수형 조합

```java
private void processPaged(
    Function<Pageable, Page<WatchlistStock>> fetcher,
    String logMessage,
    Consumer<WatchlistStock> processor
) {
    Pageable pageable = PageRequest.of(0, PAGE_SIZE);
    Page<WatchlistStock> page;

    do {
        page = fetcher.apply(pageable);
        log.info(logMessage, page.getNumberOfElements(), page.getNumber() + 1, page.getTotalPages());
        page.getContent().forEach(processor);
        pageable = page.nextPageable();
    } while (page.hasNext());
}

// 사용
processPaged(
    p -> watchlistStockRepository.findByBackfillCompleted(true, p),
    "Collecting daily prices for {} stocks (page {}/{})",
    stock -> {
        var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        collectDailyPriceByAssetType(stock, today, today);
    }
);
```

- 장점: 클래스 추가 없이 중복 제거 가능
- 단점:
  - 예외 처리, 통계 추적 등 추가 요구사항 반영 어려움
  - 가독성 저하 (람다 내부 로직이 길어질 경우)

### 대안 3: Builder Pattern

```java
new PagedStockProcessor()
    .withQuery(p -> watchlistStockRepository.findByBackfillCompleted(true, p))
    .withLogMessage("Collecting daily prices for {} stocks (page {}/{})")
    .withProcessor(stock -> collectDailyPriceByAssetType(stock, today, today))
    .withExceptionHandler(this::handleException)
    .execute();
```

- 장점: 유연성 높음, 확장 용이
- 단점:
  - 클래스 추가 (Builder 1개)
  - 현재 요구사항 대비 과도한 설계

## 채택 이유

1. **Rule of Three 원칙**: 3번째 중복 발생 시 추상화 도입이 적절
2. **Critical 문제 우선**: 트랜잭션 버그가 데이터 무결성에 더 심각한 영향
3. **변화 가능성**: Phase 2에서 요구사항이 변경될 가능성
4. **단순성 유지**: 조기 최적화보다 현재 Critical 문제 해결이 우선
5. **개선 여지 확보**: 추후 Template Method Pattern 재검토 가능

## 재검토 조건

다음 상황에서 Template Method Pattern 도입을 재검토합니다:

### Phase 2: 3번째 유사 메서드 추가 시
- **예시**: `collectRealTimeUpdates()`, `reprocessFailedStocks()` 등
- **조건**: 페이징 처리 로직이 80% 이상 유사
- **판단 기준**: Rule of Three 충족

### 추가 고려사항
- 3번째 메서드가 추가될 때 요구사항을 재분석
- Template Method Pattern vs 함수형 조합 vs Builder Pattern 재비교
- 현재 시점(2026-01-26)의 개선사항(BatchStatistics, 예외 분류)을 템플릿에 반영

## 트레이드오프

| 항목 | Template Method 즉시 적용 | Critical 우선 + 보류 | 평가 |
|-----|------------------------|------------------|------|
| 중복 코드 제거 | ✅ 완전 제거 | ⚠️ 85% 유지 | ⚠️ 추후 개선 |
| Critical 버그 해결 | ⏸️ 지연 | ✅ 즉시 해결 | ✅ 데이터 무결성 우선 |
| 클래스 개수 | ⚠️ 3개 증가 | ✅ 1개 증가 | ✅ 단순성 유지 |
| 간접성 | ⚠️ 추상 계층 추가 | ✅ 직접 호출 | ✅ 가독성 유지 |
| Rule of Three | ❌ 미충족 | ✅ 준수 | ✅ 조기 최적화 방지 |
| 운영 가시성 | - | ✅ BatchStatistics | ✅ 장애 대응 개선 |

## 참고

- 관련 파일:
  - **Modified**: `StockBackfillService.java` (트랜잭션 경계 수정)
  - **Modified**: `StockPriceCollectionService.java` (시간대 명시, 예외 처리 개선)
  - **Modified**: `KisAuthService.java` (시간대 명시)
  - **New**: `BatchStatistics.java` (통계 추적)
- 관련 ADR:
  - [ADR-0009](0009-stockprice-strategy-pattern.md) - StockPrice Strategy Pattern 도입
  - [ADR-0007](0007-pagination-for-bulk-data-query.md) - 페이징 처리 표준화
- 설계 원칙: Rule of Three, YAGNI (You Aren't Gonna Need It)
- 재검토 시점: Phase 2 (3번째 유사 메서드 추가 시)
