package com.custom.trader.kis.dto.stockprice;

import com.custom.trader.kis.dto.KisApiResponse;

import java.util.List;

public record OverseasIndexDailyPriceResponse(
    String rtCd,
    String msgCd,
    String msg1,
    Output1 output1,
    List<PriceItem> output2
) implements KisApiResponse {

    public record Output1(
        String ovrsNmixPrdyVrss,
        String prdyVrssSign,
        String prdyCtrt,
        String ovrsNmixPrdyClpr,
        String acmlVol,
        String htsKorIsnm,
        String ovrsNmixPrpr,
        String stckShrnIscd,
        String prdyVol,
        String ovrsProdOprc,
        String ovrsProdHgpr,
        String ovrsProdLwpr
    ) {}

    public record PriceItem(
        String stckBsopDate,
        String ovrsNmixPrpr,
        String ovrsNmixOprc,
        String ovrsNmixHgpr,
        String ovrsNmixLwpr,
        String acmlVol,
        String modYn
    ) {}
}