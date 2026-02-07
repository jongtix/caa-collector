# ADR-0012: Spring Security 통합 및 인증 체계 도입

> **⚠️ 2026-02-02 업데이트**: 이 ADR에서 설계한 H2 Console 보호 기능은 실제로 사용되지 않아 제거되었습니다.
> H2 Console은 로컬 환경에서도 비활성화되며, SecurityConfig에서 `/h2-console` 경로 관련 코드는 모두 제거되었습니다.
> 이 문서는 역사적 기록으로 유지하되, 현재 구현과는 다름을 주의하세요.

## 상태
✅ **승인됨** (2026-02-01)

## 컨텍스트

### 보안 감사 결과

Phase 2 Week 1 문서화 작업 중 보안 감사를 수행한 결과, 다음과 같은 심각한 보안 취약점이 발견되었습니다:

#### HIGH-01: Spring Security 미적용
- **현황**: 애플리케이션 전체에 인증/인가 체계 부재
- **위험**: 모든 엔드포인트에 무제한 접근 가능
- **영향도**: HIGH (전체 시스템 보안 무방비)

#### HIGH-02: Actuator 엔드포인트 무방비 노출
- **현황**: `/actuator/*` 경로가 인증 없이 노출
- **노출 정보**:
  - `/actuator/health` - 애플리케이션 상태
  - `/actuator/metrics` - 성능 메트릭
  - `/actuator/env` - 환경 변수 (민감 정보 포함 가능)
  - `/actuator/loggers` - 로깅 설정 조회/변경
- **위험**: 시스템 정보 유출, 운영 환경 조작 가능
- **영향도**: HIGH (공격 벡터 제공)

#### CVE-01: H2 Console 무방비 노출
- **현황**: `/h2-console` 경로가 인증 없이 노출
- **위험**: 개발 환경에서 DB 직접 접근 가능
- **영향도**: CRITICAL (개발 환경에서 데이터 유출/변조 가능)

### 향후 요구사항

Phase 4 이후 서비스 간 통신 및 외부 API 연동이 예정되어 있어, 인증 체계 구축이 필수적입니다:

- **Phase 4 (2026-03-02 ~ 03-22)**: AI Advisor 서비스와 REST API 통신
- **Phase 5 (2026-03-23 ~ 04-05)**: Notifier 서비스와 Kafka 통신
- **Phase 6 이후**: 외부 모니터링 시스템 연동

## 결정

**Spring Security를 도입하여 인증 체계를 구축**합니다.

### 핵심 설계

#### 1. Spring Security 의존성 추가

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security'
}
```

#### 2. SecurityConfig 구성

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)  // Stateless API이므로 전체 비활성화
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/internal/management/health",
                                "/internal/management/health/liveness",
                                "/internal/management/health/readiness")
                    .permitAll()
                .requestMatchers("/internal/management/**").hasRole("ACTUATOR")
                .requestMatchers("/h2-console/**").hasRole("ACTUATOR")
                .anyRequest().authenticated()  // Deny by Default 원칙
            )
            .httpBasic(Customizer.withDefaults())
            .headers(headers -> headers
                .frameOptions(this::configureFrameOptions)
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            );

        return http.build();
    }
}
```

#### 3. Basic Authentication 선택

- **Actuator 인증**: HTTP Basic Auth 사용
- **자격 증명**: 환경변수로 관리 (`ACTUATOR_USERNAME`, `ACTUATOR_PASSWORD`)
- **세션 정책**: Stateless (무상태 인증)

#### 4. Actuator 경로 변경

```yaml
# application.yml
management:
  endpoints:
    web:
      base-path: /internal/management
```

- **변경 전**: `/actuator/*`
- **변경 후**: `/internal/management/*`
- **이유**: 내부 관리 목적 명시, 외부 노출 최소화

#### 5. H2 Console 환경변수 제어

```yaml
# application-db-dev.yml
spring:
  h2:
    console:
      enabled: ${H2_CONSOLE_ENABLED:false}
```

