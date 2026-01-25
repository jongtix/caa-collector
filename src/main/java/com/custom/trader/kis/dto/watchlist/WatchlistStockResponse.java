package com.custom.trader.kis.dto.watchlist;

import com.custom.trader.kis.dto.KisApiResponse;

import java.util.List;

public record WatchlistStockResponse(
    String rtCd,
    String msgCd,
    String msg1,
    List<StockItem> output2
) implements KisApiResponse {
    public record StockItem(
        String fidMrktClsCode,
        String jongCode,
        String htsKorIsnm,
        String exchCode
    ) {}
}
