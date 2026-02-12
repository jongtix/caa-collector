package com.custom.trader.stockprice.overseas.repository;

import com.custom.trader.stockprice.overseas.entity.OverseasStockDailyPrice;
import com.custom.trader.testcontainers.MySQLTestcontainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(MySQLTestcontainersConfig.class)
@ActiveProfiles("test")
class OverseasStockDailyPriceRepositoryTest {

    @Autowired
    private OverseasStockDailyPriceRepository repository;

    private static final String STOCK_CODE_AAPL = "AAPL";
    private static final String STOCK_CODE_TSLA = "TSLA";
    private static final String EXCHANGE_CODE_NAS = "NAS";
    private static final String EXCHANGE_CODE_NYS = "NYS";

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("날짜 범위 조회 정합성 검증 - BETWEEN 경계값 포함")
    void 날짜_범위_조회_정합성_검증() {
        // Given: 2024-01-01 ~ 2024-01-05 데이터 5개 저장
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 1)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 2)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 3)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 4)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 5)));
        repository.flush();

        // When: 2024-01-02 ~ 2024-01-04 범위 조회
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndExchangeCodeAndTradeDateBetween(
                STOCK_CODE_AAPL,
                EXCHANGE_CODE_NAS,
                LocalDate.of(2024, 1, 2),
                LocalDate.of(2024, 1, 4)
        );

        // Then: 경계값 포함하여 3개 반환
        assertThat(tradeDates)
                .hasSize(3)
                .containsExactlyInAnyOrder(
                        LocalDate.of(2024, 1, 2),
                        LocalDate.of(2024, 1, 3),
                        LocalDate.of(2024, 1, 4)
                );
    }

    @Test
    @DisplayName("BETWEEN 경계값 검증 - startDate와 endDate 정확히 포함")
    void BETWEEN_경계값_검증() {
        // Given: 2024-01-01 ~ 2024-01-05 데이터 5개 저장
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 1)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 2)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 3)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 4)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 5)));
        repository.flush();

        // When: 경계값 조회
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndExchangeCodeAndTradeDateBetween(
                STOCK_CODE_AAPL,
                EXCHANGE_CODE_NAS,
                LocalDate.of(2024, 1, 2),
                LocalDate.of(2024, 1, 4)
        );

        // Then: startDate - 1, endDate + 1은 제외됨
        assertThat(tradeDates)
                .contains(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 4))
                .doesNotContain(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5));
    }

    @Test
    @DisplayName("중복 제거 검증 - 동일 날짜 데이터 저장 시 Set으로 중복 제거")
    void 중복_제거_검증() {
        // Given: 동일 날짜 데이터 저장 (uniqueConstraint로 실제로는 1개만 저장됨)
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 1)));
        repository.flush();

        // When: 조회
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndExchangeCodeAndTradeDateBetween(
                STOCK_CODE_AAPL,
                EXCHANGE_CODE_NAS,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 1)
        );

        // Then: Set 반환이므로 중복 없이 1개만 반환
        assertThat(tradeDates).hasSize(1);
        assertThat(tradeDates).containsExactly(LocalDate.of(2024, 1, 1));
    }

    @Test
    @DisplayName("빈 결과 처리 - 데이터가 없는 날짜 범위 조회 시 빈 Set 반환")
    void 빈_결과_처리() {
        // Given: 데이터 없음 (setUp에서 deleteAll 실행)

        // When: 존재하지 않는 날짜 범위 조회
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndExchangeCodeAndTradeDateBetween(
                STOCK_CODE_AAPL,
                EXCHANGE_CODE_NAS,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31)
        );

        // Then: 빈 Set 반환
        assertThat(tradeDates).isEmpty();
    }

    @Test
    @DisplayName("날짜 순서 검증 - 반환된 Set의 날짜들이 올바른지 확인")
    void 날짜_순서_검증() {
        // Given: 역순으로 데이터 저장
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 5)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 3)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 1)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 4)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 2)));
        repository.flush();

        // When: 전체 범위 조회
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndExchangeCodeAndTradeDateBetween(
                STOCK_CODE_AAPL,
                EXCHANGE_CODE_NAS,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 5)
        );

        // Then: 모든 날짜가 포함됨 (순서는 Set이므로 보장되지 않음)
        assertThat(tradeDates)
                .hasSize(5)
                .containsExactlyInAnyOrder(
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 1, 2),
                        LocalDate.of(2024, 1, 3),
                        LocalDate.of(2024, 1, 4),
                        LocalDate.of(2024, 1, 5)
                );
    }

    @Test
    @DisplayName("존재하지 않는 종목 코드 조회 - 빈 Set 반환")
    void 존재하지_않는_종목_코드_조회() {
        // Given: AAPL 데이터만 저장
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 1)));
        repository.flush();

        // When: 존재하지 않는 종목 코드로 조회
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndExchangeCodeAndTradeDateBetween(
                "INVALID",
                EXCHANGE_CODE_NAS,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31)
        );

        // Then: 빈 Set 반환
        assertThat(tradeDates).isEmpty();
    }

    @Test
    @DisplayName("startDate > endDate 케이스 - 빈 Set 반환")
    void startDate_endDate_역순_조회() {
        // Given: 데이터 저장
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 15)));
        repository.flush();

        // When: startDate > endDate로 조회
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndExchangeCodeAndTradeDateBetween(
                STOCK_CODE_AAPL,
                EXCHANGE_CODE_NAS,
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 1, 1)
        );

        // Then: 빈 Set 반환
        assertThat(tradeDates).isEmpty();
    }

    @Test
    @DisplayName("여러 종목 코드 데이터 중 특정 종목만 조회")
    void 여러_종목_중_특정_종목_조회() {
        // Given: AAPL과 TSLA 데이터 저장
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 1)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 2)));
        repository.save(createStockPrice(STOCK_CODE_TSLA, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 1)));
        repository.save(createStockPrice(STOCK_CODE_TSLA, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 2)));
        repository.flush();

        // When: AAPL만 조회
        Set<LocalDate> aaplDates = repository.findTradeDatesByStockCodeAndExchangeCodeAndTradeDateBetween(
                STOCK_CODE_AAPL,
                EXCHANGE_CODE_NAS,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 2)
        );

        // Then: AAPL 데이터만 반환
        assertThat(aaplDates).hasSize(2);
    }

    @Test
    @DisplayName("동일 종목 다른 거래소 데이터 조회 - 거래소 코드별 필터링")
    void 동일_종목_다른_거래소_조회() {
        // Given: AAPL NAS와 NYS 데이터 저장 (실제로는 AAPL은 NAS만 있지만 테스트용)
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NAS, LocalDate.of(2024, 1, 1)));
        repository.save(createStockPrice(STOCK_CODE_AAPL, EXCHANGE_CODE_NYS, LocalDate.of(2024, 1, 1)));
        repository.flush();

        // When: NAS만 조회
        Set<LocalDate> nasDates = repository.findTradeDatesByStockCodeAndExchangeCodeAndTradeDateBetween(
                STOCK_CODE_AAPL,
                EXCHANGE_CODE_NAS,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 1)
        );

        // When: NYS만 조회
        Set<LocalDate> nysDates = repository.findTradeDatesByStockCodeAndExchangeCodeAndTradeDateBetween(
                STOCK_CODE_AAPL,
                EXCHANGE_CODE_NYS,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 1)
        );

        // Then: 각각 1개씩 반환
        assertThat(nasDates).hasSize(1);
        assertThat(nysDates).hasSize(1);
    }

    /**
     * 테스트용 OverseasStockDailyPrice 생성 헬퍼 메서드
     */
    private OverseasStockDailyPrice createStockPrice(String stockCode, String exchangeCode, LocalDate tradeDate) {
        return OverseasStockDailyPrice.builder()
                .stockCode(stockCode)
                .exchangeCode(exchangeCode)
                .tradeDate(tradeDate)
                .openPrice(new BigDecimal("185.5000"))
                .highPrice(new BigDecimal("186.0000"))
                .lowPrice(new BigDecimal("184.0000"))
                .closePrice(new BigDecimal("185.7500"))
                .volume(50000000L)
                .tradingValue(new BigDecimal("9287500000.0000"))
                .build();
    }
}
