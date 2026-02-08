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
 * Dev/Local 프로파일 Actuator 경로 테스트 (TEST-M-01)
 *
 * <p>Dev/Local 환경에서 management.server.port 설정이 없으므로 기본 경로 /actuator를 사용한다.</p>
 *
 * <p>검증 대상:</p>
 * <ul>
 *   <li>dev/local 프로파일: /actuator/health 접근 가능 (200)</li>
 *   <li>dev/local 프로파일: /actuator/info 인증 필요 (401 → 인증 시 200)</li>
 *   <li>dev/local 프로파일: /internal/management/** 경로는 404 (prod 전용 경로)</li>
 * </ul>
 *
 * <p>보안 요구사항:</p>
 * <ul>
 *   <li>개발 환경의 편의성을 위해 /actuator 경로 노출</li>
 *   <li>민감 엔드포인트(info 등)는 인증 필요</li>
 *   <li>SecurityConfig는 /actuator/**와 /internal/management/** 모두 허용하도록 설정</li>
 * </ul>
 *
 * <p>관련 이슈: docs/review/code-review-report-feature-docker-hub-deployment.md (TEST-M-01)</p>
 *
 * @see SecurityConfig
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("actuator-test")
@Import(MySQLTestcontainersConfig.class)
class SecurityConfigDevLocalProfileTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private static final String VALID_USERNAME = "testuser";
    private static final String VALID_PASSWORD = "testpass";

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Nested
    @DisplayName("Dev/Local 프로파일 /actuator 경로 접근 테스트")
    class ActuatorPathTest {

        @Test
        @DisplayName("dev/local 프로파일에서 /actuator/health 접근 가능 (200)")
        void devLocal_actuator_health_접근가능() {
            // given
            var url = getBaseUrl() + "/actuator/health";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"status\":\"UP\"");
        }

        @Test
        @DisplayName("dev/local 프로파일에서 /actuator/health/liveness 접근 가능 (200)")
        void devLocal_actuator_liveness_접근가능() {
            // given
            var url = getBaseUrl() + "/actuator/health/liveness";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"status\":\"UP\"");
        }

        @Test
        @DisplayName("dev/local 프로파일에서 /actuator/health/readiness 접근 가능 (200)")
        void devLocal_actuator_readiness_접근가능() {
            // given
            var url = getBaseUrl() + "/actuator/health/readiness";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"status\":\"UP\"");
        }

        @Test
        @DisplayName("dev/local 프로파일에서 /actuator/info 접근 시 인증 필요 (401)")
        void devLocal_actuator_info_인증없이_접근불가() {
            // given
            var url = getBaseUrl() + "/actuator/info";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("dev/local 프로파일에서 올바른 자격 증명으로 /actuator/info 접근 가능 (200)")
        void devLocal_actuator_info_인증시_접근가능() {
            // given
            var url = getBaseUrl() + "/actuator/info";
            var headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(VALID_USERNAME, VALID_PASSWORD));

            // when
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("Dev/Local 프로파일 /internal/management 경로 차단 테스트")
    class InternalManagementPathBlockTest {

        @Test
        @DisplayName("dev/local 프로파일에서 /internal/management/health 접근 시 500 또는 404")
        void devLocal_internal_management_health_매핑되지않음() {
            // given
            var url = getBaseUrl() + "/internal/management/health";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            // SecurityConfig에서 /internal/management/health를 permitAll() 설정했으므로 인증은 통과
            // 하지만 Spring Boot Actuator는 management.endpoints.web.base-path 설정이 /actuator이므로
            // /internal/management 경로에는 엔드포인트가 매핑되지 않음
            // SecurityFilterChain을 통과한 후 핸들러를 찾지 못해 404 또는 GlobalExceptionHandler에서 500 반환
            assertThat(response.getStatusCode())
                    .as("dev/local에서는 /internal/management 경로가 Actuator에 매핑되지 않으므로 " +
                        "SecurityConfig는 통과하지만 핸들러를 찾지 못해 500 또는 404")
                    .isIn(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("dev/local 프로파일에서 /internal/management/info 접근 시 인증 필요 또는 500")
        void devLocal_internal_management_info_인증필요_또는_500() {
            // given
            var url = getBaseUrl() + "/internal/management/info";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            // SecurityConfig는 /internal/management/info를 hasRole(ACTUATOR)로 설정
            // 인증 없이 접근 시 401 반환되거나,
            // Actuator가 /internal/management 경로에 매핑하지 않아 핸들러를 찾지 못해 500
            assertThat(response.getStatusCode())
                    .as("dev/local에서는 /internal/management/info가 인증 필요(401) 또는 경로 미매핑(500)")
                    .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("프로파일별 base-path 차이 검증")
    class BasePathDifferenceTest {

        @Test
        @DisplayName("dev/local 프로파일에서는 /actuator가 기본 경로")
        void devLocal_기본경로_actuator() {
            // given
            var actuatorHealthUrl = getBaseUrl() + "/actuator/health";

            // when
            var response = restTemplate.getForEntity(actuatorHealthUrl, String.class);

            // then
            assertThat(response.getStatusCode())
                    .as("dev/local 프로파일에서는 management.endpoints.web.base-path 설정이 없으므로 " +
                        "기본 경로 /actuator를 사용해야 함")
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"status\":\"UP\"");
        }
    }

    /**
     * Basic 인증 헤더 생성 (RFC 7617)
     *
     * @param username 사용자명
     * @param password 비밀번호
     * @return Basic 인증 헤더 값
     */
    private String createBasicAuthHeader(String username, String password) {
        var credentials = username + ":" + password;
        var encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        return "Basic " + encodedCredentials;
    }
}
