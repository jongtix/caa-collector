# 0009. StockPrice 도메인에 Strategy Pattern 도입

## 상태
Accepted (2026-01-26)

## 컨텍스트

`StockPriceCollectorService`와 `StockBackfillService`에서 AssetType별 처리 로직이 3곳에서 반복적으로 switch 문으로 구현되어 있었습니다.

### 문제 상황

```java
// StockPriceCollectorService.java
public void collectDailyPrice(WatchlistStock stock, LocalDate date) {
    switch (stock.getAssetType()) {
        case DOMESTIC_STOCK -> collectDomesticStockDailyPrice(stock, date);
        case DOMESTIC_INDEX -> collectDomesticIndexDailyPrice(stock, date);
        case OVERSEAS_STOCK -> collectOverseasStockDailyPrice(stock, date);
        case OVERSEAS_INDEX -> collectOverseasIndexDailyPrice(stock, date);
    }
}

// StockBackfillService.java (2곳)
// 1. backfillHistoricalPrices()에서 동일한 switch
// 2. backfillByType()에서 동일한 switch
```

**문제점**:
- **코드 중복**: 3곳에서 동일한 switch 문 반복
- **OCP 위반**: 새로운 AssetType 추가 시 3곳을 모두 수정해야 함
- **테스트 어려움**: AssetType별 로직이 Service에 강결합되어 독립 테스트 불가
- **확장성 부족**: 새로운 처리 로직 추가 시 Service 클래스가 비대해짐

### 추가 개선 사항

**1. StockPriceFetchService 제거**
- `KisStockPriceService` 메서드를 단순 위임만 하는 불필요한 레이어
- Orchestration 계층인 `StockPriceCollectorService`에서 직접 호출하도록 변경

**2. Date Parsing 중앙화**
- 4가지 타입별로 각각 구현되어 있던 `LocalDate.parse()` 로직
- `StockPriceConstants.parseDate()`로 통합

**3. Generic 메서드 도입**
- 95% 동일한 4개의 Backfill 메서드 (AssetType별)
- `backfillDailyPrices()`와 `saveBatchPrices()` 2개의 Generic 메서드로 통합

## 결정

**Strategy Pattern 도입 + Generic 메서드 + 불필요한 레이어 제거**

### 구현 방법

#### 1. Strategy 인터페이스 정의

```java
public interface StockPriceStrategy {
    int collectDailyPrice(WatchlistStock stock, LocalDate startDate, LocalDate endDate);
    void backfillHistoricalPrices(WatchlistStock stock, LocalDate startDate, LocalDate endDate);
}
```

#### 2. AssetType별 Strategy 구현

```java
@Component
public class DomesticStockStrategy implements StockPriceStrategy {
    private final KisStockPriceService kisStockPriceService;
    private final DomesticStockDailyPriceRepository repository;

    @Override
    public int collectDailyPrice(WatchlistStock stock, LocalDate startDate, LocalDate endDate) {
        // 국내 주식 수집 로직
    }

    @Override
    public void backfillHistoricalPrices(WatchlistStock stock, LocalDate startDate, LocalDate endDate) {
        // 국내 주식 백필 로직
    }
}
```

#### 3. Factory로 Strategy 제공

```java
@Component
public class StockPriceStrategyFactory {
    private Map<AssetType, StockPriceStrategy> strategies;

    public StockPriceStrategy getStrategy(AssetType assetType) {
        return strategies.get(assetType);
    }
}
```

#### 4. Service에서 Strategy 사용

```java
// Before (3곳에서 switch 문)
public void collectDailyPrice(WatchlistStock stock, LocalDate date) {
    switch (stock.getAssetType()) {
        case DOMESTIC_STOCK -> collectDomesticStockDailyPrice(stock, date);
        // ...
    }
}

// After (1줄로 단순화)
public void collectDailyPrice(WatchlistStock stock, LocalDate date) {
    var strategy = strategyFactory.getStrategy(stock.getAssetType());
    strategy.collectDailyPrice(stock, date, date);
}
```

#### 5. Generic 메서드로 중복 제거

```java
// Before (4개의 유사한 메서드)
private void backfillDomesticStock(WatchlistStock stock, LocalDate startDate, LocalDate endDate) { ... }
private void backfillDomesticIndex(WatchlistStock stock, LocalDate startDate, LocalDate endDate) { ... }
private void backfillOverseasStock(WatchlistStock stock, LocalDate startDate, LocalDate endDate) { ... }
private void backfillOverseasIndex(WatchlistStock stock, LocalDate startDate, LocalDate endDate) { ... }

// After (1개의 Generic 메서드)
private <T> void backfillDailyPrices(
    TriFunction<String, LocalDate, LocalDate, List<T>> apiFetcher,
    Function<T, LocalDate> dateExtractor,
    BiFunction<WatchlistStock, T, ?> entityMapper,
    JpaRepository<?, ?> repository,
    WatchlistStock stock,
    LocalDate startDate,
    LocalDate endDate
) { ... }
```

