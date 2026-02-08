package com.custom.trader.config;

import com.custom.trader.kis.config.KisProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(KisProperties.class)
public class RestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    /**
     * HTTP 요청/응답 로깅 인터셉터 (local, dev 환경 전용).
     *
     * <p>개발 환경에서만 등록되어 디버깅을 지원하며,
     * 운영 환경(prod)에서는 등록하지 않아 성능 영향을 최소화합니다.</p>
     *
     * @return LoggingInterceptor 인스턴스
     */
    @Bean
    @Profile({"log-local", "log-dev"})
    public LoggingInterceptor loggingInterceptor() {
        return new LoggingInterceptor();
    }

    /**
     * 한국투자증권 API용 RestClient 빈.
     *
     * <p>local, dev 환경에서는 LoggingInterceptor가 자동 주입되어 등록되며,
     * prod 환경에서는 인터셉터 없이 RestClient가 생성됩니다.</p>
     *
     * @param kisProperties 한국투자증권 API 설정
     * @param builder RestClient.Builder
     * @param loggingInterceptorProvider HTTP 로깅 인터셉터 제공자 (선택적, local/dev만)
     * @return 설정된 RestClient 인스턴스
     */
    @Bean
    public RestClient kisApiRestClient(
            KisProperties kisProperties,
            RestClient.Builder builder,
            ObjectProvider<LoggingInterceptor> loggingInterceptorProvider
    ) {
        // HttpClient 생성 (connectTimeout 설정)
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        // JdkClientHttpRequestFactory 생성 (readTimeout 및 HttpClient 설정)
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        var clientBuilder = builder
                .baseUrl(kisProperties.baseUrl())
                .requestFactory(requestFactory);

        // local, dev 환경에서만 인터셉터 등록
        loggingInterceptorProvider.ifAvailable(clientBuilder::requestInterceptor);

        return clientBuilder.build();
    }
}
