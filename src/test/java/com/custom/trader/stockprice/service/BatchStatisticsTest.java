package com.custom.trader.stockprice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BatchStatistics 단위 테스트.
 *
 * <p>배치 처리 통계 추적 로직을 검증합니다.</p>
 */
@DisplayName("BatchStatistics 단위 테스트")
class BatchStatisticsTest {

    private BatchStatistics statistics;

    @BeforeEach
    void setUp() {
        statistics = new BatchStatistics();
    }

    @Nested
    @DisplayName("초기 상태 검증")
    class InitialStateTest {

        @Test
        @DisplayName("초기값: 모든 카운터가 0")
        void initialValues() {
            assertThat(statistics.getTotal()).isZero();
            assertThat(statistics.getSuccess()).isZero();
            assertThat(statistics.getRecoverableFailure()).isZero();
            assertThat(statistics.getCriticalFailure()).isZero();
            assertThat(statistics.getUnexpectedFailure()).isZero();
        }

        @Test
        @DisplayName("초기 성공률: 0.0%")
        void initialSuccessRate() {
            assertThat(statistics.getSuccessRate()).isZero();
        }

        @Test
        @DisplayName("초기 요약 메시지")
        void initialSummary() {
            String summary = statistics.getSummary();

            assertThat(summary)
                .contains("Total: 0")
                .contains("Success: 0")
                .contains("0.00%");
        }
    }

    @Nested
    @DisplayName("카운터 증가 검증")
    class IncrementTest {

        @Test
        @DisplayName("incrementTotal() 호출 시 total 증가")
        void incrementTotal() {
            statistics.incrementTotal();
            statistics.incrementTotal();

            assertThat(statistics.getTotal()).isEqualTo(2);
        }

        @Test
        @DisplayName("incrementSuccess() 호출 시 success 증가")
        void incrementSuccess() {
            statistics.incrementSuccess();
            statistics.incrementSuccess();
            statistics.incrementSuccess();

            assertThat(statistics.getSuccess()).isEqualTo(3);
        }

        @Test
        @DisplayName("incrementRecoverableFailure() 호출 시 recoverableFailure 증가")
        void incrementRecoverableFailure() {
            statistics.incrementRecoverableFailure();

            assertThat(statistics.getRecoverableFailure()).isEqualTo(1);
        }

        @Test
        @DisplayName("incrementCriticalFailure() 호출 시 criticalFailure 증가")
        void incrementCriticalFailure() {
            statistics.incrementCriticalFailure();

            assertThat(statistics.getCriticalFailure()).isEqualTo(1);
        }

        @Test
        @DisplayName("incrementUnexpectedFailure() 호출 시 unexpectedFailure 증가")
        void incrementUnexpectedFailure() {
            statistics.incrementUnexpectedFailure();

            assertThat(statistics.getUnexpectedFailure()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("성공률 계산 검증")
    class SuccessRateTest {

        @Test
        @DisplayName("100% 성공: total=10, success=10")
        void successRate_100Percent() {
            // Given
            for (int i = 0; i < 10; i++) {
                statistics.incrementTotal();
                statistics.incrementSuccess();
            }

            // When
            double rate = statistics.getSuccessRate();

            // Then
            assertThat(rate).isEqualTo(100.0);
        }

        @Test
        @DisplayName("50% 성공: total=10, success=5")
        void successRate_50Percent() {
            // Given
            for (int i = 0; i < 10; i++) {
                statistics.incrementTotal();
            }
            for (int i = 0; i < 5; i++) {
                statistics.incrementSuccess();
            }

            // When
            double rate = statistics.getSuccessRate();

            // Then
            assertThat(rate).isEqualTo(50.0);
        }

        @Test
        @DisplayName("0% 성공: total=5, success=0")
        void successRate_0Percent() {
            // Given
            for (int i = 0; i < 5; i++) {
                statistics.incrementTotal();
            }

            // When
            double rate = statistics.getSuccessRate();

            // Then
            assertThat(rate).isZero();
        }

        @Test
        @DisplayName("소수점 성공률: total=3, success=2 → 약 66.67%")
        void successRate_Decimal() {
            // Given
            statistics.incrementTotal();
            statistics.incrementTotal();
            statistics.incrementTotal();
            statistics.incrementSuccess();
            statistics.incrementSuccess();

            // When
            double rate = statistics.getSuccessRate();

            // Then
            assertThat(rate).isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.01));
        }
    }

    @Nested
    @DisplayName("요약 메시지 검증")
    class SummaryTest {

        @Test
        @DisplayName("혼합 통계: 성공 + 실패")
        void summary_MixedStatistics() {
            // Given
            statistics.incrementTotal(); // 1
            statistics.incrementSuccess();

            statistics.incrementTotal(); // 2
            statistics.incrementRecoverableFailure();

            statistics.incrementTotal(); // 3
            statistics.incrementCriticalFailure();

            statistics.incrementTotal(); // 4
            statistics.incrementUnexpectedFailure();

            // When
            String summary = statistics.getSummary();

            // Then
            assertThat(summary)
                .contains("Total: 4")
                .contains("Success: 1")
                .contains("25.00%")
                .contains("Recoverable: 1")
                .contains("Critical: 1")
                .contains("Unexpected: 1");
        }

        @Test
        @DisplayName("전체 성공 시나리오")
        void summary_AllSuccess() {
            // Given
            for (int i = 0; i < 100; i++) {
                statistics.incrementTotal();
                statistics.incrementSuccess();
            }

            // When
            String summary = statistics.getSummary();

            // Then
            assertThat(summary)
                .contains("Total: 100")
                .contains("Success: 100")
                .contains("100.00%")
                .contains("Recoverable: 0")
                .contains("Critical: 0")
                .contains("Unexpected: 0");
        }

        @Test
        @DisplayName("전체 실패 시나리오")
        void summary_AllFailures() {
            // Given
            for (int i = 0; i < 10; i++) {
                statistics.incrementTotal();
            }
            statistics.incrementRecoverableFailure();
            statistics.incrementCriticalFailure();
            statistics.incrementUnexpectedFailure();

            // When
            String summary = statistics.getSummary();

            // Then
            assertThat(summary)
                .contains("Total: 10")
                .contains("Success: 0")
                .contains("0.00%")
                .contains("Recoverable: 1")
                .contains("Critical: 1")
                .contains("Unexpected: 1");
        }
    }

    @Nested
    @DisplayName("엣지 케이스 검증")
    class EdgeCaseTest {

        @Test
        @DisplayName("Total=0일 때 성공률은 0.0% (ZeroDivision 방지)")
        void successRate_WhenTotalIsZero() {
            // When
            double rate = statistics.getSuccessRate();

            // Then
            assertThat(rate).isZero();
        }

        @Test
        @DisplayName("대량 배치 처리: 10,000개")
        void largeScale_10000Items() {
            // Given
            for (int i = 0; i < 10000; i++) {
                statistics.incrementTotal();
                if (i % 2 == 0) {
                    statistics.incrementSuccess();
                } else {
                    statistics.incrementRecoverableFailure();
                }
            }

            // Then
            assertThat(statistics.getTotal()).isEqualTo(10000);
            assertThat(statistics.getSuccess()).isEqualTo(5000);
            assertThat(statistics.getRecoverableFailure()).isEqualTo(5000);
            assertThat(statistics.getSuccessRate()).isEqualTo(50.0);
        }
    }
}
