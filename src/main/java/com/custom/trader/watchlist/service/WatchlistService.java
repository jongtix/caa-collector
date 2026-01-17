package com.custom.trader.watchlist.service;

import com.custom.trader.kis.config.KisProperties;
import com.custom.trader.kis.dto.watchlist.WatchlistGroupResponse;
import com.custom.trader.kis.dto.watchlist.WatchlistStockResponse;
import com.custom.trader.kis.service.KisWatchlistService;
import com.custom.trader.watchlist.entity.WatchlistGroup;
import com.custom.trader.watchlist.entity.WatchlistStock;
import com.custom.trader.watchlist.repository.WatchlistGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final KisWatchlistService kisWatchlistService;
    private final WatchlistGroupRepository watchlistGroupRepository;
    private final KisProperties kisProperties;

    @Transactional
    public void syncWatchlist() {
        String userId = kisProperties.userId();
        log.info("Starting watchlist sync for user: {}", userId);

        List<WatchlistGroupResponse.GroupItem> groups = kisWatchlistService.getWatchlistGroups();
        log.info("Found {} groups to sync", groups.size());

        for (WatchlistGroupResponse.GroupItem groupItem : groups) {
            syncGroup(userId, groupItem);
        }

        log.info("Watchlist sync completed for user: {}", userId);
    }

    private void syncGroup(String userId, WatchlistGroupResponse.GroupItem groupItem) {
        WatchlistGroup group = watchlistGroupRepository
                .findByAccountNumberAndGroupCode(userId, groupItem.interGrpCode())
                .orElseGet(() -> WatchlistGroup.builder()
                        .accountNumber(userId)
                        .groupCode(groupItem.interGrpCode())
                        .groupName(groupItem.interGrpName())
                        .type("1")
                        .build());

        group.updateGroupName(groupItem.interGrpName());
        group.clearStocks();

        List<WatchlistStockResponse.StockItem> stocks = kisWatchlistService.getStocksByGroup(groupItem.interGrpCode());

        for (WatchlistStockResponse.StockItem stockItem : stocks) {
            WatchlistStock stock = WatchlistStock.builder()
                    .stockCode(stockItem.pdno())
                    .stockName(stockItem.prdtName())
                    .marketCode(stockItem.mktIdCd())
                    .build();
            group.addStock(stock);
        }

        watchlistGroupRepository.save(group);
        log.info("Synced group '{}' with {} stocks", groupItem.interGrpName(), stocks.size());
    }

    @Transactional(readOnly = true)
    public List<WatchlistGroup> getWatchlistGroups(String userId) {
        return watchlistGroupRepository.findByAccountNumber(userId);
    }
}
