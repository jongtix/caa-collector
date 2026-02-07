package com.custom.trader.testcontainers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers MySQL 설정
 *
 * <p>모든 통합 테스트에서 공통으로 사용할 MySQL 컨테이너를 정의한다.
 * Spring Boot 3.1+의 @ServiceConnection을 활용하여
 * 자동으로 DataSource 설정을 주입한다.</p>
 *
 * <h3>사용 방법</h3>
 * <pre>{@code
 * @SpringBootTest
 * @Import(MySQLTestcontainersConfig.class)
 * class MyIntegrationTest {
 *     // 테스트 코드
 * }
 * }</pre>
 *
 * <h3>주요 특징</h3>
 * <ul>
 *   <li>MySQL 8.0 이미지 사용</li>
 *   <li>컨테이너 재사용 활성화 (성능 최적화)</li>
 *   <li>Spring Boot의 @ServiceConnection으로 자동 연결</li>
 * </ul>
 */
@TestConfiguration(proxyBeanMethods = false)
public class MySQLTestcontainersConfig {

    /**
     * MySQL 테스트 컨테이너
     *
     * <p>@ServiceConnection 어노테이션으로 인해 Spring Boot가 자동으로
     * DataSource, JPA 설정을 구성한다.</p>
     *
     * @return MySQL 컨테이너 인스턴스
     */
    @Bean
    @ServiceConnection
    public MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withReuse(true)  // 컨테이너 재사용으로 테스트 속도 향상
                .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");
    }
}
