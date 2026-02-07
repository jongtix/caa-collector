package com.custom.trader.stockprice.scheduler;

import com.custom.trader.stockprice.service.StockPriceCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.custom.trader.common.constant.DateFormatConstants.KST_ZONE;

/**
 * 주식 가격 수집 스케줄러.
 *
 * <p><b>정기 실행 일정:</b></p>
 * <ul>
 *   <li><b>03:00 (KST)</b>: 백필 (Backfill) - 새로 추가된 종목의 과거 데이터 수집</li>
 *   <li><b>18:30 (KST)</b>: 일간 수집 - 기존 종목의 당일 종가 데이터 수집</li>
 * </ul>
 *
 * <p><b>백필 (Backfill) 상세:</b></p>
 * <ul>
 *   <li>조건: WatchlistStock의 backfillCompleted=false인 종목만 대상</li>
 *   <li>기간: 가능한한 모든 과거 데이터 수집 (API에서 제공하는 모든 히스토리 데이터)</li>
 *   <li>방식: 페이징 처리 (100일 단위 반복 조회, API 응답 데이터 모두 소진할 때까지)</li>
 *   <li>완료 시: backfillCompleted=true로 마크하여 이후 일간 수집에만 포함</li>
 * </ul>
 *
 * <p><b>일간 수집 (Daily Collection) 상세:</b></p>
 * <ul>
 *   <li>조건: WatchlistStock의 backfillCompleted=true인 종목만 대상</li>
 *   <li>주기: 매일 장 마감 후 (18:30) 당일 종가 수집</li>
 *   <li>범위: 국내/해외 주식, 국내/해외 지수</li>
 * </ul>
 *
 * <p><b>분산 잠금 (Distributed Lock) 처리:</b></p>
 * <ul>
 *   <li>ShedLock을 사용하여 다중 인스턴스에서 중복 수집 방지</li>
 *   <li>백필: 최대 6시간 동안 추가 실행 금지 (장시간 대량 API 호출)</li>
 *   <li>일간 수집: 최대 30분 동안 추가 실행 금지</li>
 * </ul>
 *
 * <p><b>예외 처리:</b></p>
 * <ul>
 *   <li>KIS API 오류: 로깅하고 계속 진행 (다음 스케줄 재시도)</li>
 *   <li>DB 오류: 로깅하고 스케줄 중단 (심각한 오류)</li>
 *   <li>부분 성공: 일부 종목 수집 실패해도 나머지는 계속 진행</li>
 * </ul>
 *
 * @see StockPriceCollectionService 가격 수집 로직 (Facade)
 * @see AbstractBackfillStrategy 백필 페이징 로직 (Template Method Pattern)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceScheduler {

    private final StockPriceCollectionService stockPriceCollectionService;

    @Scheduled(cron = "0 0 3 * * ?", zone = KST_ZONE)
    @SchedulerLock(name = "backfillHistoricalPrices", lockAtMostFor = "PT6H", lockAtLeastFor = "PT3H")
    public void backfillHistoricalPrices() {
        log.info("Starting scheduled backfill of historical prices");
        try {
            stockPriceCollectionService.backfillHistoricalPrices();
            log.info("Scheduled backfill completed successfully");
        } catch (Exception e) {
            log.error("Scheduled backfill failed", e);
        }
    }

    @Scheduled(cron = "0 30 18 * * ?", zone = KST_ZONE)
    @SchedulerLock(name = "collectDailyPrices", lockAtMostFor = "PT30M", lockAtLeastFor = "PT10M")
    public void collectDailyPrices() {
        log.info("Starting scheduled daily price collection");
        try {
            stockPriceCollectionService.collectDailyPrices();
            log.info("Scheduled daily price collection completed successfully");
        } catch (Exception e) {
            log.error("Scheduled daily price collection failed", e);
        }
    }
}