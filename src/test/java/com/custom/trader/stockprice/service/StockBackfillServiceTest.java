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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockBackfillServiceTest {

    @Mock
    private StockPriceStrategyFactory strategyFactory;

    @Mock
    private StockPriceStrategy strategy;

    @Mock
    private WatchlistStockRepository watchlistStockRepository;

    private StockBackfillService stockBackfillService;

    @BeforeEach
    void setUp() {
        stockBackfillService = new StockBackfillService(strategyFactory, watchlistStockRepository);
    }

    @Test
    @DisplayName("국내 주식 백필")
    void 국내_주식_백필() {
        // given
        var stock = createDomesticStock("005930", "삼성전자", AssetType.DOMESTIC_STOCK);
        var startDate = LocalDate.of(2024, 1, 1);
        var endDate = LocalDate.of(2024, 1, 31);

        given(strategyFactory.getStrategy(AssetType.DOMESTIC_STOCK)).willReturn(strategy);

        // when
        stockBackfillService.backfillSingleStock(stock, startDate, endDate);

        // then
        verify(strategyFactory).getStrategy(AssetType.DOMESTIC_STOCK);
        verify(strategy).backfillHistoricalPrices(eq(stock), eq(startDate), eq(endDate));
        verify(watchlistStockRepository).save(stock);
        assertThat(stock.isBackfillCompleted()).isTrue();
    }

    @Test
    @DisplayName("해외 주식 백필")
    void 해외_주식_백필() {
        // given
        var stock = createOverseasStock("AAPL", "애플", MarketCode.NAS, AssetType.OVERSEAS_STOCK);
        var startDate = LocalDate.of(2024, 1, 1);
        var endDate = LocalDate.of(2024, 1, 31);

        given(strategyFactory.getStrategy(AssetType.OVERSEAS_STOCK)).willReturn(strategy);

        // when
        stockBackfillService.backfillSingleStock(stock, startDate, endDate);

        // then
        verify(strategyFactory).getStrategy(AssetType.OVERSEAS_STOCK);
        verify(strategy).backfillHistoricalPrices(eq(stock), eq(startDate), eq(endDate));
        verify(watchlistStockRepository).save(stock);
        assertThat(stock.isBackfillCompleted()).isTrue();
    }

    @Test
    @DisplayName("AssetType이 null이면 스킵")
    void AssetType_null_스킵() {
        // given
        var stock = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(null)
                .build();
        var startDate = LocalDate.of(2024, 1, 1);
        var endDate = LocalDate.of(2024, 1, 31);

        // when
        stockBackfillService.backfillSingleStock(stock, startDate, endDate);

        // then
        verify(strategyFactory, never()).getStrategy(any());
        verify(strategy, never()).backfillHistoricalPrices(any(), any(), any());
        verify(watchlistStockRepository, never()).save(any());
        assertThat(stock.isBackfillCompleted()).isFalse();
    }

    private WatchlistStock createDomesticStock(String stockCode, String stockName, AssetType assetType) {
        return WatchlistStock.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .marketCode(MarketCode.KRX)
                .assetType(assetType)
                .build();
    }

    private WatchlistStock createOverseasStock(String stockCode, String stockName,
                                                MarketCode marketCode, AssetType assetType) {
        return WatchlistStock.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .marketCode(marketCode)
                .assetType(assetType)
                .build();
    }
}
