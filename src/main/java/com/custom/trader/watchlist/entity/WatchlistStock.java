package com.custom.trader.watchlist.entity;

import com.custom.trader.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "watchlist_stock", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_id", "stock_code"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchlistStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private WatchlistGroup group;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", length = 100)
    private String stockName;

    @Column(name = "market_code", length = 10)
    private String marketCode;

    @Builder
    public WatchlistStock(String stockCode, String stockName, String marketCode) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.marketCode = marketCode;
    }

    void setGroup(WatchlistGroup group) {
        this.group = group;
    }

    public void updateStockInfo(String stockName, String marketCode) {
        this.stockName = stockName;
        this.marketCode = marketCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WatchlistStock that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
