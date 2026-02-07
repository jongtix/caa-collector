package com.custom.trader.config;

import com.custom.trader.testcontainers.MySQLTestcontainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RestClientConfig 프로필별 LoggingInterceptor 등록 테스트
 *
 * <p>local, dev 프로필에서는 LoggingInterceptor가 등록되고,
 * prod 프로필에서는 등록되지 않는지 검증한다.</p>
 */
class RestClientConfigProfileTest {

    /**
     * local 프로필 테스트
     */
    @SpringBootTest
    @ActiveProfiles({"log-local", "db-test-override"})
    @Import(MySQLTestcontainersConfig.class)
    @DisplayName("local 프로필에서 LoggingInterceptor 등록됨")
    static class LocalProfileTest {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        @DisplayName("local 프로필에서 LoggingInterceptor 빈이 등록됨")
        void local_프로필_LoggingInterceptor_등록됨() {
            // given
            var interceptors = applicationContext.getBeansOfType(ClientHttpRequestInterceptor.class);

            // when
            var loggingInterceptorExists = interceptors.values().stream()
                    .anyMatch(interceptor -> interceptor instanceof LoggingInterceptor);

            // then
            assertThat(loggingInterceptorExists)
                    .as("local 프로필에서 LoggingInterceptor가 등록되어야 함")
                    .isTrue();
        }
    }

    /**
     * dev 프로필 테스트
     */
    @SpringBootTest
    @ActiveProfiles({"log-dev", "db-test-override"})
    @Import(MySQLTestcontainersConfig.class)
    @DisplayName("dev 프로필에서 LoggingInterceptor 등록됨")
    static class DevProfileTest {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        @DisplayName("dev 프로필에서 LoggingInterceptor 빈이 등록됨")
        void dev_프로필_LoggingInterceptor_등록됨() {
            // given
            var interceptors = applicationContext.getBeansOfType(ClientHttpRequestInterceptor.class);

            // when
            var loggingInterceptorExists = interceptors.values().stream()
                    .anyMatch(interceptor -> interceptor instanceof LoggingInterceptor);

            // then
            assertThat(loggingInterceptorExists)
                    .as("dev 프로필에서 LoggingInterceptor가 등록되어야 함")
                    .isTrue();
        }
    }

    /**
     * prod 프로필 테스트
     */
    @SpringBootTest
    @ActiveProfiles({"log-prod", "prod-test"})
    @Import(MySQLTestcontainersConfig.class)
    @DisplayName("prod 프로필에서 LoggingInterceptor 등록 안 됨")
    static class ProdProfileTest {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        @DisplayName("prod 프로필에서 LoggingInterceptor 빈이 등록 안 됨")
        void prod_프로필_LoggingInterceptor_등록안됨() {
            // given
            var interceptors = applicationContext.getBeansOfType(ClientHttpRequestInterceptor.class);

            // when
            var loggingInterceptorExists = interceptors.values().stream()
                    .anyMatch(interceptor -> interceptor instanceof LoggingInterceptor);

            // then
            assertThat(loggingInterceptorExists)
                    .as("prod 프로필에서 LoggingInterceptor가 등록되지 않아야 함")
                    .isFalse();
        }
    }
}
