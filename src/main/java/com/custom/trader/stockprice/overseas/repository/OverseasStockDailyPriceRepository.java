package com.custom.trader.stockprice.overseas.repository;

import com.custom.trader.stockprice.overseas.entity.OverseasStockDailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

public interface OverseasStockDailyPriceRepository extends JpaRepository<OverseasStockDailyPrice, Long> {

    Optional<OverseasStockDailyPrice> findTopByStockCodeAndExchangeCodeOrderByTradeDateDesc(String stockCode, String exchangeCode);

    boolean existsByStockCodeAndExchangeCodeAndTradeDate(String stockCode, String exchangeCode, LocalDate tradeDate);

    @Query("SELECT o.tradeDate FROM OverseasStockDailyPrice o WHERE o.stockCode = :stockCode AND o.exchangeCode = :exchangeCode AND o.tradeDate BETWEEN :startDate AND :endDate")
    Set<LocalDate> findTradeDatesByStockCodeAndExchangeCodeAndTradeDateBetween(@Param("stockCode") String stockCode, @Param("exchangeCode") String exchangeCode, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
