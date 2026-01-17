package com.custom.trader.kis.dto.watchlist;

import java.util.List;

public record WatchlistStockResponse(
    String rtCd,
    String msgCd,
    String msg1,
    List<StockItem> output2
) {
    public record StockItem(
        String pdno,
        String prdtName,
        String mktIdCd
    ) {}
}
