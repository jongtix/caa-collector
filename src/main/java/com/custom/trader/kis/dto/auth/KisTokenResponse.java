package com.custom.trader.kis.dto.auth;

import com.custom.trader.common.util.LogMaskingUtil;

public record KisTokenResponse(
    String accessToken,
    String accessTokenTokenExpired,
    String tokenType,
    Long expiresIn
) {
    @Override
    public String toString() {
        return "KisTokenResponse[" +
            "accessToken=" + LogMaskingUtil.maskAccessToken(accessToken) +
            ", accessTokenTokenExpired=" + accessTokenTokenExpired +
            ", tokenType=" + tokenType +
            ", expiresIn=" + expiresIn +
            "]";
    }
}
