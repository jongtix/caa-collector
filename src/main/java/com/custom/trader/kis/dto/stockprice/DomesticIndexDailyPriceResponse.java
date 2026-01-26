package com.custom.trader.kis.dto.stockprice;

import com.custom.trader.kis.dto.KisApiResponse;

import java.util.List;

public record DomesticIndexDailyPriceResponse(
    String rtCd,
    String msgCd,
    String msg1,
    Output1 output1,
    List<PriceItem> output2
) implements KisApiResponse {

    public record Output1(
        String bstpNmixPrpr,
        String bstpNmixPrdyVrss,
        String prdyVrssSign,
        String bstpNmixPrdyCtrt,
        String acmlVol,
        String acmlTrPbmn,
        String htsKorIsnm
    ) {}

    public record PriceItem(
        String stckBsopDate,
        String bstpNmixPrpr,
        String bstpNmixOprc,
        String bstpNmixHgpr,
        String bstpNmixLwpr,
        String acmlVol,
        String acmlTrPbmn
    ) {}
}
