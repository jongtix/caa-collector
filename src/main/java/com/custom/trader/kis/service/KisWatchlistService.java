package com.custom.trader.kis.service;

import com.custom.trader.common.util.LogMaskingUtil;
import com.custom.trader.kis.client.KisRestClient;
import com.custom.trader.kis.config.KisAccountProperties;
import com.custom.trader.kis.config.KisApiEndpoint;
import com.custom.trader.kis.config.KisProperties;
import com.custom.trader.kis.dto.watchlist.WatchlistGroupResponse;
import com.custom.trader.kis.dto.watchlist.WatchlistStockResponse;
import com.custom.trader.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisWatchlistService {

    private final KisRestClient kisRestClient;
    private final KisAuthService kisAuthService;
    private final KisProperties kisProperties;

    public List<WatchlistGroupResponse.GroupItem> getWatchlistGroups() {
        var account = kisAuthService.getDefaultAccount();
        var accessToken = kisAuthService.getAccessToken(account.name());

        log.info("Fetching watchlist groups for user: {}", LogMaskingUtil.maskUserId(kisProperties.userId()));

        var response = kisRestClient.get(
                KisApiEndpoint.WATCHLIST_GROUP,
                uriBuilder -> uriBuilder
                        .path(KisApiEndpoint.WATCHLIST_GROUP.getPath())
                        .queryParam("TYPE", "1")
                        .queryParam("FID_ETC_CLS_CODE", "00")
                        .queryParam("USER_ID", kisProperties.userId())
                        .build(),
                accessToken,
                account,
                WatchlistGroupResponse.class
        );

        log.info("Fetched {} watchlist groups", response.output2() != null ? response.output2().size() : 0);
        return response.output2() != null ? response.output2() : Collections.emptyList();
    }

    public List<WatchlistStockResponse.StockItem> getStocksByGroup(String groupCode) {
        var account = kisAuthService.getDefaultAccount();
        var accessToken = kisAuthService.getAccessToken(account.name());

        log.info("Fetching stocks for group: {}", groupCode);

        var response = kisRestClient.get(
                KisApiEndpoint.WATCHLIST_STOCK,
                uriBuilder -> uriBuilder
                        .path(KisApiEndpoint.WATCHLIST_STOCK.getPath())
                        .queryParam("TYPE", "1")
                        .queryParam("USER_ID", kisProperties.userId())
                        .queryParam("INTER_GRP_CODE", groupCode)
                        .queryParam("FID_ETC_CLS_CODE", "4")
                        .queryParam("DATA_RANK", "")
                        .queryParam("INTER_GRP_NAME", "")
                        .queryParam("HTS_KOR_ISNM", "")
                        .queryParam("CNTG_CLS_CODE", "")
                        .build(),
                accessToken,
                account,
                WatchlistStockResponse.class
        );

        log.info("Fetched {} stocks for group {}", response.output2() != null ? response.output2().size() : 0, groupCode);
        return response.output2() != null ? response.output2() : Collections.emptyList();
    }
}
