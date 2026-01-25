# 0008. Rate Limiter 중앙화 - KisRestClient로 이동

## 상태
Accepted (2026-01-25)

## 컨텍스트

`StockBackfillService`와 `StockPriceCollectorService`에서 KIS API 호출 전 `kisApiRateLimiter.acquire()`를 각각 4곳씩 총 8곳에서 중복 호출하고 있었습니다.

### 문제 상황

```java
// StockBackfillService.java (4곳)
private void backfillDomesticStock(...) {
    kisApiRateLimiter.acquire();  // 중복 #1
    var prices = kisStockPriceService.getDomesticStockDailyPrices(...);
}

// StockPriceCollectorService.java (4곳)
private void collectDomesticStockDailyPrice(...) {
    kisApiRateLimiter.acquire();  // 중복 #2
    var prices = kisStockPriceService.getDomesticStockDailyPrices(...);
}
```

**문제점**:
- **코드 중복**: 8곳에서 동일한 `acquire()` 호출
- **책임 분산**: API 호출 전 Rate Limiting이 Service 계층에 분산
- **유지보수성**: 새로운 API 호출 메서드 추가 시 acquire() 호출 잊어버릴 위험
- **확장성 부족**: KisWatchlistService 등 다른 Service도 동일한 문제 발생 가능

## 결정

**RateLimiter를 KisRestClient로 이동하여 HTTP 계층에서 중앙 관리**

### 구현 방법

1. **KisRestClient에 RateLimiter 주입**:
   ```java
   @Component
   @RequiredArgsConstructor
   public class KisRestClient {
       private final RestClient kisApiRestClient;
       private final RateLimiter kisApiRateLimiter;

       public <T extends KisApiResponse> T get(...) {
           kisApiRateLimiter.acquire();  // HTTP 호출 전 Rate Limiting
           // RestClient 호출...
       }
   }
   ```

2. **StockBackfillService와 StockPriceCollectorService에서 제거**:
   ```java
   // Before
   private void backfillDomesticStock(...) {
       kisApiRateLimiter.acquire();  // 제거!
       var prices = kisStockPriceService.getDomesticStockDailyPrices(...);
   }

   // After
   private void backfillDomesticStock(...) {
       var prices = kisStockPriceService.getDomesticStockDailyPrices(...);
   }
   ```

## 결과

### 긍정적 영향

- **완전한 중복 제거**: 8곳 → 0곳 (100% 제거!)
- **완전 자동화**: KisRestClient를 사용하는 **모든 Service**가 자동으로 Rate Limiting 적용
  - KisStockPriceService ✅
  - KisWatchlistService ✅
  - KisAuthService ✅
  - 향후 추가될 모든 Service ✅
- **안전성**: 개발자가 Rate Limiting을 잊어버릴 가능성 0%
- **HTTP 계층 제어**: 가장 낮은 레벨에서 제어하므로 누락 불가능
- **유지보수성**: Rate Limiting 로직 변경 시 KisRestClient 한 곳만 수정

### 부정적 영향

- **없음**: 가장 논리적으로 올바른 위치

## 대안

### 대안 1: KisStockPriceService로 이동

```java
@Service
public class KisStockPriceService {
    private final RateLimiter kisApiRateLimiter;

    public List<...> getDomesticStockDailyPrices(...) {
        kisApiRateLimiter.acquire();
        // ...
    }
}
```

- 장점: 중복 일부 감소 (8곳 → 4곳)
- 단점: KisWatchlistService 등 다른 Service는 여전히 수동 관리, 불완전한 해결

### 대안 2: AOP 적용

```java
@Aspect
public class RateLimitAspect {
    @Around("execution(* com.custom.trader.kis.service.*Service.*(..))")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint) { ... }
}
```

- 장점: 자동 적용
- 단점: AOP 복잡성, 포인트컷 관리 복잡

### 대안 3: 현재 상태 유지

- 장점: 변경 없음
- 단점: 중복 유지, 유지보수성 저하

## 채택 이유

1. **완전 자동화**: 모든 KIS API 호출이 자동으로 Rate Limiting 적용
2. **중복 완전 제거**: 8곳 → 0곳 (100% 제거)
3. **HTTP 계층 제어**: RestClient 레벨에서 제어하므로 누락 불가능
4. **최소 변경**: KisRestClient 한 곳만 수정
5. **간단한 구현**: AOP 없이 명시적 호출로 가독성 유지

## 참고

- 관련 파일:
  - `KisRestClient.java` (Rate Limiter 추가)
  - `StockBackfillService.java` (acquire() 제거)
  - `StockPriceCollectorService.java` (acquire() 제거)
- Rate Limiter 설정: 초당 20회 (RateLimiterConfig.java)
- 적용 범위: KisRestClient를 사용하는 모든 Service
