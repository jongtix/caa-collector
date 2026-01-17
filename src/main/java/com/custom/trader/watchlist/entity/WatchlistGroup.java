package com.custom.trader.watchlist.entity;

import com.custom.trader.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "watchlist_group", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"account_number", "group_code"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchlistGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "group_code", nullable = false, length = 10)
    private String groupCode;

    @Column(name = "group_name", length = 100)
    private String groupName;

    @Column(length = 20)
    private String type;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WatchlistStock> stocks = new ArrayList<>();

    @Builder
    public WatchlistGroup(String accountNumber, String groupCode, String groupName, String type) {
        this.accountNumber = accountNumber;
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.type = type;
    }

    public void updateGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<WatchlistStock> getStocks() {
        return Collections.unmodifiableList(stocks);
    }

    public void addStock(WatchlistStock stock) {
        this.stocks.add(stock);
        stock.setGroup(this);
    }

    public void removeStock(WatchlistStock stock) {
        this.stocks.remove(stock);
        stock.setGroup(null);
    }

    public void clearStocks() {
        this.stocks.forEach(stock -> stock.setGroup(null));
        this.stocks.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WatchlistGroup that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