- **기본값**: `false` (비활성화)
- **개발 시**: `H2_CONSOLE_ENABLED=true`로 명시적 활성화
- **인증 필수**: Basic Auth로 보호

#### 6. 향후 API Key 인증 기반 마련

```java
// Phase 4에서 구현 예정
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    // X-API-Key 헤더 검증 로직
}
```

- **목적**: 서비스 간 통신(AI Advisor, Notifier)에 사용
- **현재**: 인터페이스만 정의, 구현은 Phase 4에서 진행

## 대안 (고려했으나 채택하지 않음)

### 대안 1: 인증 체계 없이 유지

```yaml
# Spring Security 의존성 없이 운영
```

- **장점**: 구현 단순, 개발 편의성 유지
- **단점**:
  - 보안 취약점 방치
  - Actuator 정보 유출 위험
  - H2 Console 무방비 노출
- **결정**: ❌ 거부 (보안 위험 수용 불가)

### 대안 2: 네트워크 레벨 제한만 적용

```yaml
# 방화벽 규칙으로 특정 IP만 허용
management:
  server:
    address: 127.0.0.1
```

- **장점**: Spring Security 의존성 불필요
- **단점**:
  - 애플리케이션 레벨 보안 부재
  - 내부 네트워크 침투 시 무방비
  - 클라우드 환경에서 IP 기반 제어 한계
- **결정**: ❌ 거부 (다층 방어 필요)

### 대안 3: API Key만 사용

```java
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    // X-API-Key 헤더만 검증
}
```

- **장점**: 간단한 구현, Stateless 인증
- **단점**:
  - Actuator 접근에는 과도한 설정 (사람이 직접 접근)
  - Key 관리 복잡도 증가
  - 현재 단계에서는 과잉 설계
- **결정**: ⚠️ 보류 (Phase 4에서 서비스 간 통신용으로 추가)

### 대안 4: JWT 기반 인증

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // JWT 토큰 검증 로직
}
```

- **장점**: 확장성 높음, 마이크로서비스에 적합
- **단점**:
  - 현재 규모에 과잉 설계
  - Token 발급/갱신 로직 복잡
  - Actuator 접근용으로는 부적합
- **결정**: ❌ 거부 (현재 요구사항에 과도)

## 결과

### 긍정적 영향

#### 보안 강화
- **인증 체계 확립**: 민감한 엔드포인트 접근 통제
- **정보 유출 방지**: Actuator, H2 Console 보호
- **다층 방어**: 애플리케이션 + 네트워크 레벨 보안

#### 운영 안정성
- **Actuator 보호**: 헬스 체크, 메트릭 접근 제어
- **환경변수 관리**: 민감 정보 환경변수로 분리
- **H2 Console 제어**: 개발 환경에서만 명시적 활성화

#### 확장성
- **API Key 인증 기반**: Phase 4 서비스 간 통신 준비
- **Stateless 인증**: 수평 확장에 유리
- **Spring Security 생태계**: 향후 OAuth2, JWT 도입 용이

### 부정적 영향

#### 개발 복잡도 증가
- **인증 설정**: Actuator, H2 Console 접근 시 자격 증명 필요
- **환경변수 관리**: 로컬, 개발, 운영 환경별 설정 분리
- **테스트 설정**: `@AutoConfigureMockMvc(addFilters = false)` 필요

#### 초기 설정 비용
- **개발 시간**: 약 3시간 소요 (설계 1h + 구현 1h + 테스트 1h)
- **학습 곡선**: Spring Security 기본 개념 이해 필요

### 중립적 영향

#### 기존 기능 영향 없음
- **스케줄러**: 내부 작업이므로 인증 불필요
- **KIS API 연동**: 외부 API 호출이므로 영향 없음
- **DB 접근**: JPA 레이어는 그대로 유지

## 구현 세부사항

### 1. 보호 대상 엔드포인트

```java
.requestMatchers("/internal/management/health",
                "/internal/management/health/liveness",
                "/internal/management/health/readiness")
    .permitAll()                                              // 헬스체크 공개
