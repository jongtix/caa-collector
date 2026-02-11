package com.custom.trader.watchlist.entity;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WatchlistStock Entity")
class WatchlistStockTest {

    @Nested
    @DisplayName("equals/hashCode 검증")
    class EqualsAndHashCode {

        @Test
        @DisplayName("자기_자신과_비교_시_true를_반환한다")
        void 자기_자신과_비교_시_true를_반환한다() {
            // Given
            WatchlistStock stock = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();

            // When & Then
            assertThat(stock).isEqualTo(stock);
        }

        @Test
        @DisplayName("null과_비교_시_false를_반환한다")
        void null과_비교_시_false를_반환한다() {
            // Given
            WatchlistStock stock = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();

            // When & Then
            assertThat(stock).isNotEqualTo(null);
        }

        @Test
        @DisplayName("id가_null인_두_인스턴스는_서로_다르다")
        void id가_null인_두_인스턴스는_서로_다르다() {
            // Given
            WatchlistStock stock1 = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();

            WatchlistStock stock2 = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();

            // When & Then
            assertThat(stock1.getId()).isNull();
            assertThat(stock2.getId()).isNull();
            assertThat(stock1).isNotEqualTo(stock2);
        }

        @Test
        @DisplayName("동일한_id를_가진_인스턴스는_동등하다")
        void 동일한_id를_가진_인스턴스는_동등하다() {
            // Given
            WatchlistStock stock1 = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();
            ReflectionTestUtils.setField(stock1, "id", 1L);

            WatchlistStock stock2 = WatchlistStock.builder()
                .stockCode("000660")
                .stockName("SK하이닉스")
                .marketCode(MarketCode.NYS)
                .assetType(AssetType.OVERSEAS_STOCK)
                .build();
            ReflectionTestUtils.setField(stock2, "id", 1L);

            // When & Then
            assertThat(stock1).isEqualTo(stock2);
        }

        @Test
        @DisplayName("다른_id를_가진_인스턴스는_동등하지_않다")
        void 다른_id를_가진_인스턴스는_동등하지_않다() {
            // Given
            WatchlistStock stock1 = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();
            ReflectionTestUtils.setField(stock1, "id", 1L);

            WatchlistStock stock2 = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();
            ReflectionTestUtils.setField(stock2, "id", 2L);

            // When & Then
            assertThat(stock1).isNotEqualTo(stock2);
        }

        @Test
        @DisplayName("hashCode는_클래스_타입에_의존하여_일관성을_유지한다")
        void hashCode는_클래스_타입에_의존하여_일관성을_유지한다() {
            // Given
            WatchlistStock stock1 = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();
            ReflectionTestUtils.setField(stock1, "id", 1L);

            WatchlistStock stock2 = WatchlistStock.builder()
                .stockCode("000660")
                .stockName("SK하이닉스")
                .marketCode(MarketCode.NYS)
                .assetType(AssetType.OVERSEAS_STOCK)
                .build();
            ReflectionTestUtils.setField(stock2, "id", 2L);

            // When & Then
            assertThat(stock1.hashCode()).isEqualTo(stock2.hashCode());
        }

        @Test
        @DisplayName("id가_null인_인스턴스도_hashCode를_반환한다")
        void id가_null인_인스턴스도_hashCode를_반환한다() {
            // Given
            WatchlistStock stock = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();

            // When & Then
            assertThat(stock.getId()).isNull();
            assertThat(stock.hashCode()).isNotNull();
        }
    }
}
