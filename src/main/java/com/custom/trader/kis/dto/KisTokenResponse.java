package com.custom.trader.kis.dto;

public record KisTokenResponse(
    String accessToken,
    String accessTokenTokenExpired,
    String tokenType,
    Long expiresIn
) {}