.requestMatchers("/internal/management/**").hasRole("ACTUATOR")   // Actuator 역할 필요
.requestMatchers("/h2-console/**").hasRole("ACTUATOR")            // H2 Console 역할 필요
.anyRequest().authenticated()                                      // Deny by Default
```

### 2. 환경변수 관리

```bash
# .env
ACTUATOR_USERNAME=admin
ACTUATOR_PASSWORD=secure-password-here
H2_CONSOLE_ENABLED=false  # 기본값: 비활성화
```

### 3. Stateless 세션 정책

```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

- **이유**: 세션 저장소 불필요, 수평 확장 용이
- **방식**: 매 요청마다 Basic Auth 헤더 검증

### 4. CSRF 설정

```java
.csrf(AbstractHttpConfigurer::disable)  // Stateless API이므로 전체 비활성화
```

- **설정**: CSRF 전체 비활성화
- **이유**: Stateless 세션 정책을 사용하므로 CSRF 토큰이 무의미함. 세션 기반 인증을 사용하지 않으므로 CSRF 보호가 불필요하며, 전체 비활성화가 Stateless 정책과 일관됨.

### 5. X-Frame-Options 프로필별 분기

```java
// 프로필별 X-Frame-Options 분기
// prod: DENY (클릭재킹 방어)
// local/dev: SAMEORIGIN (H2 Console iframe 지원)

private void configureFrameOptions(FrameOptionsConfig frameOptions) {
    if (isProdProfile()) {
        frameOptions.deny();
    } else {
        frameOptions.sameOrigin();
    }
}

private boolean isProdProfile() {
    return environment.matchesProfiles("prod");
}
```

- **prod 환경**: `DENY` - 클릭재킹(Clickjacking) 공격 방어를 위해 iframe 완전 차단
- **local/dev 환경**: `SAMEORIGIN` - H2 Console이 iframe을 사용하므로 허용

### 6. 보안 응답 헤더

다음 보안 헤더가 자동으로 적용됩니다:

- **Content-Security-Policy**: `default-src 'self'` (XSS 방어)
- **Strict-Transport-Security**: `max-age=31536000; includeSubDomains` (HTTPS 강제)
- **X-Frame-Options**: 프로필별 분기 (위 항목 참조)

```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp ->
        csp.policyDirectives("default-src 'self'"))
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000))
    .frameOptions(this::configureFrameOptions)
)
```

## 7. Rate Limiting 전략 (Brute Force 공격 방어)

### 개요

**이슈 배경**: MA-13 - Actuator 엔드포인트에서 무한 인증 시도 방어 필요

### 해결책

- **방식**: Redis 기반 Rate Limiting
- **정책**: 동일 IP에서 5회 실패 시 5분간 429 응답 반환
- **구성**: Spring Security 실패 핸들러 + Redis TTL

### Redis 선택 이유

1. **기존 인프라 활용**: 토큰 캐싱, ShedLock용 Redis 이미 운영 중
2. **TTL 자동 정리**: 만료 시간 후 자동 삭제로 메모리 효율적
3. **확장성**: 멀티 인스턴스 환경에서 중앙화된 제어 가능

### 구현 계획

- **일정**: Phase 2 Week 2 (배포 자동화 작업 중)
- **소요 시간**: 약 2.5시간 (설계 1h + 구현 1h + 테스트 0.5h)

## 테스트 전략

