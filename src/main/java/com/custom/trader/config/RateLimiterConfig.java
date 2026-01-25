package com.custom.trader.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("UnstableApiUsage")
@Configuration
public class RateLimiterConfig {

    private static final double PERMITS_PER_SECOND = 20.0;

    @Bean
    public RateLimiter kisApiRateLimiter() {
        return RateLimiter.create(PERMITS_PER_SECOND);
    }
}
