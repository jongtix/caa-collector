package com.custom.trader.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketCodeTest {

    @Test
    @DisplayName("fromExcdOrDefault: KRX -> KRX")
    void fromExcdOrDefault_성공() {
        assertThat(MarketCode.fromExcdOrDefault("KRX", MarketCode.KRX)).isEqualTo(MarketCode.KRX);
    }

    @Test
    @DisplayName("fromExcdOrDefault: NAS -> NAS")
    void fromExcdOrDefault_NAS() {
        assertThat(MarketCode.fromExcdOrDefault("NAS", MarketCode.KRX)).isEqualTo(MarketCode.NAS);
    }

    @Test
    @DisplayName("fromExcdOrDefault: null -> 기본값")
    void fromExcdOrDefault_null_기본값() {
        assertThat(MarketCode.fromExcdOrDefault(null, MarketCode.KRX)).isEqualTo(MarketCode.KRX);
    }

    @Test
    @DisplayName("fromExcdOrDefault: 빈 문자열 -> 기본값")
    void fromExcdOrDefault_빈값_기본값() {
        assertThat(MarketCode.fromExcdOrDefault("", MarketCode.KRX)).isEqualTo(MarketCode.KRX);
    }

    @Test
    @DisplayName("fromExcdOrDefault: 알 수 없는 값 -> 기본값")
    void fromExcdOrDefault_알수없는값_기본값() {
        assertThat(MarketCode.fromExcdOrDefault("UNKNOWN", MarketCode.NAS)).isEqualTo(MarketCode.NAS);
    }
}
