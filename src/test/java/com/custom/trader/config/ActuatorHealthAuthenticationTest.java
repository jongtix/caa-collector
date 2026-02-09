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
import org.springframework.test.context.ActiveProfiles;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Healthcheck Sidecar 통합 테스트
 *
 * <p>Distroless 이미지 전환으로 Healthcheck Sidecar 패턴이 도입되었습니다.
 * 이 테스트는 Sidecar가 Actuator health 엔드포인트에 접근하는 시나리오를 검증합니다.</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>인증 없이 health 엔드포인트 접근 (기본 정보만 반환)</li>
 *   <li>올바른 인증으로 health 엔드포인트 접근 (상세 정보 반환)</li>
 *   <li>잘못된 인증으로 health 엔드포인트 접근 (401)</li>
 * </ul>
 *
 * <h3>Docker Compose Sidecar 설정 참조</h3>
 * <pre>{@code
 * collector-healthcheck:
 *   image: curlimages/curl:8.11.1
 *   network_mode: "service:collector"
 *   command: >
 *     curl -f -s -u "$ACTUATOR_USERNAME:$ACTUATOR_PASSWORD"
 *     http://localhost:9090/internal/management/health
 * }</pre>
 *
 * @see SecurityConfig
 * @see SecurityConfigTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(MySQLTestcontainersConfig.class)
class ActuatorHealthAuthenticationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private static final String VALID_USERNAME = "testuser";
    private static final String VALID_PASSWORD = "testpass";
    private static final String INVALID_USERNAME = "wronguser";
    private static final String INVALID_PASSWORD = "wrongpass";

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Nested
    @DisplayName("Healthcheck Sidecar 시나리오 테스트 (CR-04)")
    class HealthcheckSidecarScenarioTest {

        @Test
        @DisplayName("인증 없이 health 엔드포인트 접근 시 200 OK (기본 상태만 반환)")
        void health_인증없이_접근_성공() {
            // given
            var url = getBaseUrl() + "/internal/management/health";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .as("인증 없이는 기본 상태만 반환")
                    .contains("\"status\":\"UP\"")
                    .doesNotContain("\"components\"");
        }

        @Test
        @DisplayName("올바른 인증으로 health 엔드포인트 접근 시 200 OK (상세 정보 반환)")
        void health_올바른_인증_상세정보_조회() {
            // given
            var headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(VALID_USERNAME, VALID_PASSWORD));
            var url = getBaseUrl() + "/internal/management/health";

            // when
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .as("인증된 사용자는 상세 정보 조회 가능")
                    .contains("\"status\":\"UP\"")
                    .contains("\"components\"");
        }

        @Test
        @DisplayName("잘못된 인증으로 health 엔드포인트 접근 시 401 Unauthorized")
        void health_잘못된_인증_접근_실패() {
            // given
            var headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(VALID_USERNAME, INVALID_PASSWORD));
            var url = getBaseUrl() + "/internal/management/health";

            // when
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            // then
            assertThat(response.getStatusCode())
                    .as("잘못된 인증 정보는 401 반환")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 접근 시 401 Unauthorized")
        void health_존재하지않는_사용자_접근_실패() {
            // given
            var headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(INVALID_USERNAME, INVALID_PASSWORD));
            var url = getBaseUrl() + "/internal/management/health";

            // when
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            // then
            assertThat(response.getStatusCode())
                    .as("존재하지 않는 사용자는 401 반환")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Liveness/Readiness Probe 테스트 (Kubernetes 호환)")
    class KubernetesProbeTest {

        @Test
        @DisplayName("liveness 엔드포인트는 인증 없이 접근 가능")
        void liveness_인증없이_접근_성공() {
            // given
            var url = getBaseUrl() + "/internal/management/health/liveness";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"status\":\"UP\"");
        }

        @Test
        @DisplayName("readiness 엔드포인트는 인증 없이 접근 가능")
        void readiness_인증없이_접근_성공() {
            // given
            var url = getBaseUrl() + "/internal/management/health/readiness";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"status\":\"UP\"");
        }

        @Test
        @DisplayName("liveness 엔드포인트는 인증 없이 또는 올바른 인증으로 접근 가능")
        void liveness_인증_선택적_접근_가능() {
            // given: 올바른 인증
            var headersValid = new HttpHeaders();
            headersValid.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(VALID_USERNAME, VALID_PASSWORD));
            var url = getBaseUrl() + "/internal/management/health/liveness";

            // when: 올바른 인증으로 접근
            var responseWithAuth = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headersValid),
                    String.class
            );

            // when: 인증 없이 접근
            var responseWithoutAuth = restTemplate.getForEntity(url, String.class);

            // then: 둘 다 200 OK
            assertThat(responseWithAuth.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseWithoutAuth.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseWithAuth.getBody()).contains("\"status\":\"UP\"");
            assertThat(responseWithoutAuth.getBody()).contains("\"status\":\"UP\"");
        }

        @Test
        @DisplayName("readiness 엔드포인트는 인증 없이 또는 올바른 인증으로 접근 가능")
        void readiness_인증_선택적_접근_가능() {
            // given: 올바른 인증
            var headersValid = new HttpHeaders();
            headersValid.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(VALID_USERNAME, VALID_PASSWORD));
            var url = getBaseUrl() + "/internal/management/health/readiness";

            // when: 올바른 인증으로 접근
            var responseWithAuth = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headersValid),
                    String.class
            );

            // when: 인증 없이 접근
            var responseWithoutAuth = restTemplate.getForEntity(url, String.class);

            // then: 둘 다 200 OK
            assertThat(responseWithAuth.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseWithoutAuth.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseWithAuth.getBody()).contains("\"status\":\"UP\"");
            assertThat(responseWithoutAuth.getBody()).contains("\"status\":\"UP\"");
        }

        @Test
        @DisplayName("liveness 엔드포인트는 잘못된 인증 시 401 반환 (Spring Security 기본 동작)")
        void liveness_잘못된_인증_접근_실패() {
            // given: 잘못된 인증
            // Spring Security 특성: permitAll()이어도 Authorization 헤더가 있으면 인증 시도
            var headersInvalid = new HttpHeaders();
            headersInvalid.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(INVALID_USERNAME, INVALID_PASSWORD));
            var url = getBaseUrl() + "/internal/management/health/liveness";

            // when: 잘못된 인증으로 접근
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headersInvalid),
                    String.class
            );

            // then: 401 Unauthorized
            // 이는 보안상 바람직함 (잘못된 인증 시도를 명시적으로 거부)
            assertThat(response.getStatusCode())
                    .as("잘못된 인증 헤더가 있으면 Spring Security가 인증 시도 후 실패")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("readiness 엔드포인트는 잘못된 인증 시 401 반환 (Spring Security 기본 동작)")
        void readiness_잘못된_인증_접근_실패() {
            // given: 잘못된 인증
            var headersInvalid = new HttpHeaders();
            headersInvalid.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(INVALID_USERNAME, INVALID_PASSWORD));
            var url = getBaseUrl() + "/internal/management/health/readiness";

            // when: 잘못된 인증으로 접근
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headersInvalid),
                    String.class
            );

            // then: 401 Unauthorized
            assertThat(response.getStatusCode())
                    .as("잘못된 인증 헤더가 있으면 Spring Security가 인증 시도 후 실패")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("보안 엣지 케이스 테스트")
    class SecurityEdgeCaseTest {

        @Test
        @DisplayName("Authorization 헤더 없이 health 접근 시 기본 정보만 반환")
        void health_헤더없이_접근_기본정보만_반환() {
            // given
            var url = getBaseUrl() + "/internal/management/health";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .as("인증 없으면 components 정보 숨김")
                    .contains("\"status\":")
                    .doesNotContain("\"components\"");
        }

        @Test
        @DisplayName("빈 Authorization 헤더로 health 접근 시 기본 정보만 반환")
        void health_빈헤더로_접근_기본정보만_반환() {
            // given
            var headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "");
            var url = getBaseUrl() + "/internal/management/health";

            // when
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .contains("\"status\":")
                    .doesNotContain("\"components\"");
        }

        @Test
        @DisplayName("잘못된 형식의 Authorization 헤더로 접근 시 무시되고 200 OK (Spring Security 기본 동작)")
        void health_잘못된형식_헤더로_접근_성공() {
            // given
            // Spring Security 특성: "InvalidFormat" 같은 인식 불가능한 Authorization 헤더는 무시
            // "Basic xxx" 형식이 아니면 Basic Auth 필터가 처리하지 않음
            var headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "InvalidFormat");
            var url = getBaseUrl() + "/internal/management/health";

            // when
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            // then
            assertThat(response.getStatusCode())
                    .as("인식 불가능한 Authorization 헤더는 무시되고 permitAll() 적용")
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .as("기본 정보만 반환")
                    .contains("\"status\":")
                    .doesNotContain("\"components\"");
        }

        @Test
        @DisplayName("Base64 디코딩 불가능한 Authorization 헤더로 접근 시 401")
        void health_디코딩불가능_헤더로_접근_실패() {
            // given
            var headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Basic !!!invalid-base64!!!");
            var url = getBaseUrl() + "/internal/management/health";

            // when
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            // then
            assertThat(response.getStatusCode())
                    .as("디코딩 불가능한 헤더는 401 반환")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("콜론이 없는 Base64 인코딩 헤더로 접근 시 401")
        void health_콜론없는_헤더로_접근_실패() {
            // given
            var headers = new HttpHeaders();
            // "usernameonly" → Base64 인코딩 (콜론 없음)
            var encodedWithoutColon = Base64.getEncoder().encodeToString("usernameonly".getBytes());
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedWithoutColon);
            var url = getBaseUrl() + "/internal/management/health";

            // when
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            // then
            assertThat(response.getStatusCode())
                    .as("콜론이 없는 형식은 401 반환")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Basic 인증 헤더 생성 (RFC 7617)
     *
     * @param username 사용자명
     * @param password 비밀번호
     * @return "Basic [base64-encoded-credentials]" 형식의 헤더 값
     */
    private String createBasicAuthHeader(String username, String password) {
        var credentials = username + ":" + password;
        var encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        return "Basic " + encodedCredentials;
    }
}
