package com.custom.trader.stockprice.mapper;

import com.custom.trader.common.constant.DateFormatConstants;
import com.custom.trader.kis.dto.stockprice.DomesticIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.DomesticStockDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasStockDailyPriceResponse;
import com.custom.trader.stockprice.domestic.entity.DomesticIndexDailyPrice;
import com.custom.trader.stockprice.domestic.entity.DomesticStockDailyPrice;
import com.custom.trader.stockprice.overseas.entity.OverseasIndexDailyPrice;
import com.custom.trader.stockprice.overseas.entity.OverseasStockDailyPrice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 주식 가격 DTO ↔ Entity 변환을 담당하는 MapStruct Mapper.
 *
 * <p>컴파일 타임에 구현체가 자동 생성되며, Spring Bean으로 등록됩니다.</p>
 *
 * <p>변환 대상:
 * <ul>
 *   <li>국내 주식 일간 가격: {@link DomesticStockDailyPriceResponse.PriceItem} → {@link DomesticStockDailyPrice}</li>
 *   <li>국내 지수 일간 가격: {@link DomesticIndexDailyPriceResponse.PriceItem} → {@link DomesticIndexDailyPrice}</li>
 *   <li>해외 주식 일간 가격: {@link OverseasStockDailyPriceResponse.PriceItem} → {@link OverseasStockDailyPrice}</li>
 *   <li>해외 지수 일간 가격: {@link OverseasIndexDailyPriceResponse.PriceItem} → {@link OverseasIndexDailyPrice}</li>
 * </ul>
 * </p>
 */
@Mapper(componentModel = "spring")
public interface StockPriceMapper {

    /**
     * 국내 주식 DTO를 Entity로 변환합니다.
     *
     * @param stockCode 종목 코드 (예: "005930")
     * @param dto KIS API 응답 DTO
     * @return 변환된 Entity
     */
    @Mapping(target = "tradeDate", expression = "java(parseDate(dto.stckBsopDate()))")
    @Mapping(target = "openPrice", expression = "java(parseBigDecimal(dto.stckOprc()))")
    @Mapping(target = "highPrice", expression = "java(parseBigDecimal(dto.stckHgpr()))")
    @Mapping(target = "lowPrice", expression = "java(parseBigDecimal(dto.stckLwpr()))")
    @Mapping(target = "closePrice", expression = "java(parseBigDecimal(dto.stckClpr()))")
    @Mapping(target = "volume", expression = "java(parseLong(dto.acmlVol()))")
    @Mapping(target = "tradingValue", expression = "java(parseBigDecimal(dto.acmlTrPbmn()))")
    DomesticStockDailyPrice toDomesticStock(
            String stockCode,
            DomesticStockDailyPriceResponse.PriceItem dto
    );

    /**
     * 국내 지수 DTO를 Entity로 변환합니다.
     *
     * @param indexCode 지수 코드 (예: "0001")
     * @param dto KIS API 응답 DTO
     * @return 변환된 Entity
     */
    @Mapping(target = "tradeDate", expression = "java(parseDate(dto.stckBsopDate()))")
    @Mapping(target = "openPrice", expression = "java(parseBigDecimal(dto.bstpNmixOprc()))")
    @Mapping(target = "highPrice", expression = "java(parseBigDecimal(dto.bstpNmixHgpr()))")
    @Mapping(target = "lowPrice", expression = "java(parseBigDecimal(dto.bstpNmixLwpr()))")
    @Mapping(target = "closePrice", expression = "java(parseBigDecimal(dto.bstpNmixPrpr()))")
    @Mapping(target = "volume", expression = "java(parseLong(dto.acmlVol()))")
    @Mapping(target = "tradingValue", expression = "java(parseBigDecimal(dto.acmlTrPbmn()))")
    DomesticIndexDailyPrice toDomesticIndex(
            String indexCode,
            DomesticIndexDailyPriceResponse.PriceItem dto
    );

    /**
     * 해외 주식 DTO를 Entity로 변환합니다.
     *
     * @param stockCode 종목 코드 (예: "AAPL")
     * @param exchangeCode 거래소 코드 (예: "NAS")
     * @param dto KIS API 응답 DTO
     * @return 변환된 Entity
     */
    @Mapping(target = "tradeDate", expression = "java(parseDate(dto.xymd()))")
    @Mapping(target = "openPrice", expression = "java(parseBigDecimal(dto.open()))")
    @Mapping(target = "highPrice", expression = "java(parseBigDecimal(dto.high()))")
    @Mapping(target = "lowPrice", expression = "java(parseBigDecimal(dto.low()))")
    @Mapping(target = "closePrice", expression = "java(parseBigDecimal(dto.clos()))")
    @Mapping(target = "volume", expression = "java(parseLong(dto.tvol()))")
    @Mapping(target = "tradingValue", expression = "java(parseBigDecimal(dto.tamt()))")
    OverseasStockDailyPrice toOverseasStock(
            String stockCode,
            String exchangeCode,
            OverseasStockDailyPriceResponse.PriceItem dto
    );

    /**
     * 해외 지수 DTO를 Entity로 변환합니다.
     *
     * @param indexCode 지수 코드 (예: "COMP")
     * @param exchangeCode 거래소 코드 (예: "NAS")
     * @param dto KIS API 응답 DTO
     * @return 변환된 Entity
     */
    @Mapping(target = "tradeDate", expression = "java(parseDate(dto.stckBsopDate()))")
    @Mapping(target = "openPrice", expression = "java(parseBigDecimal(dto.ovrsNmixOprc()))")
    @Mapping(target = "highPrice", expression = "java(parseBigDecimal(dto.ovrsNmixHgpr()))")
    @Mapping(target = "lowPrice", expression = "java(parseBigDecimal(dto.ovrsNmixLwpr()))")
    @Mapping(target = "closePrice", expression = "java(parseBigDecimal(dto.ovrsNmixPrpr()))")
    @Mapping(target = "volume", expression = "java(parseLong(dto.acmlVol()))")
    @Mapping(target = "tradingValue", expression = "java(java.math.BigDecimal.ZERO)")
    OverseasIndexDailyPrice toOverseasIndex(
            String indexCode,
            String exchangeCode,
            OverseasIndexDailyPriceResponse.PriceItem dto
    );

    /**
     * 날짜 문자열을 LocalDate로 변환합니다.
     *
     * @param dateStr yyyyMMdd 형식의 날짜 문자열
     * @return 변환된 LocalDate
     */
    default LocalDate parseDate(String dateStr) {
        return DateFormatConstants.parseDate(dateStr);
    }

    /**
     * 문자열을 BigDecimal로 변환합니다.
     *
     * @param value 변환할 문자열
     * @return 변환된 BigDecimal (null이거나 빈 문자열인 경우 BigDecimal.ZERO)
     * @implNote 숫자로 변환 불가능한 값("N/A", "-", "∞" 등)은 BigDecimal.ZERO로 처리
     */
    default BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            // KIS API가 "N/A", "-", "∞" 등 비정상 값 반환 시 ZERO 처리
            return BigDecimal.ZERO;
        }
    }

    /**
     * 문자열을 Long으로 변환합니다.
     *
     * @param value 변환할 문자열
     * @return 변환된 Long (null이거나 빈 문자열인 경우 0L)
     * @implNote 숫자로 변환 불가능한 값("N/A", "-" 등)은 0L로 처리
     */
    default Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            // KIS API가 "N/A", "-" 등 비정상 값 반환 시 0 처리
            return 0L;
        }
    }
}
