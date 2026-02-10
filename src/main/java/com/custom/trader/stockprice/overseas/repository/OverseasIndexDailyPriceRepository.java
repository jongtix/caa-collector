package com.custom.trader.stockprice.overseas.repository;

import com.custom.trader.stockprice.overseas.entity.OverseasIndexDailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

public interface OverseasIndexDailyPriceRepository extends JpaRepository<OverseasIndexDailyPrice, Long> {

    Optional<OverseasIndexDailyPrice> findTopByIndexCodeAndExchangeCodeOrderByTradeDateDesc(String indexCode, String exchangeCode);

    boolean existsByIndexCodeAndExchangeCodeAndTradeDate(String indexCode, String exchangeCode, LocalDate tradeDate);

    @Query("SELECT o.tradeDate FROM OverseasIndexDailyPrice o WHERE o.indexCode = :indexCode AND o.exchangeCode = :exchangeCode AND o.tradeDate BETWEEN :startDate AND :endDate")
    Set<LocalDate> findTradeDatesByIndexCodeAndExchangeCodeAndTradeDateBetween(@Param("indexCode") String indexCode, @Param("exchangeCode") String exchangeCode, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
