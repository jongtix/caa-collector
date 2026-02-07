package com.custom.trader.stockprice.strategy;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
import com.custom.trader.kis.dto.stockprice.DomesticIndexDailyPriceResponse;
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
class DomesticIndexStrategyTest {

    @Mock
    private KisStockPriceService kisStockPriceService;

    @Mock
    private StockPricePersistenceService persistenceService;

    @InjectMocks
    private DomesticIndexStrategy strategy;

    private WatchlistStock domesticIndex;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        domesticIndex = WatchlistStock.builder()
                .stockCode("0001")
                .stockName("코스피")
                .assetType(AssetType.DOMESTIC_INDEX)
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
                    new DomesticIndexDailyPriceResponse.PriceItem(
                            "20240131", "2500.50", "2520.00", "2490.00", "2510.25", "500000000", "75000000000"
                    )
            );
            given(kisStockPriceService.getDomesticIndexDailyPrices(
                    eq("0001"), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveDomesticIndexPrices(eq("0001"), eq(priceItems)))
                    .willReturn(1);

            // when
            int saved = strategy.collectDailyPrice(domesticIndex, startDate, endDate);

            // then
            assertThat(saved).isEqualTo(1);
            verify(kisStockPriceService).getDomesticIndexDailyPrices("0001", startDate, endDate);
            verify(persistenceService).saveDomesticIndexPrices("0001", priceItems);
        }

        @Test
        @DisplayName("빈 데이터 반환시 0 저장")
        void 빈_데이터_반환시_0_저장() {
            // given
            given(kisStockPriceService.getDomesticIndexDailyPrices(
                    eq("0001"), any(LocalDate.class), any(LocalDate.class))
            ).willReturn(Collections.emptyList());
            given(persistenceService.saveDomesticIndexPrices(eq("0001"), any()))
                    .willReturn(0);

            // when
            int saved = strategy.collectDailyPrice(domesticIndex, startDate, endDate);

            // then
            assertThat(saved).isZero();
        }

        @Test
        @DisplayName("KisApiException 발생시 예외 전파")
        void KisApiException_발생시_예외_전파() {
            // given
            given(kisStockPriceService.getDomesticIndexDailyPrices(
                    eq("0001"), any(LocalDate.class), any(LocalDate.class))
            ).willThrow(new KisApiException("API 호출 실패"));

            // when & then
            assertThatThrownBy(() -> strategy.collectDailyPrice(domesticIndex, startDate, endDate))
                    .isInstanceOf(KisApiException.class)
                    .hasMessage("API 호출 실패");
        }

        @Test
        @DisplayName("다수의 가격 데이터를 수집하고 저장")
        void 다수의_가격_데이터를_수집하고_저장() {
            // given
            var priceItems = List.of(
                    new DomesticIndexDailyPriceResponse.PriceItem(
                            "20240131", "2500.50", "2520.00", "2490.00", "2510.25", "500000000", "75000000000"
                    ),
                    new DomesticIndexDailyPriceResponse.PriceItem(
                            "20240130", "2490.00", "2500.00", "2480.00", "2495.50", "480000000", "70000000000"
                    ),
                    new DomesticIndexDailyPriceResponse.PriceItem(
                            "20240129", "2480.00", "2490.00", "2470.00", "2485.75", "460000000", "68000000000"
                    )
            );
            given(kisStockPriceService.getDomesticIndexDailyPrices(
                    eq("0001"), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveDomesticIndexPrices(eq("0001"), eq(priceItems)))
                    .willReturn(3);

            // when
            int saved = strategy.collectDailyPrice(domesticIndex, startDate, endDate);

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
                    new DomesticIndexDailyPriceResponse.PriceItem(
                            "20240131", "2500.50", "2520.00", "2490.00", "2510.25", "500000000", "75000000000"
                    )
            );
            given(kisStockPriceService.getDomesticIndexDailyPrices(
                    eq("0001"), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveDomesticIndexPrices(eq("0001"), eq(priceItems)))
                    .willReturn(1);

            // when
            strategy.backfillHistoricalPrices(domesticIndex, startDate, endDate);

            // then
            verify(kisStockPriceService).getDomesticIndexDailyPrices("0001", startDate, endDate);
            verify(persistenceService).saveDomesticIndexPrices("0001", priceItems);
        }

        @Test
        @DisplayName("빈 데이터 반환시 백필 중단")
        void 빈_데이터_반환시_백필_중단() {
            // given
            given(kisStockPriceService.getDomesticIndexDailyPrices(
                    eq("0001"), any(LocalDate.class), any(LocalDate.class))
            ).willReturn(Collections.emptyList());

            // when
            strategy.backfillHistoricalPrices(domesticIndex, startDate, endDate);

            // then
            verify(kisStockPriceService).getDomesticIndexDailyPrices("0001", startDate, endDate);
        }

        @Test
        @DisplayName("KisApiException 발생시 예외 전파")
        void KisApiException_발생시_예외_전파() {
            // given
            given(kisStockPriceService.getDomesticIndexDailyPrices(
                    eq("0001"), any(LocalDate.class), any(LocalDate.class))
            ).willThrow(new KisApiException("API 호출 실패"));

            // when & then
            assertThatThrownBy(() -> strategy.backfillHistoricalPrices(domesticIndex, startDate, endDate))
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
            var annotation = DomesticIndexStrategy.class
                    .getAnnotation(org.springframework.stereotype.Component.class);

            // then
            assertThat(annotation).isNotNull();
        }
    }
}
