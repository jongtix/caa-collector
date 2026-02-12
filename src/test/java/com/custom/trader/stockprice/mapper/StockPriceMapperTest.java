package com.custom.trader.stockprice.mapper;

import com.custom.trader.kis.dto.stockprice.DomesticIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.DomesticStockDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasStockDailyPriceResponse;
import com.custom.trader.testcontainers.MySQLTestcontainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(MySQLTestcontainersConfig.class)
class StockPriceMapperTest {

    @Autowired
    private StockPriceMapper mapper;

    @Test
    @DisplayName("parseBigDecimal: 정상 숫자 변환")
    void parseBigDecimal_정상값() {
        assertThat(mapper.parseBigDecimal("12345.67")).isEqualTo(new BigDecimal("12345.67"));
    }

    @Test
    @DisplayName("parseBigDecimal: null → ZERO")
    void parseBigDecimal_null() {
        assertThat(mapper.parseBigDecimal(null)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("parseBigDecimal: 'N/A' → ZERO")
    void parseBigDecimal_NA() {
        assertThat(mapper.parseBigDecimal("N/A")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("parseBigDecimal: '-' → ZERO")
    void parseBigDecimal_대시() {
        assertThat(mapper.parseBigDecimal("-")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("parseLong: 'N/A' → 0")
    void parseLong_NA() {
        assertThat(mapper.parseLong("N/A")).isEqualTo(0L);
    }

    @Test
    @DisplayName("toDomesticStock: 비정상 값 포함 API 응답 변환")
    void toDomesticStock_비정상값_처리() {
        // given
        var dto = new DomesticStockDailyPriceResponse.PriceItem(
                "20240131",
                "N/A",      // openPrice
                "72000",    // highPrice
                "-",        // lowPrice
                "71500",    // closePrice
                "N/A",      // volume
                "71000000000"
        );

        // when
        var entity = mapper.toDomesticStock("005930", dto);

        // then
        assertThat(entity.getStockCode()).isEqualTo("005930");
        assertThat(entity.getOpenPrice()).isEqualTo(BigDecimal.ZERO);  // "N/A" → ZERO
        assertThat(entity.getLowPrice()).isEqualTo(BigDecimal.ZERO);   // "-" → ZERO
        assertThat(entity.getVolume()).isEqualTo(0L);  // "N/A" → 0
    }

    @Test
    @DisplayName("toDomesticIndex: 정상 값 변환")
    void toDomesticIndex_정상값() {
        // given
        var dto = new DomesticIndexDailyPriceResponse.PriceItem(
                "20240131",      // stckBsopDate
                "2500.50",       // bstpNmixPrpr (closePrice)
                "2490.00",       // bstpNmixOprc (openPrice)
                "2510.00",       // bstpNmixHgpr (highPrice)
                "2485.00",       // bstpNmixLwpr (lowPrice)
                "1500000000",    // acmlVol (volume)
                "5000000000000"  // acmlTrPbmn (tradingValue)
        );

        // when
        var entity = mapper.toDomesticIndex("0001", dto);

        // then
        assertThat(entity.getIndexCode()).isEqualTo("0001");
        assertThat(entity.getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 31));
        assertThat(entity.getOpenPrice()).isEqualByComparingTo("2490.00");
        assertThat(entity.getHighPrice()).isEqualByComparingTo("2510.00");
        assertThat(entity.getLowPrice()).isEqualByComparingTo("2485.00");
        assertThat(entity.getClosePrice()).isEqualByComparingTo("2500.50");
        assertThat(entity.getVolume()).isEqualTo(1500000000L);
        assertThat(entity.getTradingValue()).isEqualByComparingTo("5000000000000");
    }

    @Test
    @DisplayName("toDomesticIndex: 비정상 값 포함 API 응답 변환")
    void toDomesticIndex_비정상값_처리() {
        // given
        var dto = new DomesticIndexDailyPriceResponse.PriceItem(
                "20240131",
                "2500.50",  // closePrice
                "N/A",      // openPrice
                "2510.00",  // highPrice
                "-",        // lowPrice
                "N/A",      // volume
                "5000000000000"
        );

        // when
        var entity = mapper.toDomesticIndex("0001", dto);

        // then
        assertThat(entity.getIndexCode()).isEqualTo("0001");
        assertThat(entity.getOpenPrice()).isEqualTo(BigDecimal.ZERO);  // "N/A" → ZERO
        assertThat(entity.getLowPrice()).isEqualTo(BigDecimal.ZERO);   // "-" → ZERO
        assertThat(entity.getVolume()).isEqualTo(0L);  // "N/A" → 0
    }

    @Test
    @DisplayName("toOverseasStock: 정상 값 변환")
    void toOverseasStock_정상값() {
        // given
        var dto = new OverseasStockDailyPriceResponse.PriceItem(
                "20240131",    // xymd
                "185.50",      // clos (closePrice)
                "183.00",      // open (openPrice)
                "186.00",      // high (highPrice)
                "182.50",      // low (lowPrice)
                "75000000",    // tvol (volume)
                "13875000000", // tamt (tradingValue)
                "185.40",      // pbid
                "185.60"       // pask
        );

        // when
        var entity = mapper.toOverseasStock("AAPL", "NAS", dto);

        // then
        assertThat(entity.getStockCode()).isEqualTo("AAPL");
        assertThat(entity.getExchangeCode()).isEqualTo("NAS");
        assertThat(entity.getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 31));
        assertThat(entity.getOpenPrice()).isEqualByComparingTo("183.00");
        assertThat(entity.getHighPrice()).isEqualByComparingTo("186.00");
        assertThat(entity.getLowPrice()).isEqualByComparingTo("182.50");
        assertThat(entity.getClosePrice()).isEqualByComparingTo("185.50");
        assertThat(entity.getVolume()).isEqualTo(75000000L);
        assertThat(entity.getTradingValue()).isEqualByComparingTo("13875000000");
    }

    @Test
    @DisplayName("toOverseasStock: 비정상 값 포함 API 응답 변환")
    void toOverseasStock_비정상값_처리() {
        // given
        var dto = new OverseasStockDailyPriceResponse.PriceItem(
                "20240131",
                "185.50",   // closePrice
                "N/A",      // openPrice
                "186.00",   // highPrice
                "-",        // lowPrice
                "N/A",      // volume
                "13875000000",
                "185.40",
                "185.60"
        );

        // when
        var entity = mapper.toOverseasStock("AAPL", "NAS", dto);

        // then
        assertThat(entity.getStockCode()).isEqualTo("AAPL");
        assertThat(entity.getExchangeCode()).isEqualTo("NAS");
        assertThat(entity.getOpenPrice()).isEqualTo(BigDecimal.ZERO);  // "N/A" → ZERO
        assertThat(entity.getLowPrice()).isEqualTo(BigDecimal.ZERO);   // "-" → ZERO
        assertThat(entity.getVolume()).isEqualTo(0L);  // "N/A" → 0
    }

    @Test
    @DisplayName("toOverseasIndex: 정상 값 변환")
    void toOverseasIndex_정상값() {
        // given
        var dto = new OverseasIndexDailyPriceResponse.PriceItem(
                "20240131",    // stckBsopDate
                "15500.50",    // ovrsNmixPrpr (closePrice)
                "15450.00",    // ovrsNmixOprc (openPrice)
                "15520.00",    // ovrsNmixHgpr (highPrice)
                "15430.00",    // ovrsNmixLwpr (lowPrice)
                "2500000000",  // acmlVol (volume)
                "N"            // modYn
        );

        // when
        var entity = mapper.toOverseasIndex("COMP", "NAS", dto);

        // then
        assertThat(entity.getIndexCode()).isEqualTo("COMP");
        assertThat(entity.getExchangeCode()).isEqualTo("NAS");
        assertThat(entity.getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 31));
        assertThat(entity.getOpenPrice()).isEqualByComparingTo("15450.00");
        assertThat(entity.getHighPrice()).isEqualByComparingTo("15520.00");
        assertThat(entity.getLowPrice()).isEqualByComparingTo("15430.00");
        assertThat(entity.getClosePrice()).isEqualByComparingTo("15500.50");
        assertThat(entity.getVolume()).isEqualTo(2500000000L);
        assertThat(entity.getTradingValue()).isEqualTo(BigDecimal.ZERO);  // 항상 ZERO
    }

    @Test
    @DisplayName("toOverseasIndex: 비정상 값 포함 API 응답 변환")
    void toOverseasIndex_비정상값_처리() {
        // given
        var dto = new OverseasIndexDailyPriceResponse.PriceItem(
                "20240131",
                "15500.50",  // closePrice
                "N/A",       // openPrice
                "15520.00",  // highPrice
                "-",         // lowPrice
                "N/A",       // volume
                "N"
        );

        // when
        var entity = mapper.toOverseasIndex("COMP", "NAS", dto);

        // then
        assertThat(entity.getIndexCode()).isEqualTo("COMP");
        assertThat(entity.getExchangeCode()).isEqualTo("NAS");
        assertThat(entity.getOpenPrice()).isEqualTo(BigDecimal.ZERO);  // "N/A" → ZERO
        assertThat(entity.getLowPrice()).isEqualTo(BigDecimal.ZERO);   // "-" → ZERO
        assertThat(entity.getVolume()).isEqualTo(0L);  // "N/A" → 0
        assertThat(entity.getTradingValue()).isEqualTo(BigDecimal.ZERO);  // 항상 ZERO
    }

    @Test
    @DisplayName("parseDate: 정상 날짜 변환")
    void parseDate_정상값() {
        // when
        LocalDate result = mapper.parseDate("20240131");

        // then
        assertThat(result).isEqualTo(LocalDate.of(2024, 1, 31));
    }

    @Test
    @DisplayName("parseDate: null 입력 시 예외 발생")
    void parseDate_null() {
        // when & then
        assertThatThrownBy(() -> mapper.parseDate(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("parseDate: 빈 문자열 입력 시 예외 발생")
    void parseDate_빈문자열() {
        // when & then
        assertThatThrownBy(() -> mapper.parseDate(""))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    @DisplayName("parseDate: 잘못된 형식(하이픈 포함) 입력 시 예외 발생")
    void parseDate_잘못된_형식_하이픈() {
        // when & then
        assertThatThrownBy(() -> mapper.parseDate("2024-01-31"))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    @DisplayName("parseDate: 잘못된 형식(잘못된 월) 입력 시 예외 발생")
    void parseDate_잘못된_형식_월() {
        // when & then
        assertThatThrownBy(() -> mapper.parseDate("20241301"))
                .isInstanceOf(DateTimeParseException.class);
    }
}
