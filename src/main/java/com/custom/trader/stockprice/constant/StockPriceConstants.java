package com.custom.trader.stockprice.constant;

/**
 * 주식 가격 수집 도메인 공통 상수.
 *
 * <p>주식 가격 데이터 수집 관련 서비스에서 공통으로 사용하는 상수를 중앙 관리합니다.</p>
 */
public final class StockPriceConstants {

    private StockPriceConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    /**
     * 페이징 크기 (KIS API는 최대 100개 반환).
     */
    public static final int PAGE_SIZE = 100;
}
