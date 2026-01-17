package com.custom.trader.watchlist.scheduler;

import com.custom.trader.kis.exception.KisApiException;
import com.custom.trader.watchlist.service.WatchlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WatchlistSchedulerTest {

    @Mock
    private WatchlistService watchlistService;

    private WatchlistScheduler watchlistScheduler;

    @BeforeEach
    void setUp() {
        watchlistScheduler = new WatchlistScheduler(watchlistService);
    }

    @Nested
    @DisplayName("syncWatchlist 메소드")
    class SyncWatchlist {

        @Test
        @DisplayName("스케줄 동기화 정상 실행")
        void 스케줄_동기화_정상_실행() {
            // given
            doNothing().when(watchlistService).syncWatchlist();

            // when
            watchlistScheduler.syncWatchlist();

            // then
            verify(watchlistService).syncWatchlist();
        }

        @Test
        @DisplayName("예외 발생시 로깅 후 계속 실행")
        void 예외_발생시_로깅_후_계속_실행() {
            // given
            willThrow(new RuntimeException("API 오류")).given(watchlistService).syncWatchlist();

            // when
            watchlistScheduler.syncWatchlist();

            // then
            verify(watchlistService).syncWatchlist();
        }
    }

    @Nested
    @DisplayName("예외 타입별 처리")
    class ExceptionTypeHandling {

        @Test
        @DisplayName("KisApiException 발생시 로깅 후 정상 종료")
        void KisApiException_발생시_로깅() {
            // given
            willThrow(new KisApiException("API 토큰 만료")).given(watchlistService).syncWatchlist();

            // when & then
            assertThatCode(() -> watchlistScheduler.syncWatchlist())
                    .doesNotThrowAnyException();
            verify(watchlistService).syncWatchlist();
        }

        @Test
        @DisplayName("DataIntegrityViolationException 처리")
        void DataIntegrityViolationException_처리() {
            // given
            willThrow(new DataIntegrityViolationException("중복 키 위반"))
                    .given(watchlistService).syncWatchlist();

            // when & then
            assertThatCode(() -> watchlistScheduler.syncWatchlist())
                    .doesNotThrowAnyException();
            verify(watchlistService).syncWatchlist();
        }

        @Test
        @DisplayName("NPE 발생시 스케줄러 중단 안됨")
        void NPE_발생시_스케줄러_중단_안됨() {
            // given
            willThrow(new NullPointerException("null 참조")).given(watchlistService).syncWatchlist();

            // when & then
            assertThatCode(() -> watchlistScheduler.syncWatchlist())
                    .doesNotThrowAnyException();
            verify(watchlistService).syncWatchlist();
        }
    }
}
