package com.custom.trader.stockprice.scheduler;

import com.custom.trader.stockprice.service.StockPriceCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceScheduler {

    private final StockPriceCollectionService stockPriceCollectionService;

    @Scheduled(cron = "0 0 3 * * ?")
    @SchedulerLock(name = "backfillHistoricalPrices")
    public void backfillHistoricalPrices() {
        log.info("Starting scheduled backfill of historical prices");
        try {
            stockPriceCollectionService.backfillHistoricalPrices();
            log.info("Scheduled backfill completed successfully");
        } catch (Exception e) {
            log.error("Scheduled backfill failed", e);
        }
    }

    @Scheduled(cron = "0 30 18 * * ?")
    @SchedulerLock(name = "collectDailyPrices")
    public void collectDailyPrices() {
        log.info("Starting scheduled daily price collection");
        try {
            stockPriceCollectionService.collectDailyPrices();
            log.info("Scheduled daily price collection completed successfully");
        } catch (Exception e) {
            log.error("Scheduled daily price collection failed", e);
        }
    }
}