package com.custom.trader.stockprice.service;

import com.custom.trader.kis.dto.stockprice.DomesticIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.DomesticStockDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasStockDailyPriceResponse;
import com.custom.trader.stockprice.domestic.entity.DomesticIndexDailyPrice;
import com.custom.trader.stockprice.domestic.entity.DomesticStockDailyPrice;
import com.custom.trader.stockprice.domestic.repository.DomesticIndexDailyPriceRepository;
import com.custom.trader.stockprice.domestic.repository.DomesticStockDailyPriceRepository;
import com.custom.trader.stockprice.mapper.StockPriceMapper;
import com.custom.trader.stockprice.overseas.entity.OverseasIndexDailyPrice;
import com.custom.trader.stockprice.overseas.entity.OverseasStockDailyPrice;
import com.custom.trader.stockprice.overseas.repository.OverseasIndexDailyPriceRepository;
import com.custom.trader.stockprice.overseas.repository.OverseasStockDailyPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockPricePersistenceServiceTest {

    @Mock
    private DomesticStockDailyPriceRepository domesticStockRepository;

    @Mock
    private DomesticIndexDailyPriceRepository domesticIndexRepository;

    @Mock
    private OverseasStockDailyPriceRepository overseasStockRepository;

    @Mock
    private OverseasIndexDailyPriceRepository overseasIndexRepository;

    @Mock
    private StockPriceMapper mapper;

    private StockPricePersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new StockPricePersistenceService(
                domesticStockRepository,
                domesticIndexRepository,
                overseasStockRepository,
                overseasIndexRepository,
                mapper
        );
    }

    @Nested
    @DisplayName("국내 주식 가격 저장")
    class SaveDomesticStockPrices {

        @Test
        @DisplayName("중복 데이터는 저장하지 않음")
        void 중복_필터링() {
            // given
            var priceItems = List.of(
                    new DomesticStockDailyPriceResponse.PriceItem(
                            "20240131", "71000", "72000", "70000", "71500", "1000000", "71000000000"
                    ),
                    new DomesticStockDailyPriceResponse.PriceItem(
                            "20240130", "70000", "71000", "69000", "70500", "900000", "63000000000"
                    )
            );

            given(domesticStockRepository.findTradeDatesByStockCodeAndTradeDateBetween(eq("005930"), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(Set.of(LocalDate.of(2024, 1, 31)));

            var mockEntity = DomesticStockDailyPrice.builder()
                    .stockCode("005930")
                    .tradeDate(LocalDate.of(2024, 1, 30))
                    .build();

            given(mapper.toDomesticStock(eq("005930"), any())).willReturn(mockEntity);

            // when
            int savedCount = persistenceService.saveDomesticStockPrices("005930", priceItems);

            // then
            assertThat(savedCount).isEqualTo(1);
            verify(domesticStockRepository, times(1)).saveAll(anyList());
            verify(mapper, times(1)).toDomesticStock(eq("005930"), any());
        }

        @Test
        @DisplayName("모든 데이터가 중복이면 저장하지 않음")
        void 모든_데이터_중복() {
            // given
            var priceItems = List.of(
                    new DomesticStockDailyPriceResponse.PriceItem(
                            "20240131", "71000", "72000", "70000", "71500", "1000000", "71000000000"
                    )
            );

            given(domesticStockRepository.findTradeDatesByStockCodeAndTradeDateBetween(eq("005930"), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(Set.of(LocalDate.of(2024, 1, 31)));

            // when
            int savedCount = persistenceService.saveDomesticStockPrices("005930", priceItems);

            // then
            assertThat(savedCount).isEqualTo(0);
            verify(domesticStockRepository, never()).saveAll(any());
            verify(mapper, never()).toDomesticStock(any(), any());
        }

        @Test
        @DisplayName("중복이 없으면 모두 저장")
        void 모두_저장() {
            // given
            var priceItems = List.of(
                    new DomesticStockDailyPriceResponse.PriceItem(
                            "20240131", "71000", "72000", "70000", "71500", "1000000", "71000000000"
                    ),
                    new DomesticStockDailyPriceResponse.PriceItem(
                            "20240130", "70000", "71000", "69000", "70500", "900000", "63000000000"
                    )
            );

            given(domesticStockRepository.findTradeDatesByStockCodeAndTradeDateBetween(eq("005930"), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(Set.of());

            var mockEntity = DomesticStockDailyPrice.builder()
                    .stockCode("005930")
                    .build();

            given(mapper.toDomesticStock(eq("005930"), any())).willReturn(mockEntity);

            // when
            int savedCount = persistenceService.saveDomesticStockPrices("005930", priceItems);

            // then
            assertThat(savedCount).isEqualTo(2);
            verify(domesticStockRepository, times(1)).saveAll(anyList());
            verify(mapper, times(2)).toDomesticStock(eq("005930"), any());
        }
    }

    @Nested
    @DisplayName("국내 지수 가격 저장")
    class SaveDomesticIndexPrices {

        @Test
        @DisplayName("중복 데이터는 저장하지 않음")
        void 중복_필터링() {
            // given
            var priceItems = List.of(
                    new DomesticIndexDailyPriceResponse.PriceItem(
                            "20240131", "2500", "2520", "2480", "2510", "500000", "10000000000"
                    ),
                    new DomesticIndexDailyPriceResponse.PriceItem(
                            "20240130", "2480", "2500", "2460", "2490", "480000", "9500000000"
                    )
            );

            given(domesticIndexRepository.findTradeDatesByIndexCodeAndTradeDateBetween(eq("0001"), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(Set.of(LocalDate.of(2024, 1, 31)));

            var mockEntity = DomesticIndexDailyPrice.builder()
                    .indexCode("0001")
                    .tradeDate(LocalDate.of(2024, 1, 30))
                    .build();

            given(mapper.toDomesticIndex(eq("0001"), any())).willReturn(mockEntity);

            // when
            int savedCount = persistenceService.saveDomesticIndexPrices("0001", priceItems);

            // then
            assertThat(savedCount).isEqualTo(1);
            verify(domesticIndexRepository, times(1)).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("해외 주식 가격 저장")
    class SaveOverseasStockPrices {

        @Test
        @DisplayName("중복 데이터는 저장하지 않음")
        void 중복_필터링() {
            // given
            var priceItems = List.of(
                    new OverseasStockDailyPriceResponse.PriceItem(
                            "20240131", "185.50", "183.00", "186.00", "182.50",
                            "50000000", "9000000000", "185.00", "186.00"
                    ),
                    new OverseasStockDailyPriceResponse.PriceItem(
                            "20240130", "184.00", "182.00", "185.00", "181.50",
                            "48000000", "8800000000", "184.00", "185.00"
                    )
            );

            given(overseasStockRepository.findTradeDatesByStockCodeAndExchangeCodeAndTradeDateBetween(eq("AAPL"), eq("NAS"), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(Set.of(LocalDate.of(2024, 1, 31)));

            var mockEntity = OverseasStockDailyPrice.builder()
                    .stockCode("AAPL")
                    .exchangeCode("NAS")
                    .tradeDate(LocalDate.of(2024, 1, 30))
                    .build();

            given(mapper.toOverseasStock(eq("AAPL"), eq("NAS"), any())).willReturn(mockEntity);

            // when
            int savedCount = persistenceService.saveOverseasStockPrices("AAPL", "NAS", priceItems);

            // then
            assertThat(savedCount).isEqualTo(1);
            verify(overseasStockRepository, times(1)).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("해외 지수 가격 저장")
    class SaveOverseasIndexPrices {

        @Test
        @DisplayName("중복 데이터는 저장하지 않음")
        void 중복_필터링() {
            // given
            var priceItems = List.of(
                    new OverseasIndexDailyPriceResponse.PriceItem(
                            "20240131", "15000", "14800", "15200", "14700", "800000", "N"
                    ),
                    new OverseasIndexDailyPriceResponse.PriceItem(
                            "20240130", "14800", "14600", "15000", "14500", "750000", "N"
                    )
            );

            given(overseasIndexRepository.findTradeDatesByIndexCodeAndExchangeCodeAndTradeDateBetween(eq("COMP"), eq("NAS"), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(Set.of(LocalDate.of(2024, 1, 31)));

            var mockEntity = OverseasIndexDailyPrice.builder()
                    .indexCode("COMP")
                    .exchangeCode("NAS")
                    .tradeDate(LocalDate.of(2024, 1, 30))
                    .build();

            given(mapper.toOverseasIndex(eq("COMP"), eq("NAS"), any())).willReturn(mockEntity);

            // when
            int savedCount = persistenceService.saveOverseasIndexPrices("COMP", "NAS", priceItems);

            // then
            assertThat(savedCount).isEqualTo(1);
            verify(overseasIndexRepository, times(1)).saveAll(anyList());
        }
    }
}
