package com.custom.trader.stockprice.constant;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
     * KIS API 날짜 포맷 (yyyyMMdd).
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 페이징 크기 (KIS API는 최대 100개 반환).
     */
    public static final int PAGE_SIZE = 100;

    /**
     * 백필 기본 시작 날짜.
     *
     * <p>1900-01-01로 설정되어 있지만, 실제로는 API가 제공하지 않는 과거 날짜부터 조회를 시도하는 것이 아닙니다.
     * {@link com.custom.trader.stockprice.service.StockBackfillService}의 페이징 로직이 데이터가 없거나 100개 미만이면 자동으로 종료하므로,
     * API의 실제 제공 범위와 무관하게 안전하게 동작합니다.</p>
     *
     * <p>종료 조건:
     * <ul>
     *   <li>API 응답이 비어있을 때 ({@code prices.isEmpty()})</li>
     *   <li>페이징 시작 날짜가 종료 날짜를 넘어갈 때</li>
     * </ul>
     * </p>
     */
    public static final LocalDate DEFAULT_START_DATE = LocalDate.of(1900, 1, 1);

    /**
     * 날짜 문자열을 LocalDate로 변환합니다.
     *
     * @param dateStr yyyyMMdd 형식의 날짜 문자열
     * @return 변환된 LocalDate
     */
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
}
