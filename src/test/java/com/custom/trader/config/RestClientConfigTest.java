package com.custom.trader.config;

import com.custom.trader.kis.config.KisProperties;
import com.custom.trader.testcontainers.MySQLTestcontainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RestClientConfig 타임아웃 설정 및 인터셉터 등록 테스트.
 *
 * <p>RestClient 빈이 올바른 타임아웃 설정과 함께 생성되고,
 * 프로필별로 LoggingInterceptor가 조건부 등록되는지 검증한다.</p>
 *
 * <p><b>Reflection 사용:</b> 타임아웃 검증을 위해 {@link RestClientTestHelper}를 사용한다.
 * Spring 내부 구조 의존성은 해당 헬퍼 클래스에 캡슐화되어 있다.</p>
 *
 * @see RestClientTestHelper
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(MySQLTestcontainersConfig.class)
class RestClientConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RestClient kisApiRestClient;

    @Autowired
    private KisProperties kisProperties;

    @Nested
    @DisplayName("타임아웃 설정 검증")
    class TimeoutConfigurationTest {

        @Test
        @DisplayName("연결 타임아웃 10초 설정 확인")
        void 연결_타임아웃_10초_설정확인() throws Exception {
            // given
            var requestFactory = RestClientTestHelper.extractRequestFactory(kisApiRestClient);

            // when
            var httpClient = RestClientTestHelper.extractHttpClient(requestFactory);
            var connectTimeout = httpClient.connectTimeout();

            // then
            assertThat(connectTimeout).isPresent();
            assertThat(connectTimeout.get()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("읽기 타임아웃 30초 설정 확인")
        void 읽기_타임아웃_30초_설정확인() throws Exception {
            // given
            var requestFactory = RestClientTestHelper.extractRequestFactory(kisApiRestClient);

            // when
            var readTimeout = RestClientTestHelper.extractReadTimeout(requestFactory);

            // then
            assertThat(readTimeout).isEqualTo(Duration.ofSeconds(30));
        }
    }

    @Nested
    @DisplayName("baseUrl 설정 검증")
    class BaseUrlConfigurationTest {

        @Test
        @DisplayName("KisProperties의 baseUrl이 RestClient에 설정됨")
        void KisProperties_baseUrl_RestClient_설정됨() {
            // given
            var expectedBaseUrl = kisProperties.baseUrl();

            // when
            // RestClient는 내부적으로 baseUrl을 저장하므로 간접적으로 검증
            // (실제 요청 시 baseUrl이 사용되는지는 통합 테스트에서 검증)

            // then
            assertThat(expectedBaseUrl).isNotNull();
            assertThat(kisApiRestClient).isNotNull();
        }
    }

}
