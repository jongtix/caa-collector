package com.custom.trader.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetTypeTest {

    @Test
    @DisplayName("fromFidMrktClsCode: J -> DOMESTIC_STOCK")
    void fromFidMrktClsCode_J_국내주식() {
        assertThat(AssetType.fromFidMrktClsCode("J")).isEqualTo(AssetType.DOMESTIC_STOCK);
    }

    @Test
    @DisplayName("fromFidMrktClsCode: UN -> DOMESTIC_STOCK")
    void fromFidMrktClsCode_UN_국내주식() {
        assertThat(AssetType.fromFidMrktClsCode("UN")).isEqualTo(AssetType.DOMESTIC_STOCK);
    }

    @Test
    @DisplayName("fromFidMrktClsCode: U -> DOMESTIC_INDEX")
    void fromFidMrktClsCode_U_국내지수() {
        assertThat(AssetType.fromFidMrktClsCode("U")).isEqualTo(AssetType.DOMESTIC_INDEX);
    }

    @Test
    @DisplayName("fromFidMrktClsCode: N -> OVERSEAS_INDEX")
    void fromFidMrktClsCode_N_해외지수() {
        assertThat(AssetType.fromFidMrktClsCode("N")).isEqualTo(AssetType.OVERSEAS_INDEX);
    }

    @Test
    @DisplayName("fromFidMrktClsCode: FS -> OVERSEAS_STOCK")
    void fromFidMrktClsCode_FS_해외주식() {
        assertThat(AssetType.fromFidMrktClsCode("FS")).isEqualTo(AssetType.OVERSEAS_STOCK);
    }

    @Test
    @DisplayName("fromFidMrktClsCode: null -> DOMESTIC_STOCK (기본값)")
    void fromFidMrktClsCode_null_기본값() {
        assertThat(AssetType.fromFidMrktClsCode(null)).isEqualTo(AssetType.DOMESTIC_STOCK);
    }

    @Test
    @DisplayName("fromFidMrktClsCode: 알 수 없는 값 -> DOMESTIC_STOCK (기본값)")
    void fromFidMrktClsCode_알수없는값_기본값() {
        assertThat(AssetType.fromFidMrktClsCode("UNKNOWN")).isEqualTo(AssetType.DOMESTIC_STOCK);
    }
}
