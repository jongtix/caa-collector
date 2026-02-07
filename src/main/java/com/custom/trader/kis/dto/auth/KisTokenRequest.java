package com.custom.trader.kis.dto.auth;

import com.custom.trader.common.util.LogMaskingUtil;

public record KisTokenRequest(
    String grantType,
    String appkey,
    String appsecret
) {
    public static KisTokenRequest of(String appKey, String appSecret) {
        return new KisTokenRequest("client_credentials", appKey, appSecret);
    }

    @Override
    public String toString() {
        return "KisTokenRequest[" +
            "grantType=" + grantType +
            ", appkey=" + LogMaskingUtil.maskAppKey(appkey) +
            ", appsecret=" + LogMaskingUtil.maskAppSecret(appsecret) +
            "]";
    }
}
