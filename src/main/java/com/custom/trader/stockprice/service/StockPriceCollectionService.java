package com.custom.trader.stockprice.service;

import com.custom.trader.common.constant.DateFormatConstants;
import com.custom.trader.common.enums.AssetType;
import com.custom.trader.kis.exception.KisApiException;
import com.custom.trader.stockprice.strategy.StockPriceStrategy;
import com.custom.trader.stockprice.strategy.StockPriceStrategyFactory;
import com.custom.trader.watchlist.entity.WatchlistStock;
import com.custom.trader.watchlist.repository.WatchlistStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

import static com.custom.trader.common.constant.DateFormatConstants.DEFAULT_START_DATE;
import static com.custom.trader.stockprice.constant.StockPriceConstants.PAGE_SIZE;

/**
 * 주식 가격 수집 오케스트레이션을 담당하는 Facade 서비스.
 *
 * <p>책임:
 * <ul>
 *   <li>페이징 처리 및 예외 처리</li>
 *   <li>Fetch/Persistence 계층 조합</li>
 *   <li>오케스트레이션 (워크플로우 관리)</li>
 * </ul>
 * </p>
 *
 * <p>트랜잭션:
 * <ul>
 *   <li>Facade 레벨에는 트랜잭션 없음</li>
 *   <li>트랜잭션은 Persistence 계층에서 종목별로 관리</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceCollectionService {

    private final WatchlistStockRepository watchlistStockRepository;
    private final StockBackfillService stockBackfillService;
    private final StockPriceStrategyFactory strategyFactory;

    /**
     * 일간 가격 수집 (백필 완료된 종목 대상).
     *
     * <p>페이징 처리를 통해 대량 종목을 안전하게 처리합니다.</p>
     */
    public void collectDailyPrices() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Slice<WatchlistStock> slice;
        BatchStatistics stats = new BatchStatistics();

        do {
            slice = watchlistStockRepository.findByBackfillCompleted(true, pageable);
            log.info("Collecting daily prices for {} stocks (page {})",
                    slice.getNumberOfElements(), slice.getNumber() + 1);

            slice.getContent().forEach(stock -> {
                stats.incrementTotal();
                try {
                    var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
                    collectDailyPriceByAssetType(stock, today, today);
                    stats.incrementSuccess();
                } catch (KisApiException e) {
                    stats.incrementRecoverableFailure();
                    log.warn("Recoverable failure for stock: {} - {}", stock.getStockCode(), e.getMessage());
                } catch (DataAccessException e) {
                    stats.incrementCriticalFailure();
                    log.error("Critical DB failure for stock: {}", stock.getStockCode(), e);
                } catch (Exception e) {
                    stats.incrementUnexpectedFailure();
                    log.error("Unexpected failure for stock: {}", stock.getStockCode(), e);
                }
            });

            pageable = slice.nextPageable();
        } while (slice.hasNext());

        log.info("Daily price collection completed. {}", stats.getSummary());

        if (stats.getCriticalFailure() > 0) {
            log.error("ALERT: {} critical database failures detected!", stats.getCriticalFailure());
        }
    }

    /**
     * 과거 가격 백필 (백필 미완료 종목 대상).
     *
     * <p>백필 작업은 {@link StockBackfillService}에 위임합니다.</p>
     */
    public void backfillHistoricalPrices() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Slice<WatchlistStock> slice;
        BatchStatistics stats = new BatchStatistics();

        do {
            slice = watchlistStockRepository.findByBackfillCompleted(false, pageable);
            log.info("Backfilling historical prices for {} stocks (page {})",
                    slice.getNumberOfElements(), slice.getNumber() + 1);

            slice.getContent().forEach(stock -> {
                stats.incrementTotal();
                try {
                    var endDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
                    stockBackfillService.backfillSingleStock(stock, DEFAULT_START_DATE, endDate);
                    stats.incrementSuccess();
                } catch (KisApiException e) {
                    stats.incrementRecoverableFailure();
                    log.warn("Recoverable failure for stock: {} - {}", stock.getStockCode(), e.getMessage());
                } catch (DataAccessException e) {
                    stats.incrementCriticalFailure();
                    log.error("Critical DB failure for stock: {}", stock.getStockCode(), e);
                } catch (Exception e) {
                    stats.incrementUnexpectedFailure();
                    log.error("Unexpected failure for stock: {}", stock.getStockCode(), e);
                }
            });

            pageable = slice.nextPageable();
        } while (slice.hasNext());

        log.info("Historical price backfill completed. {}", stats.getSummary());

        if (stats.getCriticalFailure() > 0) {
            log.error("ALERT: {} critical database failures detected!", stats.getCriticalFailure());
        }
    }

    /**
     * AssetType별 가격 수집 오케스트레이션.
     *
     * <p>Strategy Pattern을 사용하여 AssetType에 맞는 처리 로직을 위임합니다.</p>
     *
     * @param stock 대상 종목
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     */
    private void collectDailyPriceByAssetType(WatchlistStock stock, LocalDate startDate, LocalDate endDate) {
        AssetType assetType = stock.getAssetType();
        if (assetType == null) {
            log.warn("AssetType is null for stock: {}, skipping", stock.getStockCode());
            return;
        }

        StockPriceStrategy strategy = strategyFactory.getStrategy(assetType);
        strategy.collectDailyPrice(stock, startDate, endDate);
    }
}
