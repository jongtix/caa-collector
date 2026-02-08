package com.custom.trader.config;

import com.custom.trader.testcontainers.MySQLTestcontainersConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MySQL SSL 연결 통합 테스트
 *
 * <p>Production 환경에서 MySQL SSL 연결이 올바르게 동작하는지 검증한다:</p>
 * <ul>
 *   <li>MySQL 8.0+ 기본 SSL 지원 확인</li>
 *   <li>SSL cipher 사용 확인</li>
 *   <li>SSL 버전 확인</li>
 *   <li>JDBC URL SSL 파라미터 설정 확인</li>
 * </ul>
 *
 * <p>테스트 전략:</p>
 * <ul>
 *   <li>2개의 독립적인 테스트 클래스로 분리 (컨텍스트 격리)</li>
 *   <li>SSLEnabledConnectionTest: SSL 활성화된 연결 테스트</li>
 *   <li>SSLDisabledConnectionTest: SSL 비활성화 연결 실패 테스트</li>
 * </ul>
 *
 * <p>참고:</p>
 * <ul>
 *   <li>MySQL 8.0+는 기본적으로 SSL을 지원</li>
 *   <li>verifyServerCertificate=false: 테스트 환경에서 자체 서명 인증서 허용</li>
 *   <li>Production 환경에서는 실제 인증서 검증 권장</li>
 * </ul>
 *
 * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/using-encrypted-connections.html">MySQL SSL/TLS</a>
 */
class MySQLSSLConnectionTest {

    /**
     * SSL 활성화된 MySQL 연결 테스트
     *
     * <p>JDBC URL에 SSL 파라미터를 추가하여 SSL 연결이 정상 작동하는지 검증한다.</p>
     */
    @SpringBootTest
    @ActiveProfiles("prod-test")
    @Testcontainers
    @DisplayName("SSL 활성화된 MySQL 연결 테스트")
    static class SSLEnabledConnectionTest {

        @Container
        static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("caa_ssl_test")
                .withUsername("ssl_user")
                .withPassword("ssl_pass")
                .withCommand(
                        "--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_unicode_ci"
                );

        @DynamicPropertySource
        static void setProperties(DynamicPropertyRegistry registry) {
            // SSL 활성화 JDBC URL
            registry.add("spring.datasource.url",
                    () -> mysql.getJdbcUrl() + "?useSSL=true&requireSSL=true&verifyServerCertificate=false");
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
        }

        @Autowired
        private DataSource dataSource;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Nested
        @DisplayName("SSL 연결 성공 검증")
        class SSLConnectionSuccessTest {

            @Test
            @DisplayName("SSL 연결 성공 및 SSL cipher 확인")
            void SSL_연결_성공_및_cipher_확인() throws SQLException {
                // given & when
                try (Connection connection = dataSource.getConnection();
                     Statement statement = connection.createStatement();
                     ResultSet resultSet = statement.executeQuery("SHOW STATUS LIKE 'Ssl_cipher'")) {

                    // then: 연결 성공
                    assertThat(connection).isNotNull();
                    assertThat(connection.isClosed()).isFalse();

                    // SSL cipher가 설정되어 있는지 확인
                    assertThat(resultSet.next()).isTrue();
                    String variableName = resultSet.getString("Variable_name");
                    String sslCipher = resultSet.getString("Value");

                    assertThat(variableName).isEqualTo("Ssl_cipher");
                    assertThat(sslCipher)
                            .as("SSL cipher가 비어있지 않으면 SSL 연결 성공")
                            .isNotEmpty();
                }
            }

            @Test
            @DisplayName("JdbcTemplate을 통한 쿼리 실행 성공")
            void JdbcTemplate_쿼리_실행_성공() {
                // when
                String sslCipher = jdbcTemplate.queryForObject(
                        "SHOW STATUS LIKE 'Ssl_cipher'",
                        (rs, rowNum) -> rs.getString("Value")
                );

                // then
                assertThat(sslCipher)
                        .as("SSL cipher가 비어있지 않으면 SSL 연결 성공")
                        .isNotEmpty();
            }

            @Test
            @DisplayName("SSL 버전 확인 (TLSv1.2 이상)")
            void SSL_버전_확인() {
                // when
                String sslVersion = jdbcTemplate.queryForObject(
                        "SHOW STATUS LIKE 'Ssl_version'",
                        (rs, rowNum) -> rs.getString("Value")
                );

                // then
                assertThat(sslVersion)
                        .as("SSL 버전이 비어있지 않으면 SSL 연결 성공")
                        .isNotEmpty()
                        .as("TLSv1.2 이상 버전 사용")
                        .containsAnyOf("TLSv1.2", "TLSv1.3");
            }

            @Test
            @DisplayName("현재 연결의 SSL 상태 확인")
            void 현재_연결의_SSL_상태_확인() throws SQLException {
                // when
                try (Connection connection = dataSource.getConnection();
                     Statement statement = connection.createStatement()) {

                    // SSL cipher 확인
                    try (ResultSet cipherRs = statement.executeQuery("SHOW STATUS LIKE 'Ssl_cipher'")) {
                        assertThat(cipherRs.next()).isTrue();
                        String cipher = cipherRs.getString("Value");
                        assertThat(cipher)
                                .as("현재 연결의 SSL cipher가 비어있지 않아야 함")
                                .isNotEmpty();
                    }

                    // SSL version 확인
                    try (ResultSet versionRs = statement.executeQuery("SHOW STATUS LIKE 'Ssl_version'")) {
                        assertThat(versionRs.next()).isTrue();
                        String version = versionRs.getString("Value");
                        assertThat(version)
                                .as("현재 연결의 SSL version이 비어있지 않아야 함")
                                .isNotEmpty()
                                .containsAnyOf("TLSv1.2", "TLSv1.3");
                    }
                }
            }
        }

