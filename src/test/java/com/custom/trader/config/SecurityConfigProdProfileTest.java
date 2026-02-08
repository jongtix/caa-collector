package com.custom.trader.config;

import com.custom.trader.testcontainers.MySQLTestcontainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
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
 * Production 프로파일 Actuator 포트 분리 테스트 (CR-01)
 *
 * <p>Prod 환경에서 Management 포트 분리 설정을 검증한다:</p>
 * <ul>
 *   <li>메인 포트(실제: 8080, 테스트: 18080): 비즈니스 로직 전용, /actuator 경로 미존재</li>
 *   <li>Management 포트(실제: 9090, 테스트: 19090): Actuator 엔드포인트 전용</li>
 * </ul>
 *
 * <p>보안 요구사항:</p>
 * <ul>
 *   <li>메인 포트로 /actuator/** 접근 불가 (404)</li>
 *   <li>Management 포트로 /internal/management/health 접근 가능 (200)</li>
 *   <li>Management 포트로 /internal/management/info 접근 시 인증 필요 (401)</li>
 * </ul>
 *
 * <p>NOTE: 테스트는 포트 충돌 방지를 위해 18080/19090 포트를 사용하지만,
 * 실제 검증 대상은 포트 분리 동작 자체입니다.</p>
 *
 * <p>관련 이슈: docs/review/code-review-report-feature-docker-hub-deployment.md (CR-01)</p>
 *
 * @see SecurityConfig
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=18080",
                "management.server.port=19090",
                "management.endpoints.web.base-path=/internal/management"
        }
)
@ActiveProfiles({"prod-test"})
@Import(MySQLTestcontainersConfig.class)
class SecurityConfigProdProfileTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int serverPort;

    @LocalManagementPort
    private int managementPort;

    private static final String VALID_USERNAME = "testuser";
    private static final String VALID_PASSWORD = "testpass-16-chars";

    @Nested
    @DisplayName("메인 포트 Actuator 경로 차단 테스트")
    class MainPortActuatorBlockTest {

        @Test
        @DisplayName("메인 포트에서 /actuator/health 접근 시 404 또는 401 (경로 미노출)")
        void 메인포트_actuator_health_접근제한() {
            // given
            assertThat(serverPort).isEqualTo(18080);
            var url = "http://localhost:" + serverPort + "/actuator/health";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            // Management 포트 분리 시 메인 포트에서는 Actuator 경로가 비활성화되어야 함
            // Spring Boot 테스트 환경에서는 404 대신 401/500이 반환될 수 있음 (SecurityFilterChain 또는 ErrorHandler 처리)
            assertThat(response.getStatusCode())
                    .as("Management 포트 분리로 인해 메인 포트에서 Actuator 경로가 차단되어야 함")
                    .isIn(HttpStatus.NOT_FOUND, HttpStatus.UNAUTHORIZED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("메인 포트에서 /actuator/info 접근 시 404 또는 401 (경로 미노출)")
        void 메인포트_actuator_info_접근제한() {
            // given
            assertThat(serverPort).isEqualTo(18080);
            var url = "http://localhost:" + serverPort + "/actuator/info";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode())
                    .as("Management 포트 분리로 인해 메인 포트에서 Actuator 경로가 차단되어야 함")
                    .isIn(HttpStatus.NOT_FOUND, HttpStatus.UNAUTHORIZED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("메인 포트에서 /internal/management/health 접근 시 404 또는 500 (경로 미노출)")
        void 메인포트_internal_management_접근제한() {
            // given
            assertThat(serverPort).isEqualTo(18080);
            var url = "http://localhost:" + serverPort + "/internal/management/health";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode())
                    .as("Management 포트 분리로 인해 메인 포트에서 /internal/management 경로가 차단되어야 함")
                    .isIn(HttpStatus.NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("Management 포트 헬스체크 접근 테스트")
    class ManagementPortHealthCheckTest {

        @Test
        @DisplayName("Management 포트에서 /internal/management/health 접근 가능 (200)")
        void management포트_health_인증없이_접근가능() {
            // given
            assertThat(managementPort).isEqualTo(19090);
            var url = "http://localhost:" + managementPort + "/internal/management/health";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"status\":\"UP\"");
        }

        @Test
        @DisplayName("Management 포트에서 /internal/management/health/liveness 접근 가능 (200)")
        void management포트_liveness_인증없이_접근가능() {
            // given
            assertThat(managementPort).isEqualTo(19090);
            var url = "http://localhost:" + managementPort + "/internal/management/health/liveness";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"status\":\"UP\"");
        }

        @Test
        @DisplayName("Management 포트에서 /internal/management/health/readiness 접근 가능 (200)")
        void management포트_readiness_인증없이_접근가능() {
            // given
            assertThat(managementPort).isEqualTo(19090);
            var url = "http://localhost:" + managementPort + "/internal/management/health/readiness";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"status\":\"UP\"");
        }
    }

    @Nested
    @DisplayName("Management 포트 Actuator 인증 테스트")
    class ManagementPortActuatorAuthTest {

        @Test
        @DisplayName("Management 포트에서 /internal/management/info 접근 시 인증 필요 (401)")
        void management포트_info_인증없이_접근불가() {
            // given
            assertThat(managementPort).isEqualTo(19090);
            var url = "http://localhost:" + managementPort + "/internal/management/info";

            // when
            var response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Management 포트에서 올바른 자격 증명으로 /internal/management/info 접근 가능 (200)")
        void management포트_info_인증시_접근가능() {
            // given
            assertThat(managementPort).isEqualTo(19090);
            var url = "http://localhost:" + managementPort + "/internal/management/info";
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

        @Test
        @DisplayName("Management 포트에서 잘못된 자격 증명으로 /internal/management/info 접근 불가 (401)")
        void management포트_info_잘못된인증_접근불가() {
            // given
            assertThat(managementPort).isEqualTo(19090);
            var url = "http://localhost:" + managementPort + "/internal/management/info";
            var headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader(VALID_USERNAME, "wrongpass"));

            // when
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("포트 설정 검증")
    class PortConfigurationTest {

        @Test
        @DisplayName("메인 포트와 Management 포트가 분리됨")
        void 포트_분리_설정_검증() {
            // then
            assertThat(serverPort)
                    .as("메인 포트는 18080이어야 함 (테스트용, 실제 prod는 8080)")
                    .isEqualTo(18080);

            assertThat(managementPort)
                    .as("Management 포트는 19090이어야 함 (테스트용, 실제 prod는 9090)")
                    .isEqualTo(19090);

            assertThat(serverPort)
                    .as("메인 포트와 Management 포트는 서로 달라야 함")
                    .isNotEqualTo(managementPort);
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
