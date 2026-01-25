# 0007. 대량 데이터 조회 시 페이징 처리

## 상태
Accepted (2026-01-25)

## 컨텍스트

`StockPriceCollectorService`에서 관심종목 전체를 한 번에 조회하여 메모리에 로드했습니다.

### 문제 상황
```java
var stocks = watchlistStockRepository.findByBackfillCompleted(true);  // 전체 조회
log.info("Collecting daily prices for {} stocks", stocks.size());

for (var stock : stocks) {
    // 처리...
}
```

- 관심종목이 1000개라면 1000개 전체를 메모리에 로드
- JPA는 기본적으로 모든 결과를 한 번에 가져옴
- OOM(Out Of Memory) 위험
- GC 압력 증가

## 결정

**페이징 처리를 도입하여 100개씩 나누어 조회**

### 구현 방법

1. **Repository에 페이징 메서드 추가**:
   ```java
   Page<WatchlistStock> findByBackfillCompleted(
       boolean backfillCompleted,
       Pageable pageable
   );
   ```

2. **Service에서 페이징 루프 처리**:
   ```java
   private static final int PAGE_SIZE = 100;

   public void collectDailyPrices() {
       Pageable pageable = PageRequest.of(0, PAGE_SIZE);
       Page<WatchlistStock> page;

       do {
           page = repository.findByBackfillCompleted(true, pageable);

           page.getContent().forEach(stock -> {
               // 처리 로직
           });

           pageable = page.nextPageable();
       } while (page.hasNext());
   }
   ```

## 결과

### 긍정적 영향
- **메모리 효율**: 한 번에 최대 100개만 메모리에 로드
- **확장성**: 관심종목이 늘어나도 메모리 사용량 일정
- **안정성**: OOM 위험 제거
- **예외 처리 독립성**: 각 페이지는 독립적인 트랜잭션, 일부 실패해도 나머지 계속 처리
- **모니터링**: 페이지별 진행 상황 로깅 가능

### 부정적 영향
- **코드 복잡도 증가**: 단순 for-each에서 페이징 루프로 변경
  - 완화: do-while 패턴으로 가독성 유지
- **쿼리 횟수 증가**: 1000개 데이터 = 10번 쿼리 (페이지당 1번)
  - 완화: 각 쿼리는 100개만 조회하므로 빠름

## 대안

### 대안 1: Stream 방식
```java
@Query("SELECT w FROM WatchlistStock w WHERE w.backfillCompleted = :completed")
Stream<WatchlistStock> streamByBackfillCompleted(@Param("completed") boolean completed);

// 사용
@Transactional(readOnly = true)
public void collectDailyPrices() {
    try (Stream<WatchlistStock> stream = repository.streamByBackfillCompleted(true)) {
        stream.forEach(stock -> { /* 처리 */ });
    }
}
```
- 장점: 메모리 효율적 (한 번에 하나씩 처리)
- 단점:
  - **@Transactional 필수** (Stream은 Lazy, 트랜잭션 끝나면 Session 닫힘)
  - try-with-resources 필수 (Stream 닫지 않으면 메모리 누수)
  - 각 stock 처리 중 예외 발생 시 전체 트랜잭션 롤백 위험

### 대안 2: ScrollPosition API (Spring Data JPA 3.1+)
```java
Window<WatchlistStock> findTop100ByBackfillCompleted(
    boolean backfillCompleted,
    ScrollPosition position
);
```
- 장점: 커서 기반으로 오프셋보다 빠름
- 단점: API가 복잡, 현재 요구사항에 과도한 설계

### 대안 3: 현재 방식 유지 (전체 조회)
- 장점: 코드 간단
- 단점: 확장성 없음, 메모리 위험

## 채택 이유: 페이징 (대안 2-B)

1. **안정성**: 트랜잭션 경계가 명확하여 부분 실패 처리 가능
2. **실용성**: Stream의 메모리 장점 + 예외 처리의 유연성
3. **테스트**: Mock 기반 테스트가 Stream보다 쉬움
4. **현재 코드와 일관성**: 이미 forEach 패턴 사용 중

## 성능 비교

| 항목 | 전체 조회 | 페이징 (100개) | Stream |
|------|---------|--------------|--------|
| 메모리 | 1000개 | 100개 | 1개씩 |
| 쿼리 횟수 | 1회 | 10회 | 1회 (fetch size로 분할) |
| 트랜잭션 | 1개 (큰 트랜잭션) | 10개 (작은 트랜잭션) | 1개 (큰 트랜잭션) |
| 예외 처리 | 전체 롤백 | 페이지별 독립 | 전체 롤백 |

## 참고
- 관련 파일: `StockPriceCollectorService.java:45-57`
- 관련 Repository: `WatchlistStockRepository.java`
- PAGE_SIZE 설정: 100개 (조정 가능)
