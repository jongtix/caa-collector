package com.custom.trader.common.converter;

import com.custom.trader.common.enums.MarketCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MarketCodeConverter 단위 테스트.
 *
 * <p>JPA AttributeConverter의 양방향 변환 로직을 검증합니다.</p>
 */
@DisplayName("MarketCodeConverter 단위 테스트")
class MarketCodeConverterTest {

    private MarketCodeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MarketCodeConverter();
    }

    @Test
    @DisplayName("Entity → DB: null 입력 시 null 반환")
    void convertToDatabaseColumn_Null() {
        // When
        Integer result = converter.convertToDatabaseColumn(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Entity → DB: MarketCode.KRX를 1로 변환")
    void convertToDatabaseColumn_KRX() {
        // When
        Integer result = converter.convertToDatabaseColumn(MarketCode.KRX);

        // Then
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("Entity → DB: MarketCode.NAS를 11로 변환")
    void convertToDatabaseColumn_NAS() {
        // When
        Integer result = converter.convertToDatabaseColumn(MarketCode.NAS);

        // Then
        assertThat(result).isEqualTo(11);
    }

    @Test
    @DisplayName("DB → Entity: null 입력 시 null 반환")
    void convertToEntityAttribute_Null() {
        // When
        MarketCode result = converter.convertToEntityAttribute(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("DB → Entity: 1을 MarketCode.KRX로 변환")
    void convertToEntityAttribute_Code1() {
        // When
        MarketCode result = converter.convertToEntityAttribute(1);

        // Then
        assertThat(result).isEqualTo(MarketCode.KRX);
    }

    @Test
    @DisplayName("DB → Entity: 11을 MarketCode.NAS로 변환")
    void convertToEntityAttribute_Code11() {
        // When
        MarketCode result = converter.convertToEntityAttribute(11);

        // Then
        assertThat(result).isEqualTo(MarketCode.NAS);
    }

    @Test
    @DisplayName("양방향 변환: MarketCode → Integer → MarketCode 일관성 보장")
    void roundTrip_AllMarketCodes() {
        // Given
        MarketCode[] allCodes = MarketCode.values();

        for (MarketCode original : allCodes) {
            // When
            Integer dbValue = converter.convertToDatabaseColumn(original);
            MarketCode restored = converter.convertToEntityAttribute(dbValue);

            // Then
            assertThat(restored)
                .as("MarketCode [%s] 양방향 변환 일관성", original.name())
                .isEqualTo(original);
        }
    }
}
