package com.custom.trader.stockprice.strategy;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
import com.custom.trader.kis.dto.stockprice.OverseasStockDailyPriceResponse;
import com.custom.trader.kis.exception.KisApiException;
import com.custom.trader.kis.service.KisStockPriceService;
import com.custom.trader.stockprice.service.StockPricePersistenceService;
import com.custom.trader.watchlist.entity.WatchlistStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OverseasStockStrategyTest {

    @Mock
    private KisStockPriceService kisStockPriceService;

    @Mock
    private StockPricePersistenceService persistenceService;

    @InjectMocks
    private OverseasStockStrategy strategy;

    private WatchlistStock overseasStock;
    private LocalDate startDate;
    private LocalDate endDate;
    private String exchangeCode;

    @BeforeEach
    void setUp() {
        exchangeCode = "NAS";
        overseasStock = WatchlistStock.builder()
                .stockCode("AAPL")
                .stockName("Apple Inc.")
                .assetType(AssetType.OVERSEAS_STOCK)
                .marketCode(MarketCode.NAS)
                .build();

        startDate = LocalDate.of(2024, 1, 1);
        endDate = LocalDate.of(2024, 1, 31);
    }

    @Nested
    @DisplayName("collectDailyPrice 메소드")
    class CollectDailyPrice {

        @Test
        @DisplayName("정상적으로 일간 가격을 수집하고 저장")
        void 정상적으로_일간_가격을_수집하고_저장() {
            // given
            var priceItems = List.of(
                    new OverseasStockDailyPriceResponse.PriceItem(
                            "20240131", "151.25", "150.50", "152.00", "149.00", "50000000", "7500000000", "151.20", "151.30"
                    )
            );
            given(kisStockPriceService.getOverseasStockDailyPrices(
                    eq("AAPL"), eq(exchangeCode), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveOverseasStockPrices(eq("AAPL"), eq(exchangeCode), eq(priceItems)))
                    .willReturn(1);

            // when
            int saved = strategy.collectDailyPrice(overseasStock, startDate, endDate);

            // then
            assertThat(saved).isEqualTo(1);
            verify(kisStockPriceService).getOverseasStockDailyPrices("AAPL", exchangeCode, startDate, endDate);
            verify(persistenceService).saveOverseasStockPrices("AAPL", exchangeCode, priceItems);
        }

        @Test
        @DisplayName("빈 데이터 반환시 0 저장")
        void 빈_데이터_반환시_0_저장() {
            // given
            given(kisStockPriceService.getOverseasStockDailyPrices(
                    eq("AAPL"), eq(exchangeCode), any(LocalDate.class), any(LocalDate.class))
            ).willReturn(Collections.emptyList());
            given(persistenceService.saveOverseasStockPrices(eq("AAPL"), eq(exchangeCode), any()))
                    .willReturn(0);

            // when
            int saved = strategy.collectDailyPrice(overseasStock, startDate, endDate);

            // then
            assertThat(saved).isZero();
        }

        @Test
        @DisplayName("KisApiException 발생시 예외 전파")
        void KisApiException_발생시_예외_전파() {
            // given
            given(kisStockPriceService.getOverseasStockDailyPrices(
                    eq("AAPL"), eq(exchangeCode), any(LocalDate.class), any(LocalDate.class))
            ).willThrow(new KisApiException("API 호출 실패"));

            // when & then
            assertThatThrownBy(() -> strategy.collectDailyPrice(overseasStock, startDate, endDate))
                    .isInstanceOf(KisApiException.class)
                    .hasMessage("API 호출 실패");
        }

        @Test
        @DisplayName("다수의 가격 데이터를 수집하고 저장")
        void 다수의_가격_데이터를_수집하고_저장() {
            // given
            var priceItems = List.of(
                    new OverseasStockDailyPriceResponse.PriceItem(
                            "20240131", "151.25", "150.50", "152.00", "149.00", "50000000", "7500000000", "151.20", "151.30"
                    ),
                    new OverseasStockDailyPriceResponse.PriceItem(
                            "20240130", "150.00", "149.00", "150.50", "148.00", "48000000", "7200000000", "149.95", "150.05"
                    ),
                    new OverseasStockDailyPriceResponse.PriceItem(
                            "20240129", "148.75", "148.00", "149.00", "147.00", "46000000", "6840000000", "148.70", "148.80"
                    )
            );
            given(kisStockPriceService.getOverseasStockDailyPrices(
                    eq("AAPL"), eq(exchangeCode), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveOverseasStockPrices(eq("AAPL"), eq(exchangeCode), eq(priceItems)))
                    .willReturn(3);

            // when
            int saved = strategy.collectDailyPrice(overseasStock, startDate, endDate);

            // then
            assertThat(saved).isEqualTo(3);
        }

        @Test
        @DisplayName("ExchangeCode 조회 검증")
        void ExchangeCode_조회_검증() {
            // given
            var priceItems = List.of(
                    new OverseasStockDailyPriceResponse.PriceItem(
                            "20240131", "151.25", "150.50", "152.00", "149.00", "50000000", "7500000000", "151.20", "151.30"
                    )
            );
            given(kisStockPriceService.getOverseasStockDailyPrices(
                    eq("AAPL"), eq("NAS"), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveOverseasStockPrices(eq("AAPL"), eq("NAS"), eq(priceItems)))
                    .willReturn(1);

            // when
            strategy.collectDailyPrice(overseasStock, startDate, endDate);

            // then
            verify(kisStockPriceService).getOverseasStockDailyPrices("AAPL", "NAS", startDate, endDate);
        }
    }

    @Nested
    @DisplayName("backfillHistoricalPrices 메소드")
    class BackfillHistoricalPrices {

        @Test
        @DisplayName("정상적으로 백필 수집")
        void 정상적으로_백필_수집() {
            // given
            var priceItems = List.of(
                    new OverseasStockDailyPriceResponse.PriceItem(
                            "20240131", "151.25", "150.50", "152.00", "149.00", "50000000", "7500000000", "151.20", "151.30"
                    )
            );
            given(kisStockPriceService.getOverseasStockDailyPrices(
                    eq("AAPL"), eq(exchangeCode), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveOverseasStockPrices(eq("AAPL"), eq(exchangeCode), eq(priceItems)))
                    .willReturn(1);

            // when
            strategy.backfillHistoricalPrices(overseasStock, startDate, endDate);

            // then
            verify(kisStockPriceService).getOverseasStockDailyPrices("AAPL", exchangeCode, startDate, endDate);
            verify(persistenceService).saveOverseasStockPrices("AAPL", exchangeCode, priceItems);
        }

        @Test
        @DisplayName("빈 데이터 반환시 백필 중단")
        void 빈_데이터_반환시_백필_중단() {
            // given
            given(kisStockPriceService.getOverseasStockDailyPrices(
                    eq("AAPL"), eq(exchangeCode), any(LocalDate.class), any(LocalDate.class))
            ).willReturn(Collections.emptyList());

            // when
            strategy.backfillHistoricalPrices(overseasStock, startDate, endDate);

            // then
            verify(kisStockPriceService).getOverseasStockDailyPrices("AAPL", exchangeCode, startDate, endDate);
        }

        @Test
        @DisplayName("KisApiException 발생시 예외 전파")
        void KisApiException_발생시_예외_전파() {
            // given
            given(kisStockPriceService.getOverseasStockDailyPrices(
                    eq("AAPL"), eq(exchangeCode), any(LocalDate.class), any(LocalDate.class))
            ).willThrow(new KisApiException("API 호출 실패"));

            // when & then
            assertThatThrownBy(() -> strategy.backfillHistoricalPrices(overseasStock, startDate, endDate))
                    .isInstanceOf(KisApiException.class)
                    .hasMessage("API 호출 실패");
        }
    }

    @Nested
    @DisplayName("Strategy 어노테이션 검증")
    class AnnotationVerification {

        @Test
        @DisplayName("@Component 어노테이션 존재 확인")
        void Component_어노테이션_존재() {
            // when
            var annotation = OverseasStockStrategy.class
                    .getAnnotation(org.springframework.stereotype.Component.class);

            // then
            assertThat(annotation).isNotNull();
        }
    }
}
