package com.custom.trader.watchlist.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "watchlist_group", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"account_number", "group_code"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchlistGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;

    private String groupCode;

    private String groupName;

    private String type;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WatchlistStock> stocks = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public WatchlistGroup(String accountNumber, String groupCode, String groupName, String type) {
        this.accountNumber = accountNumber;
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.type = type;
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

    public void updateGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void addStock(WatchlistStock stock) {
        this.stocks.add(stock);
        stock.setGroup(this);
    }

    public void clearStocks() {
        this.stocks.clear();
    }
}
