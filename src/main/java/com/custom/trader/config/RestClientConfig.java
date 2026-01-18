package com.custom.trader.config;

import com.custom.trader.kis.config.KisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(KisProperties.class)
public class RestClientConfig {

    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    @Bean
    public RestClient kisApiRestClient(KisProperties kisProperties, RestClient.Builder builder) {
        var requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return builder
                .baseUrl(kisProperties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
