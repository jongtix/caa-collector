package com.custom.trader.watchlist.scheduler;

import com.custom.trader.watchlist.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchlistScheduler {

    private final WatchlistService watchlistService;

    @Scheduled(cron = "0 0 8,18 * * ?")
    @SchedulerLock(name = "syncWatchlist")
    public void syncWatchlist() {
        log.info("Starting scheduled watchlist sync");
        try {
            watchlistService.syncWatchlist();
            log.info("Scheduled watchlist sync completed successfully");
        } catch (Exception e) {
            log.error("Scheduled watchlist sync failed", e);
        }
    }
}
