# 0002. 계정별 토큰 갱신 락 세분화

## 상태
Accepted (2026-01-23)

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

### 구현 방법
```java
private final ConcurrentHashMap<String, Object> accountLocks = new ConcurrentHashMap<>();

public String getAccessToken(String account) {
    Object accountLock = accountLocks.computeIfAbsent(account, k -> new Object());

    synchronized(accountLock) {
        // 계정별로 독립적인 락 획득
    }
}
```

- `ConcurrentHashMap`을 사용하여 계정별 락 객체 관리
- `computeIfAbsent()`로 락 객체 생성 시 thread-safe 보장

## 결과

### 긍정적 영향
- **병렬 처리 가능**: 서로 다른 계정의 토큰은 동시에 갱신 가능
- **처리량 향상**: 전체 시스템 throughput 증가
- **확장성 개선**: 계정 추가 시 성능 저하 최소화
- **안전성 유지**: 동일 계정 내에서는 여전히 순차 처리

### 부정적 영향
- **메모리 사용 증가**: 계정마다 락 객체 생성
  - 완화: 계정 수는 제한적(일반적으로 10개 이하)이므로 메모리 부담 미미
- **코드 복잡도 증가**: ConcurrentHashMap 관리 로직 추가

## 대안

### 대안 1: ReentrantLock 사용
```java
private final ConcurrentHashMap<String, ReentrantLock> accountLocks = new ConcurrentHashMap<>();
```
- 장점: tryLock(), timeout 등 고급 기능 사용 가능
- 단점: synchronized보다 복잡, 현재 요구사항에 과도한 설계

### 대안 2: Distributed Lock (Redis)
- 장점: 다중 인스턴스 환경에서도 동작
- 단점: 현재는 단일 인스턴스이므로 불필요, 네트워크 오버헤드

### 대안 3: 토큰 갱신 전용 스케줄러
- 장점: 동시성 이슈 원천 차단
- 단점: 만료 임박 시 즉시 갱신 불가, 실시간성 저하

## 참고
- 커밋: `8b414d5 ⚡ 토큰 갱신 계정별 락 세분화`
- 관련 파일: `KisAuthService.java`
- Thread-safety: `ConcurrentHashMap.computeIfAbsent()`는 Java 8+에서 atomic 보장