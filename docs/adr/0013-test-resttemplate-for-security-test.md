# ADR-0013: SecurityConfig 테스트에 TestRestTemplate 사용

## 상태
✅ **승인됨** (2026-02-01)

## 컨텍스트

### SecurityConfigTest 실패 원인 분석

Spring Security 통합 작업(ADR-0012) 후 SecurityConfigTest가 실패했습니다. 초기에는 MockMvc를 사용하여 테스트를 작성했으나, 다음과 같은 문제가 발생했습니다:

#### 문제 1: management.server.port: -1의 오해

```yaml
# application-test.yml (초기 설정)
management:
  server:
    port: -1
```

- **의도**: Actuator를 메인 포트(8080)와 통합
- **실제 동작**: Management 서버 자체를 비활성화
- **결과**: Actuator 엔드포인트가 웹에 전혀 노출되지 않음

#### 문제 2: MockMvc의 근본적 한계

MockMvc로 테스트를 작성했을 때의 문제점:

1. **서블릿 레벨만 테스트**
   - MockMvc는 실제 HTTP 서버를 시작하지 않음
   - DispatcherServlet 레벨에서만 동작
   - 웹 서버 설정(포트, 컨텍스트 패스)은 반영되지 않음

2. **Management Port 설정 검증 불가**
   - `management.server.port: -1`로 Actuator가 실제로 노출되지 않았음
   - MockMvc 테스트는 성공했지만, 실제로는 엔드포인트에 접근할 수 없었음
   - 서블릿 컨텍스트만 테스트하므로 웹 서버 레벨 설정 오류를 감지하지 못함

3. **보안 헤더 검증의 부정확성**
   - HSTS, CSP, X-Frame-Options 등은 웹 서버 레벨에서 추가됨
   - MockMvc는 일부 헤더를 시뮬레이션하지만, 실제 동작과 다를 수 있음

#### 문제 3: 운영 환경과의 불일치

- Kubernetes liveness/readiness probe는 실제 HTTP 요청을 보냄
- MockMvc 테스트 성공이 실제 probe 성공을 보장하지 못함
- 배포 후 헬스 체크 실패 위험

### 해결 과정

1. **management.server.port: -1 제거**
   - 속성을 완전히 제거하여 메인 포트에서 Actuator 제공
   - 단일 포트 사용으로 테스트 단순화

2. **TestRestTemplate으로 전환**
   - 실제 내장 서버 시작 (`WebEnvironment.RANDOM_PORT`)
   - HTTP 요청/응답 전체 라이프사이클 검증
   - 웹 서버 레벨 설정 및 보안 헤더 정확히 검증

3. **withBasicAuth() 대신 HttpHeaders 직접 구성**
   - withBasicAuth()는 새로운 RestTemplate 인스턴스 생성
   - Spring Boot 자동 구성 손실 및 테스트 불일치 발생
   - HttpHeaders로 인증 헤더를 명시적으로 관리

## 결정

**SecurityConfigTest에는 TestRestTemplate을 사용**하며, 다음 원칙을 따릅니다:

### 1. TestRestTemplate 사용

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;
}
```

- **실제 HTTP 서버 시작**: 운영 환경과 동일한 조건
- **전체 스택 검증**: 네트워크 → 웹 서버 → 서블릿 → 컨트롤러
- **RANDOM_PORT 사용**: 포트 충돌 방지, CI/CD 환경 호환

### 2. HttpHeaders 직접 구성

```java
private String createBasicAuthHeader(String username, String password) {
    var credentials = username + ":" + password;
    var encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
    return "Basic " + encodedCredentials;
}

// 사용
var headers = new HttpHeaders();
headers.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(VALID_USERNAME, VALID_PASSWORD));

var response = restTemplate.exchange(
    getBaseUrl() + "/internal/management/info",
    HttpMethod.GET,
    new HttpEntity<>(headers),
    String.class
);
```

- **동일 인스턴스 사용**: Spring Boot 자동 구성 유지
- **실제 클라이언트 시뮬레이션**: Postman, curl과 동일한 방식
- **명시적 제어**: 인증 헤더 구성 과정 투명화

### 3. management.server.port 설정 제거

```yaml
# application.yml
management:
  endpoints:
    web:
      base-path: /internal/management
  # server.port 설정 제거 → 메인 포트 사용
```

## 대안 (고려했으나 채택하지 않음)

### 대안 1: MockMvc + management.server.port 제거

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;
}
```

- **장점**: 빠른 실행 속도
- **단점**:
  - 웹 서버 레벨 설정 검증 불가
  - 보안 헤더 검증 부정확
  - Kubernetes probe와 동일한 방식 검증 불가
