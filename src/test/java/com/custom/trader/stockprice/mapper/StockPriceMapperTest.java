package com.custom.trader.stockprice.mapper;

import com.custom.trader.kis.dto.stockprice.DomesticStockDailyPriceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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
}
