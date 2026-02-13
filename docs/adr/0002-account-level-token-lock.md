# 0002. 계정별 토큰 갱신 락 세분화

## 상태
Amended (2026-02-13, 최초 Accepted: 2026-01-23)

## 컨텍스트

기존 `KisAuthService`에서 **전역 락(Global Lock)** 을 사용하여 토큰 갱신 동시성을 제어했습니다.

### 문제 상황
```java
// 기존 코드 (의사 코드)
private final Object tokenLock = new Object();

synchronized(tokenLock) {
    // 모든 계정의 토큰 갱신이 순차 처리됨
}
```

- **병목 현상**: 계정 A의 토큰 갱신 중에는 계정 B도 대기해야 함
- **확장성 저하**: 계정이 늘어날수록 대기 시간 증가
- **불필요한 블로킹**: 서로 다른 계정 간에는 동시성 제어가 불필요함

## 결정

**계정별 락(Account-Level Lock)을 사용하여 독립적인 토큰 갱신 허용**

### 구현 방법 (2026-02-13 현재)

**ReentrantLock + Double-Checked Locking 패턴 사용**

```java
private final ConcurrentHashMap<String, ReentrantLock> accountLocks = new ConcurrentHashMap<>();

public String getAccessToken(String accountNameOrNumber) {
    KisAccountProperties account = findAccount(accountNameOrNumber);
    String cacheKey = REDIS_KEY_PREFIX + redisKeyHasher.hash(account.accountNumber());

    // 1차 체크: 락 없이 캐시 확인 (Fast Path)
    String encryptedToken = redisTemplate.opsForValue().get(cacheKey);
    if (encryptedToken != null) {
        return tokenEncryptor.decrypt(encryptedToken);
    }

    // 2차 체크: 락 획득 후 다시 캐시 확인 (Slow Path)
    return refreshToken(account);
}

private String refreshToken(KisAccountProperties account) {
    String hashedAccountNumber = redisKeyHasher.hash(account.accountNumber());
    String cacheKey = REDIS_KEY_PREFIX + hashedAccountNumber;

    ReentrantLock lock = accountLocks.computeIfAbsent(
            hashedAccountNumber,
            k -> new ReentrantLock()
    );

    lock.lock();
    try {
        // Double-Check: 락 대기 중 다른 스레드가 갱신했을 수 있음
        String encryptedToken = redisTemplate.opsForValue().get(cacheKey);
        if (encryptedToken != null) {
            return tokenEncryptor.decrypt(encryptedToken);
        }

        // API 호출 및 캐시 저장
        // ...
    } finally {
        lock.unlock();
    }
}
```

### 설계 포인트
- `ConcurrentHashMap<String, ReentrantLock>`으로 계정별 락 관리
- `computeIfAbsent()`로 락 객체 생성 시 thread-safe 보장
- **Double-Checked Locking**: 캐시 히트 시 락 획득 없이 즉시 반환
- `ReentrantLock` 사용으로 향후 확장 가능성 확보 (tryLock, timeout 등)

## 결과

### 긍정적 영향
- **병렬 처리 가능**: 서로 다른 계정의 토큰은 동시에 갱신 가능
- **처리량 향상**: 전체 시스템 throughput 증가
- **확장성 개선**: 계정 추가 시 성능 저하 최소화
- **안전성 유지**: 동일 계정 내에서는 여전히 순차 처리
- **캐시 히트 최적화**: Double-Checked Locking으로 불필요한 락 획득 방지
- **고급 기능 확보**: ReentrantLock 사용으로 향후 timeout, tryLock 등 활용 가능

### 부정적 영향
- **메모리 사용 증가**: 계정마다 ReentrantLock 객체 생성
  - 완화: 계정 수는 제한적(일반적으로 10개 이하)이므로 메모리 부담 미미
- **코드 복잡도 증가**: ConcurrentHashMap + Double-Checked Locking 패턴으로 복잡도 상승
  - 완화: 동시성 테스트로 안정성 검증

