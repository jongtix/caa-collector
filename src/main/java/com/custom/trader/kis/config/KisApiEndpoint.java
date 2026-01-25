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
    ),
    DOMESTIC_STOCK_DAILY_PRICE(
        "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice",
        "FHKST03010100"
    ),
    DOMESTIC_INDEX_DAILY_PRICE(
        "/uapi/domestic-stock/v1/quotations/inquire-index-daily-price",
        "FHPUP02120000"
    ),
    OVERSEAS_STOCK_DAILY_PRICE(
        "/uapi/overseas-price/v1/quotations/dailyprice",
        "HHDFS76240000"
    ),
    OVERSEAS_INDEX_DAILY_PRICE(
        "/uapi/overseas-price/v1/quotations/inquire-daily-chartprice",
        "FHKST03030100"
    );

    private final String path;
    private final String trId;
}
