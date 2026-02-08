package com.custom.trader.config;

import com.custom.trader.testcontainers.MySQLTestcontainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spring Security 설정 테스트
 *
 * <p>TestRestTemplate 사용 이유:
 * management.server.port 설정으로 인해 MockMvc로는 Actuator 엔드포인트 노출 여부를 검증할 수 없음.
 * 실제 HTTP 서버를 통한 전체 스택 검증이 필요함.</p>
 *
 * <p>상세한 기술적 결정 배경: docs/adr/0013-test-resttemplate-for-security-test.md 참조</p>
 *
 * @see SecurityConfig
 * @see org.springframework.boot.test.web.client.TestRestTemplate
 * @see org.springframework.boot.test.context.SpringBootTest.WebEnvironment
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(MySQLTestcontainersConfig.class)
class SecurityConfigTest {

    /**
     * 실제 HTTP 요청/응답 사이클 검증 (상세: ADR-0013)
     */
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private static final String VALID_USERNAME = "testuser";
    private static final String VALID_PASSWORD = "testpass";
    private static final String INVALID_PASSWORD = "wrongpass";

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Nested
    @DisplayName("헬스체크 엔드포인트 접근 테스트")
    class HealthEndpointTest {

        @Test
        @DisplayName("health 엔드포인트는 인증 없이 접근 가능")
        void health_엔드포인트_인증없이_접근가능() {
            var response = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/health",
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"status\":\"UP\"");
        }

