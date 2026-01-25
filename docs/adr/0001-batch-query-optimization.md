# 0001. Watchlist 배치 쿼리 최적화

## 상태
Accepted (2026-01-23)

## 컨텍스트

기존 `WatchlistService`의 `syncWatchlist()` 메서드에서 관심종목 목록을 동기화할 때 **N+1 쿼리 문제**가 발생했습니다.

### 문제 상황
```java
// 기존 코드 (의사 코드)
for (WatchlistStockResponse stock : apiStocks) {
    repository.findByAccountAndGroupNoAndSymbol(...)  // 반복적인 SELECT 쿼리
}
```

- 관심종목이 100개라면 최소 100번의 개별 SELECT 쿼리 발생
- DB 왕복 횟수 증가로 인한 성능 저하
- 스케줄러가 하루 2회(08:00, 18:00) 실행되므로 누적 부하 발생 가능

## 결정

**배치 쿼리를 사용하여 한 번의 쿼리로 모든 관심종목을 조회하는 방식으로 변경**

### 구현 방법
1. Repository에 배치 조회 메서드 추가:
   ```java
   List<WatchlistStock> findByAccountAndGroupNoIn(
       String account,
       List<Integer> groupNos
   )
   ```

2. 메모리에서 Map 기반 매칭:
   ```java
   Map<String, WatchlistStock> existingStocksMap = existingStocks.stream()
       .collect(Collectors.toMap(
           stock -> stock.getGroupNo() + ":" + stock.getSymbol(),
           Function.identity()
       ));
   ```

3. API 응답과 기존 데이터를 메모리에서 비교하여 변경사항 추출

## 결과

### 긍정적 영향
- **쿼리 횟수 대폭 감소**: N+1 → 1회
- **응답 시간 개선**: DB 왕복 횟수 최소화
- **확장성 향상**: 관심종목이 늘어나도 쿼리 횟수는 일정
- **메모리 효율**: Map 기반 조회는 O(1) 시간 복잡도

### 부정적 영향
- **메모리 사용 증가**: 전체 관심종목을 메모리에 로드
  - 완화: 일반적으로 관심종목은 수백 개 수준이므로 메모리 부담 미미
- **코드 복잡도 증가**: Map 기반 로직으로 가독성 약간 저하

## 대안

### 대안 1: QueryDSL 동적 쿼리
- 장점: 유연한 쿼리 작성 가능
- 단점: QueryDSL 의존성 추가 필요, 현재 요구사항에 과도한 설계

### 대안 2: JPA EntityGraph
- 장점: 연관관계 페치 조인 최적화
- 단점: 현재는 단일 엔티티 조회이므로 적용 불가

### 대안 3: Native Query
- 장점: 최대 성능
- 단점: 유지보수성 저하, 데이터베이스 종속성

## 참고
- 커밋: `83b3ba1 ⚡ Watchlist 반복 쿼리 배치 처리 도입`
- 관련 파일: `WatchlistService.java`, `WatchlistStockRepository.java`