package com.custom.trader.stockprice.domestic.entity;

import com.custom.trader.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "domestic_stock_daily_price", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"stock_code", "trade_date"})
}, indexes = {
    @Index(name = "idx_domestic_stock_daily_price_code_date", columnList = "stock_code, trade_date DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DomesticStockDailyPrice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "open_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal closePrice;

    @Column(name = "volume", nullable = false)
    private Long volume;

    @Column(name = "trading_value", precision = 20, scale = 2)
    private BigDecimal tradingValue;

    @Builder
    public DomesticStockDailyPrice(String stockCode, LocalDate tradeDate,
                                    BigDecimal openPrice, BigDecimal highPrice,
                                    BigDecimal lowPrice, BigDecimal closePrice,
                                    Long volume, BigDecimal tradingValue) {
        this.stockCode = stockCode;
        this.tradeDate = tradeDate;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.tradingValue = tradingValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomesticStockDailyPrice that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}