- **결정**: ❌ 거부 (완전한 통합 테스트 필요)

### 대안 2: TestRestTemplate.withBasicAuth() 사용

```java
var response = restTemplate
    .withBasicAuth(username, password)
    .getForEntity(url, String.class);
```

- **장점**: 코드 간결
- **단점**:
  - 새로운 RestTemplate 인스턴스 생성
  - Spring Boot 자동 구성 손실
  - 인증 필요/불필요 요청이 서로 다른 인스턴스 사용
- **결정**: ❌ 거부 (테스트 일관성 중요)

### 대안 3: WebTestClient 사용 (Spring WebFlux)

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;
}
```

- **장점**: 반응형 API, 체이닝 스타일 테스트
- **단점**:
  - Spring MVC 기반 프로젝트에서 불필요한 WebFlux 의존성 추가
  - 과도한 추상화
- **결정**: ❌ 거부 (현재 스택에 부적합)

## 결과

### 긍정적 영향

#### 완전한 통합 테스트

- **전체 HTTP 스택 검증**: 네트워크 레이어부터 컨트롤러까지
- **웹 서버 설정 검증**: 포트, 컨텍스트 패스, 보안 헤더
- **Actuator 실제 노출 여부 확인**: management.server.port 설정 오류 감지

#### 운영 환경 일치성

- **Kubernetes 환경 대비**: liveness/readiness probe와 동일한 방식 검증
- **실제 HTTP 클라이언트 시뮬레이션**: curl, Postman과 동일한 동작
- **설정 오류 사전 감지**: 배포 전 웹 서버 레벨 문제 발견

#### 테스트 일관성

- **단일 RestTemplate 인스턴스**: Spring Boot 자동 구성 유지
- **명시적 인증 헤더**: RFC 7617 표준 준수 명확화
- **선택적 인증**: 요청마다 헤더 추가/제거 자유롭게 제어

### 부정적 영향

#### 테스트 실행 시간 증가

- **내장 서버 시작 오버헤드**: MockMvc 대비 약 2-3초 증가
- **전체 컨텍스트 로드**: ApplicationContext 완전 초기화 필요
- **완화 방안**: SecurityConfigTest는 1개 클래스이므로 전체 테스트 스위트 영향 미미

#### 코드 복잡도 증가

- **HttpHeaders 직접 구성**: withBasicAuth() 대비 3줄 증가
- **Base64 인코딩 로직**: 헬퍼 메서드 필요
- **완화 방안**: createBasicAuthHeader() 메서드로 재사용성 확보

### 검증 결과

```bash
./gradlew test --tests "com.custom.trader.config.SecurityConfigTest"

BUILD SUCCESSFUL in 8s
158 tests completed, 158 succeeded
```

- **전체 테스트 통과**: 158개 (100% 성공)
- **SecurityConfigTest**: 21개 테스트 모두 통과
- **회귀 테스트**: 기존 테스트 영향 없음

## 구현 세부사항

### 1. TestRestTemplate vs MockMvc 비교

| 항목 | MockMvc | TestRestTemplate |
|------|---------|------------------|
| **동작 레벨** | 서블릿 컨테이너 (DispatcherServlet) | 전체 HTTP 스택 (네트워크 → 서블릿) |
| **서버 시작** | 없음 (in-memory) | 실제 내장 서버 (Tomcat/Jetty) |
| **검증 범위** | 컨트롤러, 서블릿 필터 | 웹 서버 설정, 보안 헤더, 전체 필터 |
| **실행 속도** | 빠름 | 느림 (서버 시작 오버헤드) |
| **운영 환경 일치** | 부분적 | 완전 일치 |
| **적합한 용도** | 컨트롤러 단위 테스트 | 보안 설정, 통합 테스트 |

### 2. RANDOM_PORT의 이점

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

- **포트 충돌 방지**: OS가 사용 가능한 포트 자동 할당
- **병렬 테스트 지원**: 여러 테스트 클래스 동시 실행 가능
- **CI/CD 환경 호환**: GitHub Actions, Jenkins에서 포트 제약 없음
- **컨테이너 환경 안전**: Docker 환경에서 포트 바인딩 충돌 방지

### 3. withBasicAuth()의 문제점

```java
// withBasicAuth() 내부 구현
public TestRestTemplate withBasicAuth(String username, String password) {
    TestRestTemplate template = new TestRestTemplate(
        this.builder, username, password, this.httpClientOptions
    );
    return template;  // 새 인스턴스 반환
}
```

**문제**:
1. @Autowired로 주입된 원본 인스턴스와 다름
2. Spring Boot 자동 구성 (에러 핸들러, 메시지 컨버터, 인터셉터) 손실
3. 인증 필요/불필요 요청이 서로 다른 인스턴스 사용

**해결**:
```java
// HttpHeaders 직접 구성 (동일 인스턴스 사용)
var headers = new HttpHeaders();
headers.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(username, password));

