package com.custom.trader.stockprice.domestic.repository;

import com.custom.trader.stockprice.domestic.entity.DomesticIndexDailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

public interface DomesticIndexDailyPriceRepository extends JpaRepository<DomesticIndexDailyPrice, Long> {

    Optional<DomesticIndexDailyPrice> findTopByIndexCodeOrderByTradeDateDesc(String indexCode);

    boolean existsByIndexCodeAndTradeDate(String indexCode, LocalDate tradeDate);

    @Query("SELECT d.tradeDate FROM DomesticIndexDailyPrice d WHERE d.indexCode = :indexCode")
    Set<LocalDate> findAllTradeDatesByIndexCode(@Param("indexCode") String indexCode);
}