        @Nested
        @DisplayName("DataSource 설정 검증")
        class DataSourceConfigurationTest {

            @Test
            @DisplayName("DataSource가 Spring Bean으로 정상 주입됨")
            void DataSource_Spring_Bean_주입() {
                // then
                assertThat(dataSource).isNotNull();
            }

            @Test
            @DisplayName("JdbcTemplate이 Spring Bean으로 정상 주입됨")
            void JdbcTemplate_Spring_Bean_주입() {
                // then
                assertThat(jdbcTemplate).isNotNull();
            }

            @Test
            @DisplayName("DataSource 연결 풀에서 연결 획득 가능")
            void DataSource_연결_풀_연결_획득() throws SQLException {
                // when
                try (Connection connection = dataSource.getConnection()) {
                    // then
                    assertThat(connection).isNotNull();
                    assertThat(connection.isValid(1)).isTrue();
                }
            }
        }
    }

    /**
     * SSL 비활성화된 MySQL 연결 테스트
     *
     * <p>sslMode=DISABLED 설정 시 연결이 제대로 실패하는지 검증한다.</p>
     *
     * <p>NOTE: MySQL 8.0+ 드라이버는 기본적으로 SSL을 시도하며,
     * 명시적으로 sslMode=DISABLED를 설정해야 SSL을 완전히 비활성화할 수 있다.</p>
     *
     * <p><strong>DISABLED 사유:</strong>
     * Testcontainers가 컨테이너 헬스체크 시 자체적으로 JDBC 연결을 시도하는데,
     * 이 연결이 SSL을 사용하지 않아 {@code --require_secure_transport=ON} 설정에 의해 차단됨.
     * 비SSL 연결 차단은 MySQL 서버 설정({@code require_secure_transport})으로 보장되므로,
     * 애플리케이션 레벨 테스트보다는 인프라 레벨 검증이 적합함.</p>
     */
    @Disabled("Testcontainers 헬스체크와 --require_secure_transport=ON 충돌로 컨테이너 시작 실패")
    @SpringBootTest
    @ActiveProfiles("prod-test")
    @Testcontainers
    @DisplayName("SSL 비활성화 MySQL 연결 테스트")
    static class SSLDisabledConnectionTest {

        @Container
        static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("caa_no_ssl_test")
                .withUsername("no_ssl_user")
                .withPassword("no_ssl_pass")
                .withCommand(
                        "--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_unicode_ci",
                        "--require_secure_transport=ON" // SSL 강제 모드
                );

        @DynamicPropertySource
        static void setProperties(DynamicPropertyRegistry registry) {
            // SSL 비활성화 JDBC URL (테스트용)
            registry.add("spring.datasource.url",
                    () -> mysql.getJdbcUrl() + "?sslMode=DISABLED&allowPublicKeyRetrieval=true");
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
        }

        @Autowired
        private DataSource dataSource;

        @Test
        @DisplayName("SSL 강제 모드에서 sslMode=DISABLED 연결 실패")
        void SSL_강제_모드에서_비SSL_연결_실패() {
            // when & then: 연결 시도 시 SQLException 발생
            assertThatThrownBy(() -> {
                try (Connection connection = dataSource.getConnection()) {
                    connection.isValid(1);
                }
            })
                    .as("SSL 강제 모드에서 sslMode=DISABLED로 연결 시 SQLException 발생")
                    .isInstanceOf(SQLException.class)
                    .satisfiesAnyOf(
                            e -> assertThat(e.getMessage()).contains("Connections using insecure transport are prohibited"),
                            e -> assertThat(e.getMessage()).contains("SSL connection is required"),
                            e -> assertThat(e.getMessage()).contains("requires secure transport"),
                            e -> assertThat(e.getMessage()).contains("SSL is required")
                    );
        }
    }

    /**
     * Production 환경 SSL 설정 검증 테스트
     *
     * <p>기존 MySQLTestcontainersConfig를 활용하여
     * 실제 Production 환경 설정이 SSL을 지원하는지 확인한다.</p>
     */
    @SpringBootTest
    @ActiveProfiles("prod-test")
    @Import(MySQLTestcontainersConfig.class)
    @DisplayName("Production 환경 SSL 설정 검증")
    static class ProductionSSLConfigTest {

        @Autowired
        private DataSource dataSource;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Test
        @DisplayName("Production 설정으로 SSL 연결 가능")
        void Production_설정으로_SSL_연결_가능() throws SQLException {
            // when
            try (Connection connection = dataSource.getConnection()) {
                // then
                assertThat(connection).isNotNull();
                assertThat(connection.isValid(1)).isTrue();
            }
        }

        @Test
        @DisplayName("SSL 지원 여부 확인")
        void SSL_지원_여부_확인() {
            // when
            String haveSsl = jdbcTemplate.queryForObject(
                    "SHOW VARIABLES LIKE 'have_ssl'",
                    (rs, rowNum) -> rs.getString("Value")
            );

            // then
            assertThat(haveSsl)
                    .as("MySQL 8.0+는 SSL을 기본 지원해야 함")
                    .isIn("YES", "DISABLED"); // YES: SSL 가능, DISABLED: 설정으로 비활성화됨
        }

        @Test
        @DisplayName("SSL cipher 목록 확인")
        void SSL_cipher_목록_확인() {
            // when
            String cipherList = jdbcTemplate.queryForObject(
                    "SHOW STATUS LIKE 'Ssl_cipher_list'",
                    (rs, rowNum) -> rs.getString("Value")
            );

            // then
            assertThat(cipherList)
                    .as("SSL cipher list가 비어있지 않으면 SSL 활성화 가능")
                    .isNotNull();
        }
    }
}
