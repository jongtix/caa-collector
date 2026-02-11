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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.custom.trader.stockprice.constant.StockPriceConstants.PAGE_SIZE;
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

        @Test
        @DisplayName("2페이지 백필 시 커서 계산 검증")
        void 두페이지_백필_시_커서_계산_검증() {
            // given
            LocalDate backfillStart = LocalDate.of(2024, 1, 1);
            LocalDate backfillEnd = LocalDate.of(2024, 5, 31);

            // 첫 번째 페이지: 100개 (2024-05-31 ~ 2024-02-22)
            var firstPageItems = createPriceItems("20240531", 100);
            LocalDate firstPageLastDate = LocalDate.of(2024, 2, 22);
            LocalDate secondPageEndDate = LocalDate.of(2024, 2, 21);

            // 두 번째 페이지: 50개 (2024-02-21 ~ 2024-01-03)
            var secondPageItems = createPriceItems("20240221", 50);

            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), eq(backfillStart), eq(backfillEnd))
            ).willReturn(firstPageItems);

            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), eq(backfillStart), eq(secondPageEndDate))
            ).willReturn(secondPageItems);

            given(persistenceService.saveDomesticStockPrices(eq("005930"), any()))
                    .willReturn(100, 50);

            // when
            strategy.backfillHistoricalPrices(domesticStock, backfillStart, backfillEnd);

            // then
            verify(kisStockPriceService).getDomesticStockDailyPrices("005930", backfillStart, backfillEnd);
            verify(kisStockPriceService).getDomesticStockDailyPrices("005930", backfillStart, secondPageEndDate);
        }

        @Test
        @DisplayName("3페이지 백필 시 커서 누적 계산 검증")
        void 세페이지_백필_시_커서_누적_계산_검증() {
            // given
            LocalDate backfillStart = LocalDate.of(2024, 1, 1);
            LocalDate backfillEnd = LocalDate.of(2024, 8, 31);

            // 첫 번째 페이지: 100개 (2024-08-31 ~ 2024-05-24)
            var firstPageItems = createPriceItems("20240831", 100);
            LocalDate firstPageLastDate = LocalDate.of(2024, 5, 24);
            LocalDate secondPageEndDate = LocalDate.of(2024, 5, 23);

            // 두 번째 페이지: 100개 (2024-05-23 ~ 2024-02-14)
            var secondPageItems = createPriceItems("20240523", 100);
            LocalDate secondPageLastDate = LocalDate.of(2024, 2, 14);
            LocalDate thirdPageEndDate = LocalDate.of(2024, 2, 13);

            // 세 번째 페이지: 20개 (2024-02-13 ~ 2024-01-25)
            var thirdPageItems = createPriceItems("20240213", 20);

            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), eq(backfillStart), eq(backfillEnd))
            ).willReturn(firstPageItems);

            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), eq(backfillStart), eq(secondPageEndDate))
            ).willReturn(secondPageItems);

            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), eq(backfillStart), eq(thirdPageEndDate))
            ).willReturn(thirdPageItems);

            given(persistenceService.saveDomesticStockPrices(eq("005930"), any()))
                    .willReturn(100, 100, 20);

            // when
            strategy.backfillHistoricalPrices(domesticStock, backfillStart, backfillEnd);

            // then
            verify(kisStockPriceService).getDomesticStockDailyPrices("005930", backfillStart, backfillEnd);
            verify(kisStockPriceService).getDomesticStockDailyPrices("005930", backfillStart, secondPageEndDate);
            verify(kisStockPriceService).getDomesticStockDailyPrices("005930", backfillStart, thirdPageEndDate);
        }

        @Test
        @DisplayName("100개 후 빈 응답으로 종료")
        void 백개_후_빈_응답으로_종료() {
            // given
            LocalDate backfillStart = LocalDate.of(2024, 1, 1);
            LocalDate backfillEnd = LocalDate.of(2024, 4, 30);

            // 첫 번째 페이지: 정확히 100개 (2024-04-30 ~ 2024-01-22)
            var firstPageItems = createPriceItems("20240430", PAGE_SIZE);
            LocalDate firstPageLastDate = LocalDate.of(2024, 1, 22);
            LocalDate secondPageEndDate = LocalDate.of(2024, 1, 21);

            // 두 번째 페이지: 빈 응답
            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), eq(backfillStart), eq(backfillEnd))
            ).willReturn(firstPageItems);

            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), eq(backfillStart), eq(secondPageEndDate))
            ).willReturn(Collections.emptyList());

            given(persistenceService.saveDomesticStockPrices(eq("005930"), eq(firstPageItems)))
                    .willReturn(100);

            // when
            strategy.backfillHistoricalPrices(domesticStock, backfillStart, backfillEnd);

            // then
            verify(kisStockPriceService).getDomesticStockDailyPrices("005930", backfillStart, backfillEnd);
            verify(kisStockPriceService).getDomesticStockDailyPrices("005930", backfillStart, secondPageEndDate);
        }

        @Test
        @DisplayName("월말_연말_경계_커서_처리")
        void 월말_연말_경계_커서_처리() {
            // given
            LocalDate backfillStart = LocalDate.of(2023, 11, 1);
            LocalDate backfillEnd = LocalDate.of(2024, 2, 29);

            // 첫 번째 페이지: 100개 (2024-02-29 ~ 2023-11-22)
            var firstPageItems = createPriceItems("20240229", 100);
            LocalDate firstPageLastDate = LocalDate.of(2023, 11, 22);
            LocalDate secondPageEndDate = LocalDate.of(2023, 11, 21);

            // 두 번째 페이지: 20개 (2023-11-21 ~ 2023-11-02)
            var secondPageItems = createPriceItems("20231121", 20);

            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), eq(backfillStart), eq(backfillEnd))
            ).willReturn(firstPageItems);

            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), eq(backfillStart), eq(secondPageEndDate))
            ).willReturn(secondPageItems);

            given(persistenceService.saveDomesticStockPrices(eq("005930"), any()))
                    .willReturn(100, 20);

            // when
            strategy.backfillHistoricalPrices(domesticStock, backfillStart, backfillEnd);

            // then
            verify(kisStockPriceService).getDomesticStockDailyPrices("005930", backfillStart, backfillEnd);
            verify(kisStockPriceService).getDomesticStockDailyPrices("005930", backfillStart, secondPageEndDate);
        }

        @Test
        @DisplayName("startDate_도달_시_종료")
        void startDate_도달_시_종료() {
            // given
            LocalDate backfillStart = LocalDate.of(2024, 1, 20);
            LocalDate backfillEnd = LocalDate.of(2024, 2, 10);

            // 20개 데이터: 2024-02-10 ~ 2024-01-22
            var priceItems = createPriceItems("20240210", 20);

            given(kisStockPriceService.getDomesticStockDailyPrices(
                    eq("005930"), eq(backfillStart), eq(backfillEnd))
            ).willReturn(priceItems);

            given(persistenceService.saveDomesticStockPrices(eq("005930"), eq(priceItems)))
                    .willReturn(20);

            // when
            strategy.backfillHistoricalPrices(domesticStock, backfillStart, backfillEnd);

            // then
            verify(kisStockPriceService).getDomesticStockDailyPrices("005930", backfillStart, backfillEnd);
        }

        /**
         * 테스트용 가격 데이터 생성 헬퍼 메서드.
         * 주어진 시작 날짜부터 count개만큼 과거로 이동하며 데이터를 생성합니다.
         *
         * @param startDateStr 시작 날짜 (yyyyMMdd)
         * @param count 생성할 아이템 개수
         * @return 생성된 PriceItem 리스트
         */
        private List<DomesticStockDailyPriceResponse.PriceItem> createPriceItems(String startDateStr, int count) {
            LocalDate currentDate = LocalDate.parse(startDateStr, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            List<DomesticStockDailyPriceResponse.PriceItem> items = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                String dateStr = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                items.add(new DomesticStockDailyPriceResponse.PriceItem(
                        dateStr, "75000", "76000", "74000", "75500", "1000000", "75000000000"
                ));
                currentDate = currentDate.minusDays(1);
            }

            return items;
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
