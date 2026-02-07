package com.custom.trader.watchlist.scheduler;

import com.custom.trader.watchlist.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.custom.trader.common.constant.DateFormatConstants.KST_ZONE;

/**
 * 관심종목 동기화 스케줄러.
 *
 * <p><b>정기 실행 일정:</b></p>
 * <ul>
 *   <li><b>08:00 (KST)</b>: 장 개장 전 관심종목 동기화 (사용자가 추가/삭제한 종목 반영)</li>
 *   <li><b>18:00 (KST)</b>: 장 마감 후 관심종목 동기화 (추가 변경사항 반영)</li>
 * </ul>
 *
 * <p><b>분산 잠금 (Distributed Lock) 처리:</b></p>
 * <ul>
 *   <li>ShedLock을 사용하여 다중 인스턴스 환경에서 중복 실행 방지</li>
 *   <li>동시성 보장: 최대 1시간 동안 추가 실행 금지</li>
 *   <li>최소 유지 시간: 30분 (락이 다시 획득 가능해지는 최소 시간)</li>
 * </ul>
 *
 * <p><b>예외 처리:</b></p>
 * <ul>
 *   <li>동기화 실패 시 예외를 로깅하고 계속 진행 (다음 스케줄 때 재시도)</li>
 *   <li>부분 실패: DB 트랜잭션 롤백으로 데이터 일관성 유지</li>
 * </ul>
 *
 * @see WatchlistService 3-way 동기화 로직
 * @see net.javacrumbs.shedlock.spring.annotation.SchedulerLock Redis 기반 분산 잠금
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchlistScheduler {

    private final WatchlistService watchlistService;

    @Scheduled(cron = "0 0 8,18 * * ?", zone = KST_ZONE)
    @SchedulerLock(name = "syncWatchlist", lockAtMostFor = "PT1H", lockAtLeastFor = "PT30M")
    public void syncWatchlist() {
        log.info("Starting scheduled watchlist sync");
        try {
            watchlistService.syncWatchlist();
            log.info("Scheduled watchlist sync completed successfully");
        } catch (Exception e) {
            log.error("Scheduled watchlist sync failed", e);
        }
    }
}
