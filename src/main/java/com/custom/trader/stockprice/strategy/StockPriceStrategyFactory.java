package com.custom.trader.stockprice.strategy;

import com.custom.trader.common.enums.AssetType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * AssetType별 StockPriceStrategy를 제공하는 Factory.
 *
 * <p>EnumMap을 사용하여 AssetType과 Strategy를 효율적으로 매핑합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class StockPriceStrategyFactory {

    private final DomesticStockStrategy domesticStockStrategy;
    private final DomesticIndexStrategy domesticIndexStrategy;
    private final OverseasStockStrategy overseasStockStrategy;
    private final OverseasIndexStrategy overseasIndexStrategy;

    private Map<AssetType, StockPriceStrategy> strategies;

    /**
     * Factory 초기화.
     *
     * <p>Spring Bean 생성 후 자동으로 호출되어 Strategy 맵을 초기화합니다.</p>
     */
    @PostConstruct
    public void init() {
        strategies = new EnumMap<>(AssetType.class);
        strategies.put(AssetType.DOMESTIC_STOCK, domesticStockStrategy);
        strategies.put(AssetType.DOMESTIC_INDEX, domesticIndexStrategy);
        strategies.put(AssetType.OVERSEAS_STOCK, overseasStockStrategy);
        strategies.put(AssetType.OVERSEAS_INDEX, overseasIndexStrategy);
    }

    /**
     * AssetType에 맞는 Strategy를 반환합니다.
     *
     * @param assetType 자산 유형
     * @return 해당 Strategy
     * @throws IllegalArgumentException assetType이 null이거나 지원하지 않는 경우
     */
    public StockPriceStrategy getStrategy(AssetType assetType) {
        if (assetType == null) {
            throw new IllegalArgumentException("AssetType cannot be null");
        }
        StockPriceStrategy strategy = strategies.get(assetType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported AssetType: " + assetType);
        }
        return strategy;
    }
}
