package com.custom.trader.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum AssetType {

    DOMESTIC_STOCK(1),
    DOMESTIC_INDEX(2),
    OVERSEAS_STOCK(3),
    OVERSEAS_INDEX(4);

    private final int code;

    private static final Map<String, AssetType> FID_MARKET_CLASS_MAP = Map.of(
            "J", DOMESTIC_STOCK,
            "UN", DOMESTIC_STOCK,
            "U", DOMESTIC_INDEX,
            "N", OVERSEAS_INDEX,
            "FS", OVERSEAS_STOCK
    );

    public static AssetType fromCode(int code) {
        return Arrays.stream(values())
                .filter(type -> type.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown AssetType code: " + code));
    }

    public static AssetType fromFidMrktClsCode(String fidMrktClsCode) {
        if (fidMrktClsCode == null) {
            log.warn("fidMrktClsCode is null, defaulting to DOMESTIC_STOCK");
            return DOMESTIC_STOCK;
        }
        AssetType type = FID_MARKET_CLASS_MAP.get(fidMrktClsCode);
        if (type == null) {
            log.warn("Unknown fidMrktClsCode: {}, defaulting to DOMESTIC_STOCK", fidMrktClsCode);
            return DOMESTIC_STOCK;
        }
        return type;
    }
}
