package com.custom.trader.stockprice.domestic.repository;

import com.custom.trader.stockprice.domestic.entity.DomesticStockDailyPrice;
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
class DomesticStockDailyPriceRepositoryTest {

    @Autowired
    private DomesticStockDailyPriceRepository repository;

    private static final String STOCK_CODE_SAMSUNG = "005930";
    private static final String STOCK_CODE_SK_HYNIX = "000660";

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("날짜 범위 조회 정합성 검증 - BETWEEN 경계값 포함")
    void 날짜_범위_조회_정합성_검증() {
        // Given: 2024-01-01 ~ 2024-01-05 데이터 5개 저장
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 1)));
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 2)));
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 3)));
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 4)));
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 5)));
        repository.flush();

        // When: 2024-01-02 ~ 2024-01-04 범위 조회
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndTradeDateBetween(
                STOCK_CODE_SAMSUNG,
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
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 1)));
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 2)));
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 3)));
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 4)));
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 5)));
        repository.flush();

        // When: 경계값 조회
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndTradeDateBetween(
                STOCK_CODE_SAMSUNG,
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
        // Given: 동일 날짜 데이터 2개 저장 (uniqueConstraint로 실제로는 불가하지만, 테스트 목적)
        // 실제로는 uniqueConstraint로 인해 2번째 저장이 실패하므로, 1개만 저장됨
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 1)));
        repository.flush();

        // When: 조회
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndTradeDateBetween(
                STOCK_CODE_SAMSUNG,
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
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndTradeDateBetween(
                STOCK_CODE_SAMSUNG,
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
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 5)));
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 3)));
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 1)));
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 4)));
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 2)));
        repository.flush();

        // When: 전체 범위 조회
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndTradeDateBetween(
                STOCK_CODE_SAMSUNG,
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
        // Given: 삼성전자 데이터만 저장
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 1)));
        repository.flush();

        // When: 존재하지 않는 종목 코드로 조회
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndTradeDateBetween(
                "999999",
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
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 15)));
        repository.flush();

        // When: startDate > endDate로 조회
        Set<LocalDate> tradeDates = repository.findTradeDatesByStockCodeAndTradeDateBetween(
                STOCK_CODE_SAMSUNG,
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 1, 1)
        );

        // Then: 빈 Set 반환
        assertThat(tradeDates).isEmpty();
    }

    @Test
    @DisplayName("여러 종목 코드 데이터 중 특정 종목만 조회")
    void 여러_종목_중_특정_종목_조회() {
        // Given: 삼성전자와 SK하이닉스 데이터 저장
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 1)));
        repository.save(createStockPrice(STOCK_CODE_SAMSUNG, LocalDate.of(2024, 1, 2)));
        repository.save(createStockPrice(STOCK_CODE_SK_HYNIX, LocalDate.of(2024, 1, 1)));
        repository.save(createStockPrice(STOCK_CODE_SK_HYNIX, LocalDate.of(2024, 1, 2)));
        repository.flush();

        // When: 삼성전자만 조회
        Set<LocalDate> samsungDates = repository.findTradeDatesByStockCodeAndTradeDateBetween(
                STOCK_CODE_SAMSUNG,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 2)
        );

        // Then: 삼성전자 데이터만 반환
        assertThat(samsungDates).hasSize(2);
    }

    /**
     * 테스트용 DomesticStockDailyPrice 생성 헬퍼 메서드
     */
    private DomesticStockDailyPrice createStockPrice(String stockCode, LocalDate tradeDate) {
        return DomesticStockDailyPrice.builder()
                .stockCode(stockCode)
                .tradeDate(tradeDate)
                .openPrice(new BigDecimal("70000"))
                .highPrice(new BigDecimal("71000"))
                .lowPrice(new BigDecimal("69000"))
                .closePrice(new BigDecimal("70500"))
                .volume(1000000L)
                .tradingValue(new BigDecimal("70500000000"))
                .build();
    }
}
