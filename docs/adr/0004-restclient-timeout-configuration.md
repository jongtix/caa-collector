# 0004. RestClient 타임아웃 설정

## 상태
Accepted (2026-01-23)

## 컨텍스트

Spring Boot 3.x의 `RestClient`를 사용하여 한국투자증권 KIS API와 통신합니다. 기존에는 타임아웃 설정이 없어 다음과 같은 문제가 발생할 수 있었습니다:

### 문제 상황
- **무한 대기**: API 서버가 응답하지 않을 때 스레드가 무한정 블로킹
- **리소스 고갈**: 다수의 API 호출이 동시에 블로킹되면 스레드 풀 고갈
- **장애 전파**: 외부 API 장애가 내부 시스템으로 전파
- **모니터링 어려움**: 느린 응답과 장애를 구분하기 어려움

## 결정

**RestClient에 읽기 타임아웃(Read Timeout) 설정 적용**

### 구현 방법
```java
@Configuration
public class RestClientConfig {

    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    @Bean
    public RestClient kisApiRestClient(KisProperties kisProperties, RestClient.Builder builder) {
        var requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return builder
                .baseUrl(kisProperties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
```

### 타임아웃 값 선정 근거
- **읽기 타임아웃 30초**: KIS API 응답 시간 고려
  - 실시간 시세 조회: 빠른 응답 (1-2초)
  - 대량 데이터 조회 (예: 일간 차트, 백필): 느린 응답 가능 (최대 20-30초)
  - 여유 있는 타임아웃으로 설정하여 정상 요청의 실패를 방지

### 기술 선택
- **JdkClientHttpRequestFactory 사용**: Java 11+ HttpClient 기반
  - Spring Boot 3.2+에서 권장하는 기본 구현체
  - 경량화되고 성능이 우수함

## 결과

### 긍정적 영향
- **장애 격리**: 외부 API 문제가 전체 시스템에 미치는 영향 제한
- **빠른 실패(Fail-Fast)**: 타임아웃 발생 시 즉시 예외 발생 → 재시도 또는 대체 로직 실행 가능
- **리소스 보호**: 스레드가 무한정 블로킹되지 않음
- **모니터링 개선**: 타임아웃 로그로 API 응답 속도 문제 감지 가능

### 부정적 영향
- **일시적 실패 증가**: 네트워크 지연이나 서버 부하 시 타임아웃으로 실패할 수 있음
  - 완화: 재시도 로직 추가 검토 (향후 ADR)

### 현재 미적용 사항
- **연결 타임아웃(Connect Timeout)**: 현재는 설정되지 않음 (JDK 기본값 사용)
  - JDK HttpClient의 기본 연결 타임아웃은 충분히 짧아 현재는 명시적 설정 불필요
  - 필요 시 향후 추가 가능

## 대안

### 대안 1: SimpleClientHttpRequestFactory 사용
```java
SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
factory.setConnectTimeout(Duration.ofSeconds(5));
factory.setReadTimeout(Duration.ofSeconds(30));
```
- 장점: Connect Timeout도 함께 설정 가능
- 단점: Spring Boot 3.2+에서는 JdkClientHttpRequestFactory가 권장됨

### 대안 2: WebClient 사용
```java
WebClient webClient = WebClient.builder()
    .baseUrl(baseUrl)
    .clientConnector(new ReactorClientHttpConnector(
        HttpClient.create()
            .responseTimeout(Duration.ofSeconds(30))
    ))
    .build();
```
- 장점: Non-blocking I/O, 반응형 프로그래밍
- 단점:
  - 현재 프로젝트는 동기식 처리 (Spring Data JPA 사용)
  - 학습 곡선 높음
  - WebFlux 의존성 추가 필요

### 대안 3: Apache HttpClient
```java
RequestConfig config = RequestConfig.custom()
    .setConnectTimeout(5000)
    .setResponseTimeout(30000)
    .build();
```
- 장점: 세밀한 설정 가능, 연결 풀 관리 등 고급 기능
- 단점: 외부 라이브러리 의존성, Spring 표준이 아님

## 향후 고려사항

1. **연결 타임아웃 명시적 설정**: 필요 시 Connect Timeout 추가
2. **재시도 정책**: 타임아웃 발생 시 자동 재시도 (Exponential Backoff)
3. **동적 타임아웃**: API 엔드포인트별로 다른 타임아웃 값 적용
4. **모니터링**: 타임아웃 발생률 메트릭 수집

## 참고
- 커밋: `6d29eaa 🔧 RestClient 타임아웃 설정 추가`
- 관련 파일: `RestClientConfig.java:16-21`
- Spring Boot 문서: [RestClient Configuration](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient)