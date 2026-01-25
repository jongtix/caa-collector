package com.custom.trader.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum MarketCode {

    KRX(1, "KRX"),
    NYS(10, "NYS"),
    NAS(11, "NAS"),
    AMS(12, "AMS"),
    TSE(13, "TSE"),
    HKS(14, "HKS"),
    SHS(15, "SHS"),
    SZS(16, "SZS"),
    HSX(17, "HSX"),
    HNX(18, "HNX");

    private final int code;
    private final String excd;

    public static MarketCode fromCode(int code) {
        return Arrays.stream(values())
                .filter(market -> market.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown MarketCode code: " + code));
    }

    public static MarketCode fromExcd(String excd) {
        return Arrays.stream(values())
                .filter(market -> market.excd.equalsIgnoreCase(excd))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown MarketCode excd: " + excd));
    }

    public static MarketCode fromExcdOrDefault(String excd, MarketCode defaultValue) {
        if (excd == null || excd.isBlank()) {
            return defaultValue;
        }
        try {
            return fromExcd(excd);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown exchange code: {}, defaulting to {}", excd, defaultValue);
            return defaultValue;
        }
    }
}
