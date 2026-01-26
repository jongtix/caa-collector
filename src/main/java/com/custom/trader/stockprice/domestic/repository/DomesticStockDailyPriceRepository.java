package com.custom.trader.stockprice.domestic.repository;

import com.custom.trader.stockprice.domestic.entity.DomesticStockDailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

public interface DomesticStockDailyPriceRepository extends JpaRepository<DomesticStockDailyPrice, Long> {

    Optional<DomesticStockDailyPrice> findTopByStockCodeOrderByTradeDateDesc(String stockCode);

    boolean existsByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);

    @Query("SELECT d.tradeDate FROM DomesticStockDailyPrice d WHERE d.stockCode = :stockCode")
    Set<LocalDate> findAllTradeDatesByStockCode(@Param("stockCode") String stockCode);
}