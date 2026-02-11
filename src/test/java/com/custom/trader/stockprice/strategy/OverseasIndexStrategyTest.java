package com.custom.trader.stockprice.strategy;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
import com.custom.trader.kis.dto.stockprice.OverseasIndexDailyPriceResponse;
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
class OverseasIndexStrategyTest {

    @Mock
    private KisStockPriceService kisStockPriceService;

    @Mock
    private StockPricePersistenceService persistenceService;

    @InjectMocks
    private OverseasIndexStrategy strategy;

    private WatchlistStock overseasIndex;
    private LocalDate startDate;
    private LocalDate endDate;
    private String exchangeCode;

    @BeforeEach
    void setUp() {
        exchangeCode = "NAS";
        overseasIndex = WatchlistStock.builder()
                .stockCode("COMP")
                .stockName("NASDAQ Composite")
                .assetType(AssetType.OVERSEAS_INDEX)
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
                    new OverseasIndexDailyPriceResponse.PriceItem(
                            "20240131", "15100.25", "15000.50", "15200.00", "14900.00", "5000000000", "N"
                    )
            );
            given(kisStockPriceService.getOverseasIndexDailyPrices(
                    eq("COMP"), eq(exchangeCode), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveOverseasIndexPrices(eq("COMP"), eq(exchangeCode), eq(priceItems)))
                    .willReturn(1);

            // when
            int saved = strategy.collectDailyPrice(overseasIndex, startDate, endDate);

            // then
            assertThat(saved).isEqualTo(1);
            verify(kisStockPriceService).getOverseasIndexDailyPrices("COMP", exchangeCode, startDate, endDate);
            verify(persistenceService).saveOverseasIndexPrices("COMP", exchangeCode, priceItems);
        }

        @Test
        @DisplayName("빈 데이터 반환시 0 저장")
        void 빈_데이터_반환시_0_저장() {
            // given
            given(kisStockPriceService.getOverseasIndexDailyPrices(
                    eq("COMP"), eq(exchangeCode), any(LocalDate.class), any(LocalDate.class))
            ).willReturn(Collections.emptyList());
            given(persistenceService.saveOverseasIndexPrices(eq("COMP"), eq(exchangeCode), any()))
                    .willReturn(0);

            // when
            int saved = strategy.collectDailyPrice(overseasIndex, startDate, endDate);

            // then
            assertThat(saved).isZero();
        }

        @Test
        @DisplayName("KisApiException 발생시 예외 전파")
        void KisApiException_발생시_예외_전파() {
            // given
            given(kisStockPriceService.getOverseasIndexDailyPrices(
                    eq("COMP"), eq(exchangeCode), any(LocalDate.class), any(LocalDate.class))
            ).willThrow(new KisApiException("API 호출 실패"));

            // when & then
            assertThatThrownBy(() -> strategy.collectDailyPrice(overseasIndex, startDate, endDate))
                    .isInstanceOf(KisApiException.class)
                    .hasMessage("API 호출 실패");
        }

