package com.custom.trader.watchlist.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist_stock", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_id", "stock_code"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchlistStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private WatchlistGroup group;

    private String stockCode;

    private String stockName;

    private String marketCode;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public WatchlistStock(String stockCode, String stockName, String marketCode) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.marketCode = marketCode;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    void setGroup(WatchlistGroup group) {
        this.group = group;
    }

    public void updateStockInfo(String stockName, String marketCode) {
        this.stockName = stockName;
        this.marketCode = marketCode;
    }
}
