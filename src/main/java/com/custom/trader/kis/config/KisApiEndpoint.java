package com.custom.trader.kis.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KisApiEndpoint {

    WATCHLIST_GROUP(
        "/uapi/domestic-stock/v1/quotations/intstock-grouplist",
        "HHKCM113004C7"
    ),
    WATCHLIST_STOCK(
        "/uapi/domestic-stock/v1/quotations/intstock-stocklist-by-group",
        "HHKCM113004C6"
    );

    private final String path;
    private final String trId;
}
