package com.custom.trader.kis.dto.stockprice;

import com.custom.trader.kis.dto.KisApiResponse;

import java.util.List;

public record DomesticStockDailyPriceResponse(
    String rtCd,
    String msgCd,
    String msg1,
    Output1 output1,
    List<PriceItem> output2
) implements KisApiResponse {

    public record Output1(
        String stckPrpr,
        String prdyVrss,
        String prdyCtrt,
        String htsKorIsnm
    ) {}

    public record PriceItem(
        String stckBsopDate,
        String stckOprc,
        String stckHgpr,
        String stckLwpr,
        String stckClpr,
        String acmlVol,
        String acmlTrPbmn
    ) {}
}