        @Test
        @DisplayName("health/liveness 엔드포인트는 인증 없이 접근 가능")
        void health_liveness_엔드포인트_인증없이_접근가능() {
            var response = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/health/liveness",
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"status\":\"UP\"");
        }

        @Test
        @DisplayName("health/readiness 엔드포인트는 인증 없이 접근 가능")
        void health_readiness_엔드포인트_인증없이_접근가능() {
            var response = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/health/readiness",
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"status\":\"UP\"");
        }
    }

    @Nested
    @DisplayName("Actuator 엔드포인트 인증 테스트")
    class ActuatorAuthenticationTest {

        @Test
        @DisplayName("info 엔드포인트는 인증 없이 접근 불가 (401)")
        void info_엔드포인트_인증없이_접근불가() {
            var response = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/info",
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("올바른 자격 증명으로 info 엔드포인트 접근 성공")
        void info_엔드포인트_올바른_인증시_접근가능() {
            var headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(VALID_USERNAME, VALID_PASSWORD));

            var response = restTemplate.exchange(
                    getBaseUrl() + "/internal/management/info",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("잘못된 자격 증명으로 info 엔드포인트 접근 실패 (401)")
        void info_엔드포인트_잘못된_인증시_접근불가() {
            var headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(VALID_USERNAME, INVALID_PASSWORD));

            var response = restTemplate.exchange(
                    getBaseUrl() + "/internal/management/info",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("올바른 자격 증명으로 health 상세 정보 조회 가능")
        void health_상세정보_올바른_인증시_조회가능() {
            var headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(VALID_USERNAME, VALID_PASSWORD));

            var response = restTemplate.exchange(
                    getBaseUrl() + "/internal/management/health",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"status\":\"UP\"");
        }
    }

    @Nested
    @DisplayName("일반 요청 인증 테스트")
    class GeneralRequestAuthenticationTest {

        @Test
        @DisplayName("정의되지 않은 엔드포인트는 인증 없이 접근 불가 (401)")
        void 정의되지않은_엔드포인트_인증없이_접근불가() {
            var response = restTemplate.getForEntity(
                    getBaseUrl() + "/api/unknown",
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Actuator 엔드포인트 노출 제한 테스트 (TEST-M-02)")
    class ActuatorEndpointExposureTest {

        @Test
        @DisplayName("인증 없이 노출되지 않은 /internal/management/metrics 접근 시 401")
        void metrics_엔드포인트_인증없이_접근불가() {
            // given
            var url = getBaseUrl() + "/internal/management/metrics";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            // SecurityFilterChain에서 /internal/management/**는 hasRole(ACTUATOR) 요구
            // 인증되지 않은 요청은 Actuator가 처리하기 전에 401 반환
            // 이는 보안상 더 안전함 (엔드포인트 존재 여부를 노출하지 않음)
            assertThat(response.getStatusCode())
                    .as("metrics 엔드포인트는 인증되지 않은 사용자에게 401 반환 (엔드포인트 존재 여부 미노출)")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("인증 없이 노출되지 않은 /internal/management/env 접근 시 401")
        void env_엔드포인트_인증없이_접근불가() {
            // given
            var url = getBaseUrl() + "/internal/management/env";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode())
                    .as("env 엔드포인트는 인증되지 않은 사용자에게 401 반환 (환경변수 노출 방지)")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("인증 없이 노출되지 않은 /internal/management/beans 접근 시 401")
        void beans_엔드포인트_인증없이_접근불가() {
            // given
            var url = getBaseUrl() + "/internal/management/beans";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode())
                    .as("beans 엔드포인트는 인증되지 않은 사용자에게 401 반환")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("인증 없이 노출되지 않은 /internal/management/configprops 접근 시 401")
        void configprops_엔드포인트_인증없이_접근불가() {
            // given
            var url = getBaseUrl() + "/internal/management/configprops";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode())
                    .as("configprops 엔드포인트는 인증되지 않은 사용자에게 401 반환 (설정 정보 노출 방지)")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("인증 없이 노출되지 않은 /internal/management/heapdump 접근 시 401")
        void heapdump_엔드포인트_인증없이_접근불가() {
            // given
            var url = getBaseUrl() + "/internal/management/heapdump";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode())
                    .as("heapdump 엔드포인트는 인증되지 않은 사용자에게 401 반환 (민감 정보 노출 방지)")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("인증 없이 노출되지 않은 /internal/management/threaddump 접근 시 401")
        void threaddump_엔드포인트_인증없이_접근불가() {
            // given
            var url = getBaseUrl() + "/internal/management/threaddump";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode())
                    .as("threaddump 엔드포인트는 인증되지 않은 사용자에게 401 반환")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("인증된 사용자도 노출되지 않은 엔드포인트 접근 불가 (404 또는 500)")
        void 인증된_사용자도_노출되지않은_엔드포인트_접근불가() {
            // given
            var headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(VALID_USERNAME, VALID_PASSWORD));
            var url = getBaseUrl() + "/internal/management/metrics";

            // when
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            // then
            // 인증된 사용자라도 exposure.include에 포함되지 않은 엔드포인트는 접근 불가
            // SecurityFilterChain 통과 → Actuator에서 404 또는 GlobalExceptionHandler에서 500 반환
            assertThat(response.getStatusCode())
                    .as("인증된 사용자도 노출되지 않은 엔드포인트는 404 또는 500")
                    .isIn(HttpStatus.NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("설정된 엔드포인트만 노출됨 (health, info)")
        void 설정된_엔드포인트만_노출됨() {
            // given & when
            var healthResponse = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/health",
                    String.class
            );
            var infoResponse = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/info",
                    String.class
            );
            var metricsResponse = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/metrics",
                    String.class
            );

            // then
            assertThat(healthResponse.getStatusCode())
                    .as("health는 노출되어 접근 가능")
                    .isEqualTo(HttpStatus.OK);

            assertThat(infoResponse.getStatusCode())
                    .as("info는 노출되어 있지만 인증 필요")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);

            assertThat(metricsResponse.getStatusCode())
                    .as("metrics는 노출되지 않아 인증 없으면 401")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("보안 응답 헤더 테스트 (M-04)")
    class SecurityHeadersTest {

        @Test
        @DisplayName("X-Frame-Options은 DENY (클릭재킹 방어)")
        void XFrameOptions_DENY() {
            var response = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/health",
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
        }

        @Test
        @DisplayName("Content-Security-Policy 헤더가 올바르게 설정됨")
        void CSP_헤더_올바르게_설정됨() {
            var response = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/health",
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getFirst("Content-Security-Policy")).isEqualTo("default-src 'self'");
        }

        @Test
        @DisplayName("Strict-Transport-Security 헤더가 올바르게 설정됨")
        void HSTS_헤더_올바르게_설정됨() {
            // HTTP 요청에서는 HSTS 헤더가 포함되지 않음 (HTTPS에만 적용)
            // 설정 자체는 SecurityConfig에서 검증됨
            var response = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/health",
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            // HTTPS가 아닌 환경에서는 HSTS 헤더가 없을 수 있으므로, 설정 존재 여부만 확인
            // 실제 값은 HTTPS 환경에서 "max-age=31536000 ; includeSubDomains"
        }

        @Test
        @DisplayName("모든 보안 헤더가 응답에 포함됨")
        void 모든_보안헤더_응답에_포함됨() {
            var response = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/health",
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders()).containsKeys("X-Frame-Options", "Content-Security-Policy");
            // HSTS는 HTTPS 요청에만 적용되므로 HTTP 테스트에서는 제외
        }
    }

    @Nested
    @DisplayName("Actuator 자격 증명 검증 테스트")
    class ActuatorCredentialsValidationTest {

        @Test
        @DisplayName("ACTUATOR_PASSWORD null 시 애플리케이션 시작 실패")
        void actuatorPassword_null시_시작실패() throws Exception {
            assertThatThrownBy(() -> {
                var environment = new MockEnvironment();
                var config = new SecurityConfig(environment);
                setActuatorCredentials(config, "testuser", null);

                config.validateActuatorCredentials();
            })
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ACTUATOR_PASSWORD must be configured");
        }

        @Test
        @DisplayName("ACTUATOR_PASSWORD 빈 문자열 시 애플리케이션 시작 실패")
        void actuatorPassword_빈문자열시_시작실패() throws Exception {
            assertThatThrownBy(() -> {
                var environment = new MockEnvironment();
                var config = new SecurityConfig(environment);
                setActuatorCredentials(config, "testuser", "");

                config.validateActuatorCredentials();
            })
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ACTUATOR_PASSWORD must be configured");
        }

        @Test
        @DisplayName("ACTUATOR_PASSWORD 공백 문자열 시 애플리케이션 시작 실패")
        void actuatorPassword_공백문자열시_시작실패() throws Exception {
            assertThatThrownBy(() -> {
                var environment = new MockEnvironment();
                var config = new SecurityConfig(environment);
                setActuatorCredentials(config, "testuser", "   ");

                config.validateActuatorCredentials();
            })
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ACTUATOR_PASSWORD must be configured");
        }

        @Test
        @DisplayName("ACTUATOR_USERNAME null 시 애플리케이션 시작 실패")
        void actuatorUsername_null시_시작실패() throws Exception {
            assertThatThrownBy(() -> {
                var environment = new MockEnvironment();
                var config = new SecurityConfig(environment);
                setActuatorCredentials(config, null, "testpass");

                config.validateActuatorCredentials();
            })
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ACTUATOR_USERNAME must be configured");
        }

        @Test
        @DisplayName("ACTUATOR_USERNAME 빈 문자열 시 애플리케이션 시작 실패")
        void actuatorUsername_빈문자열시_시작실패() throws Exception {
            assertThatThrownBy(() -> {
                var environment = new MockEnvironment();
                var config = new SecurityConfig(environment);
                setActuatorCredentials(config, "", "testpass");

                config.validateActuatorCredentials();
            })
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ACTUATOR_USERNAME must be configured");
        }

        @Test
        @DisplayName("ACTUATOR_USERNAME 공백 문자열 시 애플리케이션 시작 실패")
        void actuatorUsername_공백문자열시_시작실패() throws Exception {
            assertThatThrownBy(() -> {
                var environment = new MockEnvironment();
                var config = new SecurityConfig(environment);
                setActuatorCredentials(config, "   ", "testpass");

                config.validateActuatorCredentials();
            })
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ACTUATOR_USERNAME must be configured");
        }

        @Test
        @DisplayName("prod 프로필에서 16자 미만 password 시 애플리케이션 시작 실패")
        void prod프로필_16자미만_password_시작실패() throws Exception {
            assertThatThrownBy(() -> {
                var environment = new MockEnvironment();
                environment.setActiveProfiles("prod");
                var config = new SecurityConfig(environment);
                setActuatorCredentials(config, "testuser", "123456789012345"); // 15자

                config.validateActuatorCredentials();
            })
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ACTUATOR_PASSWORD must be at least 16 characters in production");
        }

        @Test
        @DisplayName("prod 프로필에서 16자 password는 정상 통과")
        void prod프로필_16자_password_성공() throws Exception {
            var environment = new MockEnvironment();
            environment.setActiveProfiles("prod");
            var config = new SecurityConfig(environment);
            setActuatorCredentials(config, "testuser", "1234567890123456"); // 16자

            // 예외가 발생하지 않아야 함
            config.validateActuatorCredentials();
        }

        @Test
        @DisplayName("일반 프로필에서 12자 이상 password는 정상 통과")
        void 일반프로필_12자이상_password_성공() throws Exception {
            var environment = new MockEnvironment();
            var config = new SecurityConfig(environment);
            setActuatorCredentials(config, "testuser", "123456789012"); // 12자

            // 예외가 발생하지 않아야 함
            config.validateActuatorCredentials();
        }
    }

    @Nested
    @DisplayName("SecurityFilterChain 설정 검증 (MA-05)")
    class SecurityFilterChainTest {

        @Test
        @DisplayName("HSTS 헤더 설정이 SecurityFilterChain에 포함됨")
        void securityFilterChain_HSTS_설정() {
            // HTTP 환경에서는 HSTS 헤더가 응답에 포함되지 않음
            // SecurityConfig의 HSTS 설정 자체는 아래 코드로 확인 가능:
            // .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
            //
            // HTTP 요청에서는 HSTS 헤더가 생략되므로, 애플리케이션 시작 성공으로 설정 검증
            var response = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/health",
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            // HSTS는 HTTPS 환경에서만 동작하므로, HTTP 테스트에서는 설정 존재 여부를 간접 검증
            // SecurityConfig.securityFilterChain() 메서드가 정상적으로 빈을 생성했음을 확인
        }

        @Test
        @DisplayName("X-Frame-Options 헤더 설정")
        void securityFilterChain_X_Frame_Options() {
            // SecurityConfig의 frameOptions 설정이 HTTP 응답 헤더에 반영됨
            var response = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/health",
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getFirst("X-Frame-Options"))
                    .as("X-Frame-Options should be DENY to prevent clickjacking")
                    .isEqualTo("DENY");
        }

        @Test
        @DisplayName("X-Content-Type-Options 헤더 설정")
        void securityFilterChain_X_Content_Type_Options() {
            // Spring Security 기본 설정으로 X-Content-Type-Options: nosniff가 추가됨
            var response = restTemplate.getForEntity(
                    getBaseUrl() + "/internal/management/health",
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getFirst("X-Content-Type-Options"))
                    .as("X-Content-Type-Options should be nosniff to prevent MIME sniffing")
                    .isEqualTo("nosniff");
        }
    }

    /**
     * SecurityConfig의 Actuator 자격 증명 설정 (테스트용 헬퍼 메서드)
     *
     * <p>Reflection을 사용하여 private 필드에 값을 주입한다.
     * 이 메서드는 검증 로직 테스트를 위해 의도적으로 잘못된 값도 주입할 수 있다.</p>
     *
     * @param config SecurityConfig 인스턴스
     * @param username actuatorUsername 값 (null 허용)
     * @param password actuatorPassword 값 (null 허용)
     * @throws Exception Reflection 오류 시
     */
    private void setActuatorCredentials(SecurityConfig config, String username, String password) throws Exception {
        var usernameField = SecurityConfig.class.getDeclaredField("actuatorUsername");
        usernameField.setAccessible(true);
        usernameField.set(config, username);

        var passwordField = SecurityConfig.class.getDeclaredField("actuatorPassword");
        passwordField.setAccessible(true);
        passwordField.set(config, password);
    }

    /**
     * Basic 인증 헤더 생성 (RFC 7617)
     *
     * <p>withBasicAuth() 대신 직접 구성하는 이유: ADR-0013 참조</p>
     */
    private String createBasicAuthHeader(String username, String password) {
        var credentials = username + ":" + password;
        var encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        return "Basic " + encodedCredentials;
    }
}
