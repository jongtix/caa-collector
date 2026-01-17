package com.custom.trader.config;

import com.custom.trader.kis.config.KisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KisProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient kisRestClient(KisProperties kisProperties, RestClient.Builder builder) {
        return builder
                .baseUrl(kisProperties.baseUrl())
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();
    }
}