## 결과

### 긍정적 영향

#### 코드 품질
- **완전한 중복 제거**: 3곳의 switch 문 → 1곳의 Factory 호출
- **OCP 준수**: 새로운 AssetType 추가 시 Strategy 구현체만 추가
- **SRP 준수**: 각 Strategy가 단일 AssetType 처리에만 집중
- **테스트 용이성**: Strategy별 독립적인 단위 테스트 가능

#### 유지보수성
- **레이어 단순화**: StockPriceFetchService 제거로 불필요한 위임 제거
- **중앙화**: Date Parsing 로직을 `StockPriceConstants.parseDate()`로 통합
- **Generic 활용**: 4개 메서드 → 1개 Generic 메서드로 통합

#### 확장성
- **새로운 AssetType 추가**: Strategy 구현체 1개 + Factory 등록 1줄만 추가
- **타입 안전성**: EnumMap 사용으로 컴파일 타임 타입 안전성 확보

### 부정적 영향

- **파일 개수 증가**: 8개의 새로운 파일 생성
  - 6개의 Strategy 관련 파일
  - 2개의 함수형 인터페이스 (TriFunction, QuadFunction)
- **간접성 증가**: Factory를 거쳐야 하는 1단계 추가
- **학습 곡선**: Strategy Pattern에 대한 이해 필요

## 대안

### 대안 1: 현재 switch 문 유지

```java
// 3곳에서 switch 문 반복
switch (stock.getAssetType()) {
    case DOMESTIC_STOCK -> collectDomesticStockDailyPrice(stock, date);
    // ...
}
```

- 장점: 간단하고 직관적
- 단점: 중복 코드, OCP 위반, 테스트 어려움

### 대안 2: Map<AssetType, Consumer> 사용

```java
private Map<AssetType, BiConsumer<WatchlistStock, LocalDate>> collectors;

@PostConstruct
void init() {
    collectors = Map.of(
        DOMESTIC_STOCK, this::collectDomesticStockDailyPrice,
        // ...
    );
}
```

- 장점: Strategy Pattern보다 간단
- 단점: 타입별 로직이 여전히 Service에 강결합, 독립 테스트 불가

### 대안 3: Polymorphism (WatchlistStock에 로직 위임)

```java
public abstract class WatchlistStock {
    abstract void collectDailyPrice(LocalDate date);
}
```

- 장점: 완전한 OOP
- 단점: Entity에 비즈니스 로직 혼입, 도메인 모델 복잡화

## 채택 이유

1. **완전한 관심사 분리**: 각 Strategy가 독립적으로 AssetType 처리 로직 관리
2. **OCP 준수**: 새로운 AssetType 추가 시 기존 코드 수정 불필요
3. **테스트 용이성**: Strategy별 독립적인 단위 테스트 가능
4. **명시적 구조**: Factory Pattern으로 AssetType-Strategy 매핑이 명확
5. **확장성**: Generic 메서드로 중복 코드 제거하면서도 타입 안전성 유지

## 트레이드오프

| 항목 | Before | After | 평가 |
|-----|--------|-------|------|
| switch 문 개수 | 3곳 | 0곳 | ✅ 완전 제거 |
| 파일 개수 | 2개 | 10개 | ⚠️ 8개 증가 |
| 테스트 용이성 | 낮음 | 높음 | ✅ 독립 테스트 가능 |
| 새로운 타입 추가 | 3곳 수정 | 2개 파일 추가 | ✅ 기존 코드 수정 불필요 |
| 코드 중복 | 높음 (95% 유사 메서드 4개) | 낮음 (Generic 1개) | ✅ 중복 제거 |
| 레이어 개수 | 3계층 | 2계층 | ✅ 불필요한 레이어 제거 |

## 참고

- 관련 파일:
  - **Strategy Interface**: `StockPriceStrategy.java`
  - **Strategy 구현체**: `DomesticStockStrategy.java`, `DomesticIndexStrategy.java`, `OverseasStockStrategy.java`, `OverseasIndexStrategy.java`
  - **Factory**: `StockPriceStrategyFactory.java`
  - **함수형 인터페이스**: `TriFunction.java`, `QuadFunction.java`
  - **Service**: `StockPriceCollectorService.java` (switch 제거), `StockBackfillService.java` (Generic 메서드 도입)
- 적용 범위: stockprice 도메인 전체
- 설계 원칙: OCP, SRP, Strategy Pattern
