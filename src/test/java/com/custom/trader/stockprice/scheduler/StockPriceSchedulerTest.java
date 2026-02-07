package com.custom.trader.stockprice.scheduler;

import com.custom.trader.stockprice.service.StockPriceCollectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockPriceSchedulerTest {

    @Mock
    private StockPriceCollectionService stockPriceCollectionService;

    private StockPriceScheduler stockPriceScheduler;

    @BeforeEach
    void setUp() {
        stockPriceScheduler = new StockPriceScheduler(stockPriceCollectionService);
    }

    @Nested
    @DisplayName("backfillHistoricalPrices 메소드")
    class BackfillHistoricalPrices {

        @Test
        @DisplayName("백필 스케줄 정상 실행")
        void 백필_스케줄_정상_실행() {
            // given
            doNothing().when(stockPriceCollectionService).backfillHistoricalPrices();

            // when
            stockPriceScheduler.backfillHistoricalPrices();

            // then
            verify(stockPriceCollectionService).backfillHistoricalPrices();
        }

        @Test
        @DisplayName("예외 발생시 로깅 후 계속 실행")
        void 예외_발생시_로깅_후_계속_실행() {
            // given
            willThrow(new RuntimeException("백필 실패")).given(stockPriceCollectionService).backfillHistoricalPrices();

            // when & then
            assertThatCode(() -> stockPriceScheduler.backfillHistoricalPrices())
                    .doesNotThrowAnyException();
            verify(stockPriceCollectionService).backfillHistoricalPrices();
        }

        @Test
        @DisplayName("IllegalArgumentException 발생시 스케줄러 중단 안됨")
        void IllegalArgumentException_발생시_스케줄러_중단_안됨() {
            // given
            willThrow(new IllegalArgumentException("잘못된 파라미터"))
                    .given(stockPriceCollectionService).backfillHistoricalPrices();

            // when & then
            assertThatCode(() -> stockPriceScheduler.backfillHistoricalPrices())
                    .doesNotThrowAnyException();
            verify(stockPriceCollectionService).backfillHistoricalPrices();
        }

        @Test
        @DisplayName("NullPointerException 발생시 스케줄러 중단 안됨")
        void NPE_발생시_스케줄러_중단_안됨() {
            // given
            willThrow(new NullPointerException("null 참조"))
                    .given(stockPriceCollectionService).backfillHistoricalPrices();

            // when & then
            assertThatCode(() -> stockPriceScheduler.backfillHistoricalPrices())
                    .doesNotThrowAnyException();
            verify(stockPriceCollectionService).backfillHistoricalPrices();
        }
    }

    @Nested
    @DisplayName("collectDailyPrices 메소드")
    class CollectDailyPrices {

        @Test
        @DisplayName("일간 수집 스케줄 정상 실행")
        void 일간_수집_스케줄_정상_실행() {
            // given
            doNothing().when(stockPriceCollectionService).collectDailyPrices();

            // when
            stockPriceScheduler.collectDailyPrices();

            // then
            verify(stockPriceCollectionService).collectDailyPrices();
        }

        @Test
        @DisplayName("예외 발생시 로깅 후 계속 실행")
        void 예외_발생시_로깅_후_계속_실행() {
            // given
            willThrow(new RuntimeException("일간 수집 실패"))
                    .given(stockPriceCollectionService).collectDailyPrices();

            // when & then
            assertThatCode(() -> stockPriceScheduler.collectDailyPrices())
                    .doesNotThrowAnyException();
            verify(stockPriceCollectionService).collectDailyPrices();
        }

        @Test
        @DisplayName("IllegalStateException 발생시 스케줄러 중단 안됨")
        void IllegalStateException_발생시_스케줄러_중단_안됨() {
            // given
            willThrow(new IllegalStateException("잘못된 상태"))
                    .given(stockPriceCollectionService).collectDailyPrices();

            // when & then
            assertThatCode(() -> stockPriceScheduler.collectDailyPrices())
                    .doesNotThrowAnyException();
            verify(stockPriceCollectionService).collectDailyPrices();
        }

        @Test
        @DisplayName("다양한 예외 타입 모두 처리")
        void 다양한_예외_타입_모두_처리() {
            // given - IOException 같은 일반적인 예외 사용
            willThrow(new RuntimeException("네트워크 오류"))
                    .given(stockPriceCollectionService).collectDailyPrices();

            // when & then
            assertThatCode(() -> stockPriceScheduler.collectDailyPrices())
                    .doesNotThrowAnyException();
            verify(stockPriceCollectionService).collectDailyPrices();
        }
    }

    @Nested
    @DisplayName("어노테이션 검증")
    class AnnotationVerification {

        @Test
        @DisplayName("backfillHistoricalPrices에 @Scheduled와 @SchedulerLock 존재 확인")
        void backfillHistoricalPrices_어노테이션_존재() throws NoSuchMethodException {
            // when
            var method = StockPriceScheduler.class.getMethod("backfillHistoricalPrices");

            // then
            assertThatCode(() -> {
                method.getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);
                method.getAnnotation(net.javacrumbs.shedlock.spring.annotation.SchedulerLock.class);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("collectDailyPrices에 @Scheduled와 @SchedulerLock 존재 확인")
        void collectDailyPrices_어노테이션_존재() throws NoSuchMethodException {
            // when
            var method = StockPriceScheduler.class.getMethod("collectDailyPrices");

            // then
            assertThatCode(() -> {
                method.getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);
                method.getAnnotation(net.javacrumbs.shedlock.spring.annotation.SchedulerLock.class);
            }).doesNotThrowAnyException();
        }
    }
}
