package com.custom.trader.kis.dto.watchlist;

import com.custom.trader.kis.dto.KisApiResponse;

import java.util.List;

public record WatchlistGroupResponse(
    String rtCd,
    String msgCd,
    String msg1,
    List<GroupItem> output2
) implements KisApiResponse {
    public record GroupItem(
        String interGrpCode,
        String interGrpName
    ) {}
}
