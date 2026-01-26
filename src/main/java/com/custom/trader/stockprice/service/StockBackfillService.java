package com.custom.trader.stockprice.service;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.stockprice.strategy.StockPriceStrategy;
import com.custom.trader.stockprice.strategy.StockPriceStrategyFactory;
import com.custom.trader.watchlist.entity.WatchlistStock;
import com.custom.trader.watchlist.repository.WatchlistStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockBackfillService {

    private final StockPriceStrategyFactory strategyFactory;
    private final WatchlistStockRepository watchlistStockRepository;

    /**
     * 단일 종목의 과거 가격 데이터를 백필합니다.
     *
     * <p>트랜잭션 전파: {@link Propagation#REQUIRES_NEW}
     * <ul>
     *   <li>독립적인 트랜잭션으로 실행 (부모 트랜잭션과 분리)</li>
     *   <li>백필 실패 시 해당 종목만 롤백 (다른 종목에 영향 없음)</li>
     *   <li>백필 완료 후 {@code backfillCompleted} 플래그를 명시적으로 저장</li>
     * </ul>
     * </p>
     *
     * <p>Strategy Pattern을 사용하여 AssetType에 맞는 백필 로직을 위임합니다.</p>
     *
     * @param stock 백필 대상 종목
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void backfillSingleStock(WatchlistStock stock, LocalDate startDate, LocalDate endDate) {
        AssetType assetType = stock.getAssetType();
        if (assetType == null) {
            log.warn("AssetType is null for stock: {}, skipping", stock.getStockCode());
            return;
        }

        StockPriceStrategy strategy = strategyFactory.getStrategy(assetType);
        strategy.backfillHistoricalPrices(stock, startDate, endDate);

        stock.markBackfillCompleted();
        watchlistStockRepository.save(stock);
        log.info("Backfill completed for stock: {}", stock.getStockCode());
    }
}
