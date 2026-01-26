package com.custom.trader.stockprice.strategy;

import com.custom.trader.kis.service.KisStockPriceService;
import com.custom.trader.stockprice.constant.StockPriceConstants;
import com.custom.trader.stockprice.service.StockPricePersistenceService;
import com.custom.trader.watchlist.entity.WatchlistStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;

import static com.custom.trader.stockprice.constant.StockPriceConstants.PAGE_SIZE;

/**
 * 해외 지수 가격 수집 전략 구현체.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverseasIndexStrategy implements StockPriceStrategy {

    private final KisStockPriceService kisStockPriceService;
    private final StockPricePersistenceService persistenceService;

    @Override
    public int collectDailyPrice(WatchlistStock stock, LocalDate startDate, LocalDate endDate) {
        String exchangeCode = stock.getMarketCode().getExcd();
        var prices = kisStockPriceService.getOverseasIndexDailyPrices(
                stock.getStockCode(), exchangeCode, startDate, endDate);
        int saved = persistenceService.saveOverseasIndexPrices(
                stock.getStockCode(), exchangeCode, prices);
        log.debug("Saved {} overseas index prices for: {}", saved, stock.getStockCode());
        return saved;
    }

    @Override
    public void backfillHistoricalPrices(WatchlistStock stock, LocalDate startDate, LocalDate endDate) {
        String code = stock.getStockCode();
        String exchangeCode = stock.getMarketCode().getExcd();
        LocalDate currentEndDate = endDate;
        int totalSaved = 0;

        while (!currentEndDate.isBefore(startDate)) {
            var prices = kisStockPriceService.getOverseasIndexDailyPrices(
                    code, exchangeCode, startDate, currentEndDate);

            if (prices.isEmpty()) {
                break;
            }

            int savedCount = persistenceService.saveOverseasIndexPrices(code, exchangeCode, prices);
            totalSaved += savedCount;

            if (prices.size() < PAGE_SIZE) {
                break;
            }

            LocalDate lastDate = prices.stream()
                    .map(p -> StockPriceConstants.parseDate(p.stckBsopDate()))
                    .min(Comparator.naturalOrder())
                    .orElseThrow(() -> new IllegalStateException("Cannot extract date from empty price list"));
            currentEndDate = lastDate.minusDays(1);
        }

        log.debug("Total saved {} overseas index prices for: {}", totalSaved, code);
    }
}
