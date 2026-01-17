package com.custom.trader.kis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "kis")
public record KisProperties(
    String baseUrl,
    String userId,
    List<KisAccountProperties> accounts
) {}
