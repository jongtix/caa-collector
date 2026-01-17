package com.custom.trader.kis.dto.watchlist;

import java.util.List;

public record WatchlistGroupResponse(
    String rtCd,
    String msgCd,
    String msg1,
    List<GroupItem> output2
) {
    public record GroupItem(
        String interGrpCode,
        String interGrpName
    ) {}
}
