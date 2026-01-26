package com.custom.trader.stockprice.strategy;

import com.custom.trader.watchlist.entity.WatchlistStock;

import java.time.LocalDate;

/**
 * AssetType별 주식 가격 수집 전략 인터페이스.
 *
 * <p>Strategy Pattern을 사용하여 AssetType별 처리 로직을 분리합니다.</p>
 */
public interface StockPriceStrategy {

    /**
     * 일간 가격 수집.
     *
     * @param stock 대상 종목
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 저장된 데이터 개수
     */
    int collectDailyPrice(WatchlistStock stock, LocalDate startDate, LocalDate endDate);

    /**
     * 과거 가격 백필.
     *
     * @param stock 대상 종목
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     */
    void backfillHistoricalPrices(WatchlistStock stock, LocalDate startDate, LocalDate endDate);
}