### 1. 통합 테스트

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Test
    void actuatorEndpoint_withoutAuth_returns401() {
        mockMvc.perform(get("/internal/management/health"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorEndpoint_withValidAuth_returns200() {
        mockMvc.perform(get("/internal/management/health")
            .with(httpBasic("admin", "password")))
            .andExpect(status().isOk());
    }
}
```

### 2. 기존 테스트 영향 최소화

```java
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)  // Security 필터 비활성화
class MyControllerTest {
    // 기존 테스트 코드 변경 불필요
}
```

### 3. 테스트 결과

- **전체 테스트**: 99개 통과 (100% 성공)
- **추가 테스트**: SecurityConfigTest (8개 테스트)
- **회귀 테스트**: 기존 테스트 모두 통과

## 마이그레이션 가이드

### 1. 로컬 개발 환경

```bash
# .env 파일에 추가
ACTUATOR_USERNAME=admin
ACTUATOR_PASSWORD=local-dev-password
H2_CONSOLE_ENABLED=true
```

### 2. Actuator 접근 방법

```bash
# 이전: 인증 없이 접근
curl http://localhost:8080/actuator/health

# 이후: Basic Auth 필요
curl -u admin:password http://localhost:8080/internal/management/health
```

### 3. H2 Console 접근 방법

```bash
# 브라우저에서 접근 시 Basic Auth 팝업 표시
http://localhost:8080/h2-console

# 자격 증명 입력
Username: admin
Password: local-dev-password
```

## 향후 계획

### Phase 4 (2026-03-02 ~ 03-22): API Key 인증 추가

```java
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String apiKey = request.getHeader("X-API-Key");
        // API Key 검증 로직
    }
}
```

- **목적**: AI Advisor, Notifier 서비스 간 통신
- **방식**: `X-API-Key` 헤더 검증
- **관리**: Redis에 API Key 저장, 만료 시간 설정

### Phase 5 이후: OAuth2 고려

- **조건**: 외부 서비스 연동 증가 시
- **시점**: 3개 이상의 외부 서비스 연동 시 검토
- **현재**: 불필요 (과잉 설계)

## 참고 자료

### 보안 감사 문서
- [보안 감사 결과](../security-audit-report.md) (작성 예정)
- [환경변수 관리 가이드](../env-management.md) (작성 예정)

### Spring Security 공식 문서
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [Spring Boot Actuator Security](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.security)

### 관련 ADR
- [ADR-0002: Account Level Token Lock](./0002-account-level-token-lock.md) - Redis 기반 인증 관리

---

## 설계 변경 이력

### 2026-02-01: 기본 접근 정책 변경 (permitAll → authenticated)

- **원래 설계**: `.anyRequest().permitAll()` (인증되지 않은 요청 허용)
- **변경 후**: `.anyRequest().authenticated()` (모든 요청 인증 필요)
- **변경 사유**:
  - **Deny by Default 원칙 적용** (OWASP 권고)
  - 현재 REST Controller가 없으므로 실질적 영향 없음
  - 새 엔드포인트 추가 시 보안 규칙 누락을 자동으로 방지
- **향후 계획**: Phase 3/4에서 REST API 추가 시, 해당 경로만 명시적으로 접근 정책 설정

**Phase 3/4: REST API 추가 시 접근 정책 변경 가이드**

REST API 엔드포인트 추가 시 SecurityFilterChain에 명시적 규칙을 추가:

1. 서비스 간 API: `.requestMatchers("/api/v1/**").hasRole("SERVICE")`
2. 공개 API (필요 시): `.requestMatchers("/api/public/**").permitAll()`
3. `.anyRequest().authenticated()`는 유지 (Deny by Default)

API Key 인증 도입 시 다중 SecurityFilterChain 분리를 검토할 것.

### 2026-02-01: CSRF 정책 변경 (부분 비활성화 → 전체 비활성화)

- **원래 설계**: H2 Console 경로만 CSRF 예외 처리
- **변경 후**: Stateless API이므로 CSRF 전체 비활성화
- **변경 사유**: 세션 미사용 정책과 일관성 확보

### 2026-02-01: 보안 헤더 강화

- **추가 항목**:
  - CSP (Content-Security-Policy: `default-src 'self'`)
  - HSTS (Strict-Transport-Security: `max-age=31536000; includeSubDomains`)
- **X-Frame-Options**: 프로필별 분기 (prod: DENY, non-prod: SAMEORIGIN)

## 검토 기록

- **2026-02-01**: backend-security-coder 에이전트 보안 감사 수행
- **2026-02-01**: backend-developer 에이전트 Spring Security 구현 완료
- **2026-02-01**: test-architect 에이전트 테스트 작성 및 검증 (99개 통과)
- **2026-02-01**: pm 에이전트 최종 승인
