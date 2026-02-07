package com.custom.trader.common.converter;

import com.custom.trader.common.enums.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssetTypeConverter 단위 테스트.
 *
 * <p>JPA AttributeConverter의 양방향 변환 로직을 검증합니다.</p>
 */
@DisplayName("AssetTypeConverter 단위 테스트")
class AssetTypeConverterTest {

    private AssetTypeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new AssetTypeConverter();
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
    @DisplayName("Entity → DB: AssetType.DOMESTIC_STOCK을 1로 변환")
    void convertToDatabaseColumn_DomesticStock() {
        // When
        Integer result = converter.convertToDatabaseColumn(AssetType.DOMESTIC_STOCK);

        // Then
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("Entity → DB: AssetType.OVERSEAS_INDEX를 4로 변환")
    void convertToDatabaseColumn_OverseasIndex() {
        // When
        Integer result = converter.convertToDatabaseColumn(AssetType.OVERSEAS_INDEX);

        // Then
        assertThat(result).isEqualTo(4);
    }

    @Test
    @DisplayName("DB → Entity: null 입력 시 null 반환")
    void convertToEntityAttribute_Null() {
        // When
        AssetType result = converter.convertToEntityAttribute(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("DB → Entity: 1을 AssetType.DOMESTIC_STOCK으로 변환")
    void convertToEntityAttribute_Code1() {
        // When
        AssetType result = converter.convertToEntityAttribute(1);

        // Then
        assertThat(result).isEqualTo(AssetType.DOMESTIC_STOCK);
    }

    @Test
    @DisplayName("DB → Entity: 4를 AssetType.OVERSEAS_INDEX로 변환")
    void convertToEntityAttribute_Code4() {
        // When
        AssetType result = converter.convertToEntityAttribute(4);

        // Then
        assertThat(result).isEqualTo(AssetType.OVERSEAS_INDEX);
    }

    @Test
    @DisplayName("양방향 변환: AssetType → Integer → AssetType 일관성 보장")
    void roundTrip_AllAssetTypes() {
        // Given
        AssetType[] allTypes = AssetType.values();

        for (AssetType original : allTypes) {
            // When
            Integer dbValue = converter.convertToDatabaseColumn(original);
            AssetType restored = converter.convertToEntityAttribute(dbValue);

            // Then
            assertThat(restored)
                .as("AssetType [%s] 양방향 변환 일관성", original.name())
                .isEqualTo(original);
        }
    }
}
