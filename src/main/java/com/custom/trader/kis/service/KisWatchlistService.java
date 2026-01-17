package com.custom.trader.kis.service;

import com.custom.trader.kis.config.KisAccountProperties;
import com.custom.trader.kis.config.KisApiEndpoint;
import com.custom.trader.kis.config.KisProperties;
import com.custom.trader.kis.dto.watchlist.WatchlistGroupResponse;
import com.custom.trader.kis.dto.watchlist.WatchlistStockResponse;
import com.custom.trader.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisWatchlistService {

    private final RestClient kisRestClient;
    private final KisAuthService kisAuthService;
    private final KisProperties kisProperties;

    public List<WatchlistGroupResponse.GroupItem> getWatchlistGroups() {
        KisAccountProperties account = getDefaultAccount();
        String accessToken = kisAuthService.getAccessToken(account.name());

        log.info("Fetching watchlist groups for user: {}", kisProperties.userId());

        WatchlistGroupResponse response = kisRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(KisApiEndpoint.WATCHLIST_GROUP.getPath())
                        .queryParam("TYPE", "1")
                        .queryParam("FID_ETC_CLS_CODE", "00")
                        .queryParam("USER_ID", kisProperties.userId())
                        .build())
                .header("authorization", "Bearer " + accessToken)
                .header("appkey", account.appKey())
                .header("appsecret", account.appSecret())
                .header("tr_id", KisApiEndpoint.WATCHLIST_GROUP.getTrId())
                .header("custtype", "P")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(WatchlistGroupResponse.class);

        if (response == null || !"0".equals(response.rtCd())) {
            String errorMsg = response != null ? response.msg1() : "Unknown error";
            log.error("Failed to fetch watchlist groups: {}", errorMsg);
            throw new KisApiException("Failed to fetch watchlist groups: " + errorMsg);
        }

        log.info("Fetched {} watchlist groups", response.output2() != null ? response.output2().size() : 0);
        return response.output2() != null ? response.output2() : Collections.emptyList();
    }

    public List<WatchlistStockResponse.StockItem> getStocksByGroup(String groupCode) {
        KisAccountProperties account = getDefaultAccount();
        String accessToken = kisAuthService.getAccessToken(account.name());

        log.info("Fetching stocks for group: {}", groupCode);

        WatchlistStockResponse response = kisRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(KisApiEndpoint.WATCHLIST_STOCK.getPath())
                        .queryParam("TYPE", "1")
                        .queryParam("USER_ID", kisProperties.userId())
                        .queryParam("INTER_GRP_CODE", groupCode)
                        .queryParam("FID_ETC_CLS_CODE", "4")
                        .queryParam("DATA_RANK", "")
                        .queryParam("INTER_GRP_NAME", "")
                        .queryParam("HTS_KOR_ISNM", "")
                        .queryParam("CNTG_CLS_CODE", "")
                        .build())
                .header("authorization", "Bearer " + accessToken)
                .header("appkey", account.appKey())
                .header("appsecret", account.appSecret())
                .header("tr_id", KisApiEndpoint.WATCHLIST_STOCK.getTrId())
                .header("custtype", "P")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(WatchlistStockResponse.class);

        if (response == null || !"0".equals(response.rtCd())) {
            String errorMsg = response != null ? response.msg1() : "Unknown error";
            log.error("Failed to fetch stocks for group {}: {}", groupCode, errorMsg);
            throw new KisApiException("Failed to fetch stocks for group " + groupCode + ": " + errorMsg);
        }

        log.info("Fetched {} stocks for group {}", response.output2() != null ? response.output2().size() : 0, groupCode);
        return response.output2() != null ? response.output2() : Collections.emptyList();
    }

    private KisAccountProperties getDefaultAccount() {
        if (kisProperties.accounts().isEmpty()) {
            throw new KisApiException("No accounts configured");
        }
        return kisProperties.accounts().getFirst();
    }
}
