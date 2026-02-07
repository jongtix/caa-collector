package com.custom.trader.stockprice.strategy;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
import com.custom.trader.kis.dto.stockprice.DomesticStockDailyPriceResponse;
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
class DomesticStockStrategyTest {

    @Mock
    private KisStockPriceService kisStockPriceService;

    @Mock
    private StockPricePersistenceService persistenceService;

    @InjectMocks
    private DomesticStockStrategy strategy;

    private WatchlistStock domesticStock;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        domesticStock = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .assetType(AssetType.DOMESTIC_STOCK)
                .marketCode(MarketCode.KRX)
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
                    new DomesticStockDailyPriceResponse.PriceItem(
                            "20240131", "75000", "76000", "74000", "75500", "1000000", "75000000000"
                    )
            );
            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveDomesticStockPrices(eq("005930"), eq(priceItems)))
                    .willReturn(1);

            // when
            int saved = strategy.collectDailyPrice(domesticStock, startDate, endDate);

            // then
            assertThat(saved).isEqualTo(1);
            verify(kisStockPriceService).getDomesticStockDailyPrices("005930", startDate, endDate);
            verify(persistenceService).saveDomesticStockPrices("005930", priceItems);
        }

        @Test
        @DisplayName("빈 데이터 반환시 0 저장")
        void 빈_데이터_반환시_0_저장() {
            // given
            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), any(LocalDate.class), any(LocalDate.class))
            ).willReturn(Collections.emptyList());
            given(persistenceService.saveDomesticStockPrices(eq("005930"), any()))
                    .willReturn(0);

            // when
            int saved = strategy.collectDailyPrice(domesticStock, startDate, endDate);

            // then
            assertThat(saved).isZero();
        }

        @Test
        @DisplayName("KisApiException 발생시 예외 전파")
        void KisApiException_발생시_예외_전파() {
            // given
            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), any(LocalDate.class), any(LocalDate.class))
            ).willThrow(new KisApiException("API 호출 실패"));

            // when & then
            assertThatThrownBy(() -> strategy.collectDailyPrice(domesticStock, startDate, endDate))
                    .isInstanceOf(KisApiException.class)
                    .hasMessage("API 호출 실패");
        }

        @Test
        @DisplayName("다수의 가격 데이터를 수집하고 저장")
        void 다수의_가격_데이터를_수집하고_저장() {
            // given
            var priceItems = List.of(
                    new DomesticStockDailyPriceResponse.PriceItem(
                            "20240131", "75000", "76000", "74000", "75500", "1000000", "75000000000"
                    ),
                    new DomesticStockDailyPriceResponse.PriceItem(
                            "20240130", "74500", "75000", "74000", "74800", "900000", "67000000000"
                    ),
                    new DomesticStockDailyPriceResponse.PriceItem(
                            "20240129", "74000", "74500", "73500", "74200", "850000", "63000000000"
                    )
            );
            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveDomesticStockPrices(eq("005930"), eq(priceItems)))
                    .willReturn(3);

            // when
            int saved = strategy.collectDailyPrice(domesticStock, startDate, endDate);

            // then
            assertThat(saved).isEqualTo(3);
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
                    new DomesticStockDailyPriceResponse.PriceItem(
                            "20240131", "75000", "76000", "74000", "75500", "1000000", "75000000000"
                    )
            );
            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveDomesticStockPrices(eq("005930"), eq(priceItems)))
                    .willReturn(1);

            // when
            strategy.backfillHistoricalPrices(domesticStock, startDate, endDate);

            // then
            verify(kisStockPriceService).getDomesticStockDailyPrices("005930", startDate, endDate);
            verify(persistenceService).saveDomesticStockPrices("005930", priceItems);
        }

        @Test
        @DisplayName("빈 데이터 반환시 백필 중단")
        void 빈_데이터_반환시_백필_중단() {
            // given
            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), any(LocalDate.class), any(LocalDate.class))
            ).willReturn(Collections.emptyList());

            // when
            strategy.backfillHistoricalPrices(domesticStock, startDate, endDate);

            // then
            verify(kisStockPriceService).getDomesticStockDailyPrices("005930", startDate, endDate);
        }

        @Test
        @DisplayName("KisApiException 발생시 예외 전파")
        void KisApiException_발생시_예외_전파() {
            // given
            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), any(LocalDate.class), any(LocalDate.class))
            ).willThrow(new KisApiException("API 호출 실패"));

            // when & then
            assertThatThrownBy(() -> strategy.backfillHistoricalPrices(domesticStock, startDate, endDate))
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
            var annotation = DomesticStockStrategy.class
                    .getAnnotation(org.springframework.stereotype.Component.class);

            // then
            assertThat(annotation).isNotNull();
        }
    }
}
