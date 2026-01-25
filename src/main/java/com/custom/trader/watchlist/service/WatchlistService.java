package com.custom.trader.watchlist.service;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        List<String> groupCodes = groups.stream()
                .map(WatchlistGroupResponse.GroupItem::interGrpCode)
                .toList();
        Map<String, WatchlistGroup> existingGroups = watchlistGroupRepository
                .findByUserIdAndGroupCodeIn(userId, groupCodes)
                .stream()
                .collect(Collectors.toMap(WatchlistGroup::getGroupCode, Function.identity()));

        List<WatchlistGroup> groupsToSave = new ArrayList<>();
        for (WatchlistGroupResponse.GroupItem groupItem : groups) {
            WatchlistGroup group = syncGroup(userId, groupItem, existingGroups);
            groupsToSave.add(group);
        }

        watchlistGroupRepository.saveAll(groupsToSave);
        log.info("Watchlist sync completed for user: {}", userId);
    }

    private WatchlistGroup syncGroup(String userId,
                                     WatchlistGroupResponse.GroupItem groupItem,
                                     Map<String, WatchlistGroup> existingGroups) {
        WatchlistGroup group = existingGroups.getOrDefault(
                groupItem.interGrpCode(),
                WatchlistGroup.builder()
                        .userId(userId)
                        .groupCode(groupItem.interGrpCode())
                        .groupName(groupItem.interGrpName())
                        .type("1")
                        .build()
        );

        group.updateGroupName(groupItem.interGrpName());
        group.clearStocks();

        List<WatchlistStockResponse.StockItem> stocks = kisWatchlistService.getStocksByGroup(groupItem.interGrpCode());

        for (WatchlistStockResponse.StockItem stockItem : stocks) {
            MarketCode marketCode = MarketCode.fromExcdOrDefault(stockItem.exchCode(), MarketCode.KRX);
            AssetType assetType = AssetType.fromFidMrktClsCode(stockItem.fidMrktClsCode());

            WatchlistStock stock = WatchlistStock.builder()
                    .stockCode(stockItem.jongCode())
                    .stockName(stockItem.htsKorIsnm())
                    .marketCode(marketCode)
                    .assetType(assetType)
                    .build();
            group.addStock(stock);
        }

        log.info("Synced group '{}' with {} stocks", groupItem.interGrpName(), stocks.size());
        return group;
    }

    @Transactional(readOnly = true)
    public List<WatchlistGroup> getWatchlistGroups(String userId) {
        return watchlistGroupRepository.findByUserId(userId);
    }
}
