package com.custom.trader.stockprice.service;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
import com.custom.trader.stockprice.strategy.StockPriceStrategy;
import com.custom.trader.stockprice.strategy.StockPriceStrategyFactory;
import com.custom.trader.watchlist.entity.WatchlistStock;
import com.custom.trader.watchlist.repository.WatchlistStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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

        Page<WatchlistStock> page = new PageImpl<>(List.of(stock), PageRequest.of(0, 100), 1);
        given(watchlistStockRepository.findByBackfillCompleted(eq(true), any())).willReturn(page);
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

        Page<WatchlistStock> page = new PageImpl<>(List.of(stock), PageRequest.of(0, 100), 1);
        given(watchlistStockRepository.findByBackfillCompleted(eq(false), any())).willReturn(page);

        // when
        stockPriceCollectionService.backfillHistoricalPrices();

        // then
        verify(stockBackfillService).backfillSingleStock(eq(stock), any(), any());
    }
}