### ReentrantLock 채택 이유 (vs synchronized)
- **명시적 락 제어**: lock()/unlock()으로 락 범위 명확화
- **예외 안전성**: try-finally 블록으로 락 해제 보장
- **확장성**: 향후 `tryLock(timeout)` 등 고급 기능 필요 시 쉽게 추가 가능
- **가독성**: synchronized보다 락의 의도가 명확히 드러남

## 대안

### 대안 1: ReentrantLock 사용 ✅ **채택됨 (2026-01-18)**
```java
private final ConcurrentHashMap<String, ReentrantLock> accountLocks = new ConcurrentHashMap<>();
```
- 장점: tryLock(), timeout 등 고급 기능 사용 가능
- 단점: synchronized보다 복잡
- **채택 이유**: 향후 확장성과 명시적 락 제어 필요성 확인

### 대안 2: Distributed Lock (Redis)
- 장점: 다중 인스턴스 환경에서도 동작
- 단점: 현재는 단일 인스턴스이므로 불필요, 네트워크 오버헤드

### 대안 3: 토큰 갱신 전용 스케줄러
- 장점: 동시성 이슈 원천 차단
- 단점: 만료 임박 시 즉시 갱신 불가, 실시간성 저하

## 테스트 전략

### 동시성 테스트 필수

Lock 메커니즘의 정확성을 검증하기 위해 다음 시나리오를 테스트해야 합니다:

1. **동일 계정 동시 요청**
   - 멀티 스레드에서 동일 계정 토큰 동시 요청
   - API 호출이 1회만 발생하는지 검증
   - 모든 스레드가 동일한 토큰을 받는지 확인

2. **서로 다른 계정 병렬 처리**
   - 계정 A, B를 동시에 요청
   - 두 계정의 처리가 독립적으로 진행되는지 검증
   - 한 계정의 락이 다른 계정을 블로킹하지 않는지 확인

3. **Lock 경쟁 상황 (Race Condition)**
   - Double-Checked Locking이 올바르게 동작하는지 검증
   - Lock 획득/해제가 정상적으로 수행되는지 확인

4. **캐시 히트 시나리오**
   - 캐시에 토큰이 있을 때 Lock 획득 없이 반환되는지 확인
   - 성능 최적화가 올바르게 동작하는지 검증

### 테스트 도구

```java
// ExecutorService로 멀티 스레드 시뮬레이션
ExecutorService executor = Executors.newFixedThreadPool(10);

// CountDownLatch로 동시 시작 보장
CountDownLatch startLatch = new CountDownLatch(1);
CountDownLatch doneLatch = new CountDownLatch(threadCount);

// Mockito로 API 호출 횟수 검증
verify(kisRestClient, times(1)).post();
```

### 관련 테스트 파일
- `KisAuthServiceTest.java` - 동시성 테스트 케이스 포함 (MA-01 이슈 해결)

## 변경 이력

### 2026-02-13: ReentrantLock 채택 및 테스트 전략 추가

**Status**: Accepted → Amended

**변경 내용**:
- `synchronized(Object)` 대신 `ReentrantLock` 사용으로 변경
- Double-Checked Locking 패턴 구현
- 동시성 테스트 전략 문서화

**이유**:
- tryLock(), timeout 등 고급 기능 필요성 확인
- MA-01 이슈 검증 과정에서 동시성 테스트 누락 발견
- 명시적 락 제어로 코드 가독성 향상

**영향**:
- 기능적 동작은 동일하나 코드 복잡도 증가
- 테스트 추가로 안정성 향상
- 향후 확장 가능성 확보

**관련 이슈**:
- MA-01: 동시성 테스트 부재 (branch-review 리포트 Major 이슈)

## 참고
- 최초 커밋: `8b414d5 ⚡ 토큰 갱신 계정별 락 세분화` (2026-01-23)
- ReentrantLock 구현: `6483b28 ⚡ 토큰 갱신 계정별 락 세분화` (2026-01-18)
- 관련 파일: `src/main/java/com/custom/trader/kis/service/KisAuthService.java` (Line 44, 95-135)
- Thread-safety: `ConcurrentHashMap.computeIfAbsent()`는 Java 8+에서 atomic 보장
- Double-Checked Locking: [Java Concurrency in Practice, Goetz et al.]