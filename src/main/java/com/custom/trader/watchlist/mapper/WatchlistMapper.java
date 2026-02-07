package com.custom.trader.watchlist.mapper;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
import com.custom.trader.kis.dto.watchlist.WatchlistGroupResponse;
import com.custom.trader.kis.dto.watchlist.WatchlistStockResponse;
import com.custom.trader.watchlist.entity.WatchlistGroup;
import com.custom.trader.watchlist.entity.WatchlistStock;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 관심종목 DTO ↔ Entity 변환 유틸리티.
 *
 * <p>순수 Utility 클래스로, 모든 메서드는 static이며 상태를 가지지 않습니다.</p>
 *
 * <p><b>주요 역할:</b></p>
 * <ul>
 *   <li>API 응답 → Entity 변환 (toWatchlistStock, toWatchlistGroup)</li>
 *   <li>List → Map 변환 (buildApiStockMap, buildDbStockMap)</li>
 *   <li>방어적 데이터 처리 (null/blank 필터링, 중복 처리)</li>
 * </ul>
 */
@Slf4j
public final class WatchlistMapper {

    private WatchlistMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * API 종목 DTO → Entity 변환.
     *
     * @param stockCode 종목 코드
     * @param dto API 응답 종목 DTO
     * @return WatchlistStock 엔티티 (backfillCompleted=false 기본값)
     */
    public static WatchlistStock toWatchlistStock(String stockCode, WatchlistStockResponse.StockItem dto) {
        MarketCode marketCode = MarketCode.fromExcdOrDefault(dto.exchCode(), MarketCode.KRX);
        AssetType assetType = AssetType.fromFidMrktClsCode(dto.fidMrktClsCode());

        return WatchlistStock.builder()
                .stockCode(stockCode)
                .stockName(dto.htsKorIsnm())
                .marketCode(marketCode)
                .assetType(assetType)
                .build();
    }

    /**
     * API 그룹 DTO → Entity 변환.
     *
     * @param userId 사용자 ID
     * @param dto API 응답 그룹 DTO
     * @return WatchlistGroup 엔티티 (type="1" 고정)
     */
    public static WatchlistGroup toWatchlistGroup(String userId, WatchlistGroupResponse.GroupItem dto) {
        return WatchlistGroup.builder()
                .userId(userId)
                .groupCode(dto.interGrpCode())
                .groupName(dto.interGrpName())
                .type("1")
                .build();
    }

    /**
     * API 종목 리스트 → Map 변환 (stockCode → StockItem).
     *
     * <p><b>방어적 처리:</b></p>
     * <ul>
     *   <li>null/blank stockCode 필터링</li>
     *   <li>중복 stockCode 발견 시 나중 값 우선 (API 응답 순서 기준)</li>
     *   <li>중복 발견은 누적하여 일괄 로그 출력 (I/O 최적화)</li>
     * </ul>
     *
     * @param apiStocks API 응답 종목 리스트
     * @param groupCode 그룹 코드 (로그 출력용)
     * @return stockCode → StockItem Map
     */
    public static Map<String, WatchlistStockResponse.StockItem> buildApiStockMap(
            List<WatchlistStockResponse.StockItem> apiStocks,
            String groupCode) {
        List<String> duplicateCodes = new ArrayList<>();

        Map<String, WatchlistStockResponse.StockItem> result = apiStocks.stream()
                .filter(stock -> {
                    if (stock.jongCode() == null || stock.jongCode().isBlank()) {
                        log.warn("Skipping stock with null/blank stockCode in group '{}': {}", groupCode, stock);
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toMap(
                        WatchlistStockResponse.StockItem::jongCode,
                        Function.identity(),
                        (existing, duplicate) -> {
                            // 중복 코드 누적하여 마지막에 일괄 로그 출력
                            // - 이전: 중복 발생 시마다 WARN 로그 I/O (다중 중복 발생 시 성능 저하)
                            // - 개선: 중복 코드를 누적한 후 마지막에 단일 로그 I/O (I/O 최적화)
                            duplicateCodes.add(existing.jongCode());
                            return duplicate; // 나중 값 우선 (API 응답 순서 기준)
                        }
                ));

        // 중복 발견 시 일괄 로그 출력
        if (!duplicateCodes.isEmpty()) {
            log.warn("Duplicate stocks detected in group '{}': {} items have duplicate stockCodes. Using latest values.",
                    groupCode, duplicateCodes.size());
        }

        return result;
    }

    /**
     * DB 종목 엔티티 리스트 → Map 변환 (stockCode → WatchlistStock).
     *
     * @param stocks DB에서 조회한 종목 List
     * @return stockCode → WatchlistStock Map
     */
    public static Map<String, WatchlistStock> buildDbStockMap(List<WatchlistStock> stocks) {
        return stocks.stream()
                .collect(Collectors.toMap(
                        WatchlistStock::getStockCode,
                        Function.identity()
                ));
    }
}
