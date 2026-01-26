package com.custom.trader.kis.dto.stockprice;

import com.custom.trader.kis.dto.KisApiResponse;

import java.util.List;

public record OverseasStockDailyPriceResponse(
    String rtCd,
    String msgCd,
    String msg1,
    Output1 output1,
    List<PriceItem> output2
) implements KisApiResponse {

    public record Output1(
        String rsym,
        String zdiv,
        String nrec
    ) {}

    public record PriceItem(
        String xymd,
        String clos,
        String open,
        String high,
        String low,
        String tvol,
        String tamt,
        String pbid,
        String pask
    ) {}
}
