package com.custom.trader.common.constant;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 날짜 포맷 관련 공통 상수.
 *
 * <p>KIS API 요청/응답에서 사용되는 날짜 포맷 관련 상수를 중앙 관리합니다.
 * 이 상수들은 kis 패키지와 stockprice 패키지 간의 순환 의존성을 해결하기 위해
 * 공통 패키지로 분리되었습니다.</p>
 *
 * <p>주요 용도:
 * <ul>
 *   <li>KIS API 요청 파라미터 포맷팅 (yyyyMMdd)</li>
 *   <li>KIS API 응답 데이터 파싱 (yyyyMMdd → LocalDate)</li>
 *   <li>백필 작업 시 기본 시작 날짜 제공</li>
 * </ul>
 * </p>
 */
public final class DateFormatConstants {

    private DateFormatConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    /**
     * 한국 표준시간대 (Asia/Seoul).
     */
    public static final String KST_ZONE = "Asia/Seoul";

    /**
     * 한국 표준시간대 ZoneId.
     */
    public static final ZoneId KST_ZONE_ID = ZoneId.of(KST_ZONE);

    /**
     * KIS API 날짜 포맷 (yyyyMMdd).
     *
     * <p>KIS Open API는 날짜 파라미터로 'yyyyMMdd' 형식을 사용합니다.
     * 예: "20240125"</p>
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

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
     * <p>KIS API 응답에서 받은 yyyyMMdd 형식의 날짜 문자열을 LocalDate 객체로 변환합니다.</p>
     *
     * @param dateStr yyyyMMdd 형식의 날짜 문자열 (예: "20240125")
     * @return 변환된 LocalDate
     * @throws java.time.format.DateTimeParseException 날짜 형식이 올바르지 않은 경우
     */
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
}
