package com.custom.trader.stockprice.service;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
import com.custom.trader.kis.exception.KisApiException;
import com.custom.trader.stockprice.constant.StockPriceConstants;
import com.custom.trader.stockprice.strategy.StockPriceStrategy;
import com.custom.trader.stockprice.strategy.StockPriceStrategyFactory;
import com.custom.trader.watchlist.entity.WatchlistStock;
import com.custom.trader.watchlist.repository.WatchlistStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockPriceCollectionServiceTest {

    @Mock
    private WatchlistStockRepository watchlistStockRepository;

    @Mock
    private StockBackfillService stockBackfillService;

    @Mock
    private StockPriceStrategyFactory strategyFactory;

    @Mock
    private StockPriceStrategy strategy;

    private StockPriceCollectionService stockPriceCollectionService;

    @BeforeEach
    void setUp() {
        stockPriceCollectionService = new StockPriceCollectionService(
                watchlistStockRepository,
                stockBackfillService,
                strategyFactory
        );
    }

    @Test
    @DisplayName("일간 가격 수집")
    void collectDailyPrices() {
        // given
        var stock = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();
        stock.markBackfillCompleted();

        Slice<WatchlistStock> slice = new SliceImpl<>(List.of(stock), PageRequest.of(0, 100), false);
        given(watchlistStockRepository.findByBackfillCompleted(eq(true), any())).willReturn(slice);
        given(strategyFactory.getStrategy(AssetType.DOMESTIC_STOCK)).willReturn(strategy);
        given(strategy.collectDailyPrice(eq(stock), any(LocalDate.class), any(LocalDate.class))).willReturn(1);

        // when
        stockPriceCollectionService.collectDailyPrices();

        // then
        verify(strategyFactory).getStrategy(AssetType.DOMESTIC_STOCK);
        verify(strategy).collectDailyPrice(eq(stock), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("백필")
    void backfillHistoricalPrices() {
        // given
        var stock = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();

        Slice<WatchlistStock> slice = new SliceImpl<>(List.of(stock), PageRequest.of(0, 100), false);
        given(watchlistStockRepository.findByBackfillCompleted(eq(false), any())).willReturn(slice);

        // when
        stockPriceCollectionService.backfillHistoricalPrices();

        // then
        verify(stockBackfillService).backfillSingleStock(eq(stock), any(), any());
    }

    @Nested
    @DisplayName("collectDailyPrices 멀티 페이지 및 예외 처리")
    class CollectDailyPricesEdgeCases {

        @Test
        @DisplayName("멀티 페이지 수집 - 2페이지 이상 데이터 순회")
        void collectDailyPrices_withMultiplePages() {
            // given
            var stock1 = WatchlistStock.builder()
                    .stockCode("005930")
                    .stockName("삼성전자")
                    .marketCode(MarketCode.KRX)
                    .assetType(AssetType.DOMESTIC_STOCK)
                    .build();
            stock1.markBackfillCompleted();

            var stock2 = WatchlistStock.builder()
                    .stockCode("000660")
                    .stockName("SK하이닉스")
                    .marketCode(MarketCode.KRX)
                    .assetType(AssetType.DOMESTIC_STOCK)
                    .build();
            stock2.markBackfillCompleted();

            // 첫 번째 페이지: hasNext = true
            Slice<WatchlistStock> firstSlice = new SliceImpl<>(
                    List.of(stock1),
                    PageRequest.of(0, StockPriceConstants.PAGE_SIZE),
                    true
            );

            // 두 번째 페이지: hasNext = false
            Slice<WatchlistStock> secondSlice = new SliceImpl<>(
                    List.of(stock2),
                    PageRequest.of(1, StockPriceConstants.PAGE_SIZE),
                    false
            );

            given(watchlistStockRepository.findByBackfillCompleted(eq(true), any()))
                    .willReturn(firstSlice, secondSlice);
            given(strategyFactory.getStrategy(AssetType.DOMESTIC_STOCK)).willReturn(strategy);
            given(strategy.collectDailyPrice(any(), any(LocalDate.class), any(LocalDate.class))).willReturn(1);

            // when
            stockPriceCollectionService.collectDailyPrices();

            // then
            verify(watchlistStockRepository, times(2)).findByBackfillCompleted(eq(true), any());
            verify(strategy, times(2)).collectDailyPrice(any(), any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("빈 결과 - 백필 완료 종목이 없을 때 정상 종료")
        void collectDailyPrices_withEmptyResult() {
            // given
            Slice<WatchlistStock> emptySlice = new SliceImpl<>(
                    Collections.emptyList(),
                    PageRequest.of(0, StockPriceConstants.PAGE_SIZE),
                    false
            );
            given(watchlistStockRepository.findByBackfillCompleted(eq(true), any())).willReturn(emptySlice);

            // when
            stockPriceCollectionService.collectDailyPrices();

            // then
            verify(watchlistStockRepository, times(1)).findByBackfillCompleted(eq(true), any());
            verify(strategyFactory, never()).getStrategy(any());
            verify(strategy, never()).collectDailyPrice(any(), any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("KisApiException 발생 시 - 해당 종목 실패, 나머지 계속 처리")
        void collectDailyPrices_withKisApiException() {
            // given
            var stock1 = WatchlistStock.builder()
                    .stockCode("005930")
                    .stockName("삼성전자")
                    .marketCode(MarketCode.KRX)
                    .assetType(AssetType.DOMESTIC_STOCK)
                    .build();
            stock1.markBackfillCompleted();

            var stock2 = WatchlistStock.builder()
                    .stockCode("000660")
                    .stockName("SK하이닉스")
                    .marketCode(MarketCode.KRX)
                    .assetType(AssetType.DOMESTIC_STOCK)
                    .build();
            stock2.markBackfillCompleted();

            Slice<WatchlistStock> slice = new SliceImpl<>(
                    List.of(stock1, stock2),
                    PageRequest.of(0, StockPriceConstants.PAGE_SIZE),
                    false
            );

            given(watchlistStockRepository.findByBackfillCompleted(eq(true), any())).willReturn(slice);
            given(strategyFactory.getStrategy(AssetType.DOMESTIC_STOCK)).willReturn(strategy);

            // stock1 처리 시 KisApiException 발생
            given(strategy.collectDailyPrice(eq(stock1), any(LocalDate.class), any(LocalDate.class)))
                    .willThrow(new KisApiException("API 호출 실패"));
            // stock2는 정상 처리
            given(strategy.collectDailyPrice(eq(stock2), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(1);

            // when
            stockPriceCollectionService.collectDailyPrices();

            // then
            verify(strategy, times(2)).collectDailyPrice(any(), any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("DataAccessException 발생 시 - Critical 실패 로깅")
        void collectDailyPrices_withDataAccessException() {
            // given
            var stock = WatchlistStock.builder()
                    .stockCode("005930")
                    .stockName("삼성전자")
                    .marketCode(MarketCode.KRX)
                    .assetType(AssetType.DOMESTIC_STOCK)
                    .build();
            stock.markBackfillCompleted();

            Slice<WatchlistStock> slice = new SliceImpl<>(
                    List.of(stock),
                    PageRequest.of(0, StockPriceConstants.PAGE_SIZE),
                    false
            );

            given(watchlistStockRepository.findByBackfillCompleted(eq(true), any())).willReturn(slice);
            given(strategyFactory.getStrategy(AssetType.DOMESTIC_STOCK)).willReturn(strategy);
            given(strategy.collectDailyPrice(any(), any(LocalDate.class), any(LocalDate.class)))
                    .willThrow(new DataAccessException("DB 연결 실패") {});

            // when
            stockPriceCollectionService.collectDailyPrices();

            // then
            verify(strategy).collectDailyPrice(any(), any(LocalDate.class), any(LocalDate.class));
        }
    }

    @Nested
    @DisplayName("backfillHistoricalPrices 멀티 페이지 및 예외 처리")
    class BackfillHistoricalPricesEdgeCases {

        @Test
        @DisplayName("멀티 페이지 백필 - 2페이지 이상 순회")
        void backfillHistoricalPrices_withMultiplePages() {
            // given
            var stock1 = WatchlistStock.builder()
                    .stockCode("005930")
                    .stockName("삼성전자")
                    .marketCode(MarketCode.KRX)
                    .assetType(AssetType.DOMESTIC_STOCK)
                    .build();

            var stock2 = WatchlistStock.builder()
                    .stockCode("000660")
                    .stockName("SK하이닉스")
                    .marketCode(MarketCode.KRX)
                    .assetType(AssetType.DOMESTIC_STOCK)
                    .build();

            // 첫 번째 페이지: hasNext = true
            Slice<WatchlistStock> firstSlice = new SliceImpl<>(
                    List.of(stock1),
                    PageRequest.of(0, StockPriceConstants.PAGE_SIZE),
                    true
            );

            // 두 번째 페이지: hasNext = false
            Slice<WatchlistStock> secondSlice = new SliceImpl<>(
                    List.of(stock2),
                    PageRequest.of(1, StockPriceConstants.PAGE_SIZE),
                    false
            );

            given(watchlistStockRepository.findByBackfillCompleted(eq(false), any()))
                    .willReturn(firstSlice, secondSlice);

            // when
            stockPriceCollectionService.backfillHistoricalPrices();

            // then
            verify(watchlistStockRepository, times(2)).findByBackfillCompleted(eq(false), any());
            verify(stockBackfillService, times(2)).backfillSingleStock(any(), any(), any());
        }

        @Test
        @DisplayName("빈 결과 - 백필 미완료 종목이 없을 때 정상 종료")
        void backfillHistoricalPrices_withEmptyResult() {
            // given
            Slice<WatchlistStock> emptySlice = new SliceImpl<>(
                    Collections.emptyList(),
                    PageRequest.of(0, StockPriceConstants.PAGE_SIZE),
                    false
            );
            given(watchlistStockRepository.findByBackfillCompleted(eq(false), any())).willReturn(emptySlice);

            // when
            stockPriceCollectionService.backfillHistoricalPrices();

            // then
            verify(watchlistStockRepository, times(1)).findByBackfillCompleted(eq(false), any());
            verify(stockBackfillService, never()).backfillSingleStock(any(), any(), any());
        }
    }
}
