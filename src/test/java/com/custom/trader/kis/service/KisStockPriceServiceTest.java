package com.custom.trader.kis.service;

import com.custom.trader.kis.client.KisRestClient;
import com.custom.trader.kis.config.KisAccountProperties;
import com.custom.trader.kis.config.KisApiEndpoint;
import com.custom.trader.kis.config.KisProperties;
import com.custom.trader.kis.dto.stockprice.DomesticStockDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.DomesticIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasStockDailyPriceResponse;
import com.custom.trader.kis.exception.KisApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(MockitoExtension.class)
class KisStockPriceServiceTest {

    @Mock
    private KisRestClient kisRestClient;

    @Mock
    private KisAuthService kisAuthService;

    @Mock
    private KisProperties kisProperties;

    private KisStockPriceService kisStockPriceService;

    private KisAccountProperties testAccount;

    @BeforeEach
    void setUp() {
        kisStockPriceService = new KisStockPriceService(kisRestClient, kisAuthService, kisProperties);
        testAccount = new KisAccountProperties("테스트계정", "12345678", "appKey123", "appSecret123");
    }

    @Nested
    @DisplayName("getDomesticStockDailyPrices 메소드")
    class GetDomesticStockDailyPrices {

        @Test
        @DisplayName("국내 주식 일간 시세 정상 반환")
        void 국내_주식_일간_시세_정상_반환() {
            // given
            var stockCode = "005930";
            var startDate = LocalDate.of(2024, 1, 1);
            var endDate = LocalDate.of(2024, 1, 5);

            var priceItems = List.of(
                    new DomesticStockDailyPriceResponse.PriceItem(
                            "20240105", "71000", "72000", "70000", "71500", "1000000", "71000000000"
                    ),
                    new DomesticStockDailyPriceResponse.PriceItem(
                            "20240104", "70000", "71000", "69000", "70500", "900000", "63000000000"
                    )
            );
            var output1 = new DomesticStockDailyPriceResponse.Output1("71500", "+500", "0.70", "삼성전자");
            var response = new DomesticStockDailyPriceResponse("0", "00000000", "정상", output1, priceItems);

            given(kisAuthService.getDefaultAccount()).willReturn(testAccount);
            given(kisAuthService.getAccessToken(testAccount.name())).willReturn("test-token");
            given(kisRestClient.get(
                    eq(KisApiEndpoint.DOMESTIC_STOCK_DAILY_PRICE),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(DomesticStockDailyPriceResponse.class)
            )).willReturn(response);

            // when
            var result = kisStockPriceService.getDomesticStockDailyPrices(stockCode, startDate, endDate);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).stckBsopDate()).isEqualTo("20240105");
            assertThat(result.get(0).stckClpr()).isEqualTo("71500");
            assertThat(result.get(1).stckBsopDate()).isEqualTo("20240104");
        }

        @Test
        @DisplayName("output2가 null이면 빈 리스트 반환")
        void output2가_null이면_빈_리스트_반환() {
            // given
            var stockCode = "005930";
            var startDate = LocalDate.of(2024, 1, 1);
            var endDate = LocalDate.of(2024, 1, 5);

            var response = new DomesticStockDailyPriceResponse("0", "00000000", "정상", null, null);

            given(kisAuthService.getDefaultAccount()).willReturn(testAccount);
            given(kisAuthService.getAccessToken(testAccount.name())).willReturn("test-token");
            given(kisRestClient.get(
                    eq(KisApiEndpoint.DOMESTIC_STOCK_DAILY_PRICE),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(DomesticStockDailyPriceResponse.class)
            )).willReturn(response);

            // when
            var result = kisStockPriceService.getDomesticStockDailyPrices(stockCode, startDate, endDate);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDomesticIndexDailyPrices 메소드")
    class GetDomesticIndexDailyPrices {

        @Test
        @DisplayName("국내 지수 일간 시세 정상 반환")
        void 국내_지수_일간_시세_정상_반환() {
            // given
            var indexCode = "0001";
            var startDate = LocalDate.of(2024, 1, 1);
            var endDate = LocalDate.of(2024, 1, 5);

            var priceItems = List.of(
                    new DomesticIndexDailyPriceResponse.PriceItem(
                            "20240105", "2650.50", "2640.00", "2660.00", "2630.00", "500000000", "10000000000000"
                    )
            );
            var output1 = new DomesticIndexDailyPriceResponse.Output1("2650.50", "+10.50", "2", "0.40", "500000000", "10000000000000", "코스피");
            var response = new DomesticIndexDailyPriceResponse("0", "00000000", "정상", output1, priceItems);

            given(kisAuthService.getDefaultAccount()).willReturn(testAccount);
            given(kisAuthService.getAccessToken(testAccount.name())).willReturn("test-token");
            given(kisRestClient.get(
                    eq(KisApiEndpoint.DOMESTIC_INDEX_DAILY_PRICE),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(DomesticIndexDailyPriceResponse.class)
            )).willReturn(response);

            // when
            var result = kisStockPriceService.getDomesticIndexDailyPrices(indexCode, startDate, endDate);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).stckBsopDate()).isEqualTo("20240105");
            assertThat(result.get(0).bstpNmixPrpr()).isEqualTo("2650.50");
        }
    }

    @Nested
    @DisplayName("getOverseasStockDailyPrices 메소드")
    class GetOverseasStockDailyPrices {

        @Test
        @DisplayName("해외 주식 일간 시세 정상 반환")
        void 해외_주식_일간_시세_정상_반환() {
            // given
            var stockCode = "AAPL";
            var exchangeCode = "NAS";
            var startDate = LocalDate.of(2024, 1, 1);
            var endDate = LocalDate.of(2024, 1, 5);

            var priceItems = List.of(
                    new OverseasStockDailyPriceResponse.PriceItem(
                            "20240105", "185.50", "183.00", "186.00", "182.50", "50000000", "9000000000", "185.00", "186.00"
                    )
            );
            var output1 = new OverseasStockDailyPriceResponse.Output1("D+AAPL", "4", "100");
            var response = new OverseasStockDailyPriceResponse("0", "00000000", "정상", output1, priceItems);

            given(kisAuthService.getDefaultAccount()).willReturn(testAccount);
            given(kisAuthService.getAccessToken(testAccount.name())).willReturn("test-token");
            given(kisRestClient.get(
                    eq(KisApiEndpoint.OVERSEAS_STOCK_DAILY_PRICE),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(OverseasStockDailyPriceResponse.class)
            )).willReturn(response);

            // when
            var result = kisStockPriceService.getOverseasStockDailyPrices(stockCode, exchangeCode, startDate, endDate);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).xymd()).isEqualTo("20240105");
            assertThat(result.get(0).clos()).isEqualTo("185.50");
        }
    }

    @Nested
    @DisplayName("getOverseasIndexDailyPrices 메소드")
    class GetOverseasIndexDailyPrices {

        @Test
        @DisplayName("해외 지수 일간 시세 정상 반환")
        void 해외_지수_일간_시세_정상_반환() {
            // given
            var indexCode = "COMP";
            var exchangeCode = "NAS";
            var startDate = LocalDate.of(2024, 1, 1);
            var endDate = LocalDate.of(2024, 1, 5);

            var priceItems = List.of(
                    new OverseasIndexDailyPriceResponse.PriceItem(
                            "20240105", "15000.50", "14900.00", "15100.00", "14850.00", "3000000000", "N"
                    )
            );
            var output1 = new OverseasIndexDailyPriceResponse.Output1(
                    "+100.50", "2", "0.67", "14900.00", "3000000000", "나스닥종합",
                    "15000.50", "COMP", "2800000000", "14900.00", "15100.00", "14850.00"
            );
            var response = new OverseasIndexDailyPriceResponse("0", "00000000", "정상", output1, priceItems);

            given(kisAuthService.getDefaultAccount()).willReturn(testAccount);
            given(kisAuthService.getAccessToken(testAccount.name())).willReturn("test-token");
            given(kisRestClient.get(
                    eq(KisApiEndpoint.OVERSEAS_INDEX_DAILY_PRICE),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(OverseasIndexDailyPriceResponse.class)
            )).willReturn(response);

            // when
            var result = kisStockPriceService.getOverseasIndexDailyPrices(indexCode, exchangeCode, startDate, endDate);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).stckBsopDate()).isEqualTo("20240105");
            assertThat(result.get(0).ovrsNmixPrpr()).isEqualTo("15000.50");
        }
    }

    @Nested
    @DisplayName("safeExtractOutput 메소드 (private, getDomesticStockDailyPrices 통해 간접 테스트)")
    class SafeExtractOutput {

        @Test
        @DisplayName("output2가 null이면 빈 리스트 반환 (국내 주식)")
        void output2가_null이면_빈_리스트_반환_국내주식() {
            // given
            var stockCode = "005930";
            var startDate = LocalDate.of(2024, 1, 1);
            var endDate = LocalDate.of(2024, 1, 5);

            var response = new DomesticStockDailyPriceResponse("0", "00000000", "정상", null, null);

            given(kisAuthService.getDefaultAccount()).willReturn(testAccount);
            given(kisAuthService.getAccessToken(testAccount.name())).willReturn("test-token");
            given(kisRestClient.get(
                    eq(KisApiEndpoint.DOMESTIC_STOCK_DAILY_PRICE),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(DomesticStockDailyPriceResponse.class)
            )).willReturn(response);

            // when
            var result = kisStockPriceService.getDomesticStockDailyPrices(stockCode, startDate, endDate);

            // then
            assertThat(result).isEmpty();
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("output2가 정상 리스트면 그대로 반환 (국내 주식)")
        void output2가_정상_리스트면_그대로_반환_국내주식() {
            // given
            var stockCode = "005930";
            var startDate = LocalDate.of(2024, 1, 1);
            var endDate = LocalDate.of(2024, 1, 5);

            var priceItems = List.of(
                    new DomesticStockDailyPriceResponse.PriceItem(
                            "20240105", "71000", "72000", "70000", "71500", "1000000", "71000000000"
                    )
            );
            var output1 = new DomesticStockDailyPriceResponse.Output1("71500", "+500", "0.70", "삼성전자");
            var response = new DomesticStockDailyPriceResponse("0", "00000000", "정상", output1, priceItems);

            given(kisAuthService.getDefaultAccount()).willReturn(testAccount);
            given(kisAuthService.getAccessToken(testAccount.name())).willReturn("test-token");
            given(kisRestClient.get(
                    eq(KisApiEndpoint.DOMESTIC_STOCK_DAILY_PRICE),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(DomesticStockDailyPriceResponse.class)
            )).willReturn(response);

            // when
            var result = kisStockPriceService.getDomesticStockDailyPrices(stockCode, startDate, endDate);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).stckBsopDate()).isEqualTo("20240105");
        }

        @Test
        @DisplayName("output2가 빈 리스트면 빈 리스트 반환 (국내 주식)")
        void output2가_빈_리스트면_빈_리스트_반환_국내주식() {
            // given
            var stockCode = "005930";
            var startDate = LocalDate.of(2024, 1, 1);
            var endDate = LocalDate.of(2024, 1, 5);

            var response = new DomesticStockDailyPriceResponse("0", "00000000", "정상", null, Collections.emptyList());

            given(kisAuthService.getDefaultAccount()).willReturn(testAccount);
            given(kisAuthService.getAccessToken(testAccount.name())).willReturn("test-token");
            given(kisRestClient.get(
                    eq(KisApiEndpoint.DOMESTIC_STOCK_DAILY_PRICE),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(DomesticStockDailyPriceResponse.class)
            )).willReturn(response);

            // when
            var result = kisStockPriceService.getDomesticStockDailyPrices(stockCode, startDate, endDate);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDefaultAccount 메소드 호출 검증 (KisAuthService 위임)")
    class GetDefaultAccount {

        @Test
        @DisplayName("계정이 없으면 예외 발생")
        void 계정이_없으면_예외_발생() {
            // given
            var stockCode = "005930";
            var startDate = LocalDate.of(2024, 1, 1);
            var endDate = LocalDate.of(2024, 1, 5);

            given(kisAuthService.getDefaultAccount()).willThrow(new KisApiException("No accounts configured"));

            // when & then
            assertThatThrownBy(() -> kisStockPriceService.getDomesticStockDailyPrices(stockCode, startDate, endDate))
                    .isInstanceOf(KisApiException.class)
                    .hasMessageContaining("No accounts configured");
        }
    }
}
