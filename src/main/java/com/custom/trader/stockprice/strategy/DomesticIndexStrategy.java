package com.custom.trader.stockprice.strategy;

import com.custom.trader.common.constant.DateFormatConstants;
import com.custom.trader.kis.service.KisStockPriceService;
import com.custom.trader.stockprice.service.StockPricePersistenceService;
import com.custom.trader.watchlist.entity.WatchlistStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;

import static com.custom.trader.stockprice.constant.StockPriceConstants.PAGE_SIZE;

/**
 * 국내 지수 가격 수집 전략 구현체.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomesticIndexStrategy implements StockPriceStrategy {

    private final KisStockPriceService kisStockPriceService;
    private final StockPricePersistenceService persistenceService;

    @Override
    public int collectDailyPrice(WatchlistStock stock, LocalDate startDate, LocalDate endDate) {
        var prices = kisStockPriceService.getDomesticIndexDailyPrices(
                stock.getStockCode(), startDate, endDate);
        int saved = persistenceService.saveDomesticIndexPrices(stock.getStockCode(), prices);
        log.debug("Saved {} domestic index prices for: {}", saved, stock.getStockCode());
        return saved;
    }

    @Override
    public void backfillHistoricalPrices(WatchlistStock stock, LocalDate startDate, LocalDate endDate) {
        String code = stock.getStockCode();
        LocalDate currentEndDate = endDate;
        int totalSaved = 0;

        while (!currentEndDate.isBefore(startDate)) {
            var prices = kisStockPriceService.getDomesticIndexDailyPrices(code, startDate, currentEndDate);

            if (prices.isEmpty()) {
                break;
            }

            int savedCount = persistenceService.saveDomesticIndexPrices(code, prices);
            totalSaved += savedCount;

            if (prices.size() < PAGE_SIZE) {
                break;
            }

            LocalDate lastDate = prices.stream()
                    .map(p -> DateFormatConstants.parseDate(p.stckBsopDate()))
                    .min(Comparator.naturalOrder())
                    .orElseThrow(() -> new IllegalStateException("Cannot extract date from empty price list"));
            currentEndDate = lastDate.minusDays(1);
        }

        log.debug("Total saved {} domestic index prices for: {}", totalSaved, code);
    }
}