        @Test
        @DisplayName("다수의 가격 데이터를 수집하고 저장")
        void 다수의_가격_데이터를_수집하고_저장() {
            // given
            var priceItems = List.of(
                    new OverseasIndexDailyPriceResponse.PriceItem(
                            "20240131", "15100.25", "15000.50", "15200.00", "14900.00", "5000000000", "N"
                    ),
                    new OverseasIndexDailyPriceResponse.PriceItem(
                            "20240130", "14950.75", "14900.00", "15000.50", "14800.00", "4800000000", "N"
                    ),
                    new OverseasIndexDailyPriceResponse.PriceItem(
                            "20240129", "14850.50", "14800.00", "14900.00", "14700.00", "4600000000", "N"
                    )
            );
            given(kisStockPriceService.getOverseasIndexDailyPrices(
                    eq("COMP"), eq(exchangeCode), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveOverseasIndexPrices(eq("COMP"), eq(exchangeCode), eq(priceItems)))
                    .willReturn(3);

            // when
            int saved = strategy.collectDailyPrice(overseasIndex, startDate, endDate);

            // then
            assertThat(saved).isEqualTo(3);
        }

        @Test
        @DisplayName("ExchangeCode 조회 검증")
        void ExchangeCode_조회_검증() {
            // given
            var priceItems = List.of(
                    new OverseasIndexDailyPriceResponse.PriceItem(
                            "20240131", "15100.25", "15000.50", "15200.00", "14900.00", "5000000000", "N"
                    )
            );
            given(kisStockPriceService.getOverseasIndexDailyPrices(
                    eq("COMP"), eq("NAS"), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveOverseasIndexPrices(eq("COMP"), eq("NAS"), eq(priceItems)))
                    .willReturn(1);

            // when
            strategy.collectDailyPrice(overseasIndex, startDate, endDate);

            // then
            verify(kisStockPriceService).getOverseasIndexDailyPrices("COMP", "NAS", startDate, endDate);
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
                    new OverseasIndexDailyPriceResponse.PriceItem(
                            "20240131", "15100.25", "15000.50", "15200.00", "14900.00", "5000000000", "N"
                    )
            );
            given(kisStockPriceService.getOverseasIndexDailyPrices(
                    eq("COMP"), eq(exchangeCode), eq(startDate), eq(endDate))
            ).willReturn(priceItems);
            given(persistenceService.saveOverseasIndexPrices(eq("COMP"), eq(exchangeCode), eq(priceItems)))
                    .willReturn(1);

            // when
            strategy.backfillHistoricalPrices(overseasIndex, startDate, endDate);

            // then
            verify(kisStockPriceService).getOverseasIndexDailyPrices("COMP", exchangeCode, startDate, endDate);
            verify(persistenceService).saveOverseasIndexPrices("COMP", exchangeCode, priceItems);
        }

        @Test
        @DisplayName("빈 데이터 반환시 백필 중단")
        void 빈_데이터_반환시_백필_중단() {
            // given
            given(kisStockPriceService.getOverseasIndexDailyPrices(
                    eq("COMP"), eq(exchangeCode), any(LocalDate.class), any(LocalDate.class))
            ).willReturn(Collections.emptyList());

            // when
            strategy.backfillHistoricalPrices(overseasIndex, startDate, endDate);

            // then
            verify(kisStockPriceService).getOverseasIndexDailyPrices("COMP", exchangeCode, startDate, endDate);
        }

        @Test
        @DisplayName("KisApiException 발생시 예외 전파")
        void KisApiException_발생시_예외_전파() {
            // given
            given(kisStockPriceService.getOverseasIndexDailyPrices(
                    eq("COMP"), eq(exchangeCode), any(LocalDate.class), any(LocalDate.class))
            ).willThrow(new KisApiException("API 호출 실패"));

            // when & then
            assertThatThrownBy(() -> strategy.backfillHistoricalPrices(overseasIndex, startDate, endDate))
                    .isInstanceOf(KisApiException.class)
                    .hasMessage("API 호출 실패");
        }

        @Test
        @DisplayName("2페이지 백필 시 커서 계산 및 날짜 필드명 검증")
        void 두페이지_백필_시_커서_계산_및_날짜_필드명_검증() {
            // given
            LocalDate backfillStart = LocalDate.of(2024, 1, 1);
            LocalDate backfillEnd = LocalDate.of(2024, 5, 31);

            // 첫 번째 페이지: 100개 (2024-05-31 ~ 2024-02-22)
            var firstPageItems = createPriceItems("20240531", 100);
            LocalDate firstPageLastDate = LocalDate.of(2024, 2, 22);
            LocalDate secondPageEndDate = LocalDate.of(2024, 2, 21);

            // 두 번째 페이지: 50개 (2024-02-21 ~ 2024-01-03)
            var secondPageItems = createPriceItems("20240221", 50);

            given(kisStockPriceService.getOverseasIndexDailyPrices(
                    eq("COMP"), eq(exchangeCode), eq(backfillStart), eq(backfillEnd))
            ).willReturn(firstPageItems);

            given(kisStockPriceService.getOverseasIndexDailyPrices(
                    eq("COMP"), eq(exchangeCode), eq(backfillStart), eq(secondPageEndDate))
            ).willReturn(secondPageItems);

            given(persistenceService.saveOverseasIndexPrices(eq("COMP"), eq(exchangeCode), any()))
                    .willReturn(100, 50);

            // when
            strategy.backfillHistoricalPrices(overseasIndex, backfillStart, backfillEnd);

            // then
            verify(kisStockPriceService).getOverseasIndexDailyPrices("COMP", exchangeCode, backfillStart, backfillEnd);
            verify(kisStockPriceService).getOverseasIndexDailyPrices("COMP", exchangeCode, backfillStart, secondPageEndDate);
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
            given(kisStockPriceService.getOverseasIndexDailyPrices(
                    eq("COMP"), eq(exchangeCode), eq(backfillStart), eq(backfillEnd))
            ).willReturn(firstPageItems);

            given(kisStockPriceService.getOverseasIndexDailyPrices(
                    eq("COMP"), eq(exchangeCode), eq(backfillStart), eq(secondPageEndDate))
            ).willReturn(Collections.emptyList());

            given(persistenceService.saveOverseasIndexPrices(eq("COMP"), eq(exchangeCode), eq(firstPageItems)))
                    .willReturn(100);

            // when
            strategy.backfillHistoricalPrices(overseasIndex, backfillStart, backfillEnd);

            // then
            verify(kisStockPriceService).getOverseasIndexDailyPrices("COMP", exchangeCode, backfillStart, backfillEnd);
            verify(kisStockPriceService).getOverseasIndexDailyPrices("COMP", exchangeCode, backfillStart, secondPageEndDate);
        }

        /**
         * 테스트용 가격 데이터 생성 헬퍼 메서드.
         * 주어진 시작 날짜부터 count개만큼 과거로 이동하며 데이터를 생성합니다.
         * stckBsopDate 필드를 사용합니다 (해외 지수).
         *
         * @param startDateStr 시작 날짜 (yyyyMMdd)
         * @param count 생성할 아이템 개수
         * @return 생성된 PriceItem 리스트
         */
        private List<OverseasIndexDailyPriceResponse.PriceItem> createPriceItems(String startDateStr, int count) {
            LocalDate currentDate = LocalDate.parse(startDateStr, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            List<OverseasIndexDailyPriceResponse.PriceItem> items = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                String dateStr = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                items.add(new OverseasIndexDailyPriceResponse.PriceItem(
                        dateStr, "15100.25", "15000.50", "15200.00", "14900.00", "5000000000", "N"
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
            var annotation = OverseasIndexStrategy.class
                    .getAnnotation(org.springframework.stereotype.Component.class);

            // then
            assertThat(annotation).isNotNull();
        }
    }
}