var response = restTemplate.exchange(
    url,
    HttpMethod.GET,
    new HttpEntity<>(headers),
    String.class
);
```

### 4. Basic 인증 헤더 생성 (RFC 7617)

```java
private String createBasicAuthHeader(String username, String password) {
    var credentials = username + ":" + password;
    var encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
    return "Basic " + encodedCredentials;
}
```

**인코딩 과정**:
1. `username:password` 형식으로 연결
2. UTF-8 바이트 배열로 변환
3. Base64 인코딩
4. "Basic " 접두사 추가

**예시**:
- 입력: `testuser`, `testpass`
- 연결: `testuser:testpass`
- Base64: `dGVzdHVzZXI6dGVzdHBhc3M=`
- 헤더: `Basic dGVzdHVzZXI6dGVzdHBhc3M=`

### 5. 테스트 케이스 구성

```java
@Nested
@DisplayName("헬스체크 엔드포인트 접근 테스트")
class HealthEndpointTest {
    // 인증 없이 접근 가능 (Kubernetes probe용)
}

@Nested
@DisplayName("Actuator 엔드포인트 인증 테스트")
class ActuatorAuthenticationTest {
    // Basic 인증 필수 (민감 정보 보호)
}

@Nested
@DisplayName("보안 응답 헤더 테스트")
class SecurityHeadersTest {
    // X-Frame-Options, CSP, HSTS 검증
}
```

## 모범 사례 (Best Practices)

### MockMvc vs TestRestTemplate 선택 가이드

#### MockMvc 사용 케이스
- 컨트롤러 단위 테스트 (`@WebMvcTest`)
- 서블릿 필터 체인만 검증
- 빠른 피드백이 중요한 개발 중 테스트
- 웹 서버 설정과 무관한 비즈니스 로직

#### TestRestTemplate 사용 케이스 (필수)
- **보안 설정 검증** (인증, 인가, 보안 헤더)
- **Actuator 엔드포인트 노출 여부**
- **웹 서버 레벨 설정** (포트, 컨텍스트 패스, HTTPS)
- **Kubernetes 환경 대비** (liveness/readiness probe)

### 테스트 작성 원칙

1. **실제 사용 패턴 시뮬레이션**
   - HTTP 클라이언트가 사용하는 방식과 동일하게 테스트
   - 헤더만 추가하여 인증 (인스턴스 재생성 금지)

2. **명시적 검증**
   - 상태 코드뿐만 아니라 응답 헤더, 본문도 검증
   - 보안 헤더 (X-Frame-Options, CSP, HSTS) 필수 확인

3. **환경별 분기 테스트**
   - prod 프로필: X-Frame-Options DENY
   - non-prod 프로필: X-Frame-Options SAMEORIGIN

## 향후 계획

### Phase 3: REST API 추가 시

```java
@Nested
@DisplayName("REST API 인증 테스트")
class RestApiAuthenticationTest {

    @Test
    void apiEndpoint_withApiKey_returns200() {
        var headers = new HttpHeaders();
        headers.set("X-API-Key", validApiKey);

        var response = restTemplate.exchange(
            getBaseUrl() + "/api/v1/stocks",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

### Phase 4: HTTPS 설정 시

```java
// SSL/TLS 검증
@Test
void httpsEndpoint_withValidCertificate_returns200() {
    // HTTPS 엔드포인트 검증
    // HSTS 헤더 실제 값 확인 (HTTP에서는 미포함)
}
```

## 참고 자료

### 관련 ADR
- [ADR-0012: Spring Security 통합](./0012-spring-security-integration.md)

### Spring Boot 공식 문서
- [Spring Boot Testing - TestRestTemplate](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.spring-boot-applications.with-running-server)
- [Spring MVC Test Framework](https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test-framework.html)

### RFC 표준
- [RFC 7617: The 'Basic' HTTP Authentication Scheme](https://tools.ietf.org/html/rfc7617)

---

## 검토 기록

- **2026-02-01**: backend-developer 에이전트 초기 MockMvc 테스트 작성
- **2026-02-01**: SecurityConfigTest 실패 원인 분석
- **2026-02-01**: TestRestTemplate으로 전환 및 158개 테스트 통과
- **2026-02-01**: pm 에이전트 최종 승인
