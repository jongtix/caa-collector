package com.custom.trader.watchlist.entity;

import com.custom.trader.common.converter.AssetTypeConverter;
import com.custom.trader.common.converter.MarketCodeConverter;
import com.custom.trader.common.entity.BaseEntity;
import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * 관심종목 엔티티.
 * 관심종목 그룹에 속한 개별 종목 정보를 저장한다.
 */
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

    @Column(name = "market_code")
    @Convert(converter = MarketCodeConverter.class)
    private MarketCode marketCode;

    @Column(name = "asset_type")
    @Convert(converter = AssetTypeConverter.class)
    private AssetType assetType;

    /**
     * 백필 완료 여부.
     *
     * <p>라이프사이클:
     * <ul>
     *   <li><b>false (초기 상태)</b>: 종목이 처음 추가되었을 때, 히스토리 데이터 수집이 필요함</li>
     *   <li><b>true (백필 완료)</b>: {@link com.custom.trader.stockprice.service.StockPriceCollectionService#backfillHistoricalPrices()}에서
     *       과거 데이터 수집이 완료되면 {@link #markBackfillCompleted()}를 통해 true로 변경됨</li>
     * </ul>
     * </p>
     *
     * <p>사용처:
     * <ul>
     *   <li>{@link com.custom.trader.stockprice.service.StockPriceCollectionService#collectDailyPrices()}: backfillCompleted=true인 종목만 일일 수집</li>
     *   <li>{@link com.custom.trader.stockprice.service.StockPriceCollectionService#backfillHistoricalPrices()}: backfillCompleted=false인 종목만 백필</li>
     * </ul>
     * </p>
     */
    @Column(name = "backfill_completed", nullable = false)
    private boolean backfillCompleted = false;

    @Builder
    public WatchlistStock(String stockCode, String stockName, MarketCode marketCode, AssetType assetType) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.marketCode = marketCode;
        this.assetType = assetType;
    }

    void setGroup(WatchlistGroup group) {
        this.group = group;
    }

    public void updateStockInfo(String stockName, MarketCode marketCode, AssetType assetType) {
        this.stockName = stockName;
        this.marketCode = marketCode;
        this.assetType = assetType;
    }

    public void markBackfillCompleted() {
        this.backfillCompleted = true;
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
