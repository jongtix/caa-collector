package com.custom.trader.watchlist.repository;

import com.custom.trader.watchlist.entity.WatchlistStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistStockRepository extends JpaRepository<WatchlistStock, Long> {

    List<WatchlistStock> findByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);

    List<WatchlistStock> findByBackfillCompleted(boolean backfillCompleted);

    Page<WatchlistStock> findByBackfillCompleted(boolean backfillCompleted, Pageable pageable);
}
