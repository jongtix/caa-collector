package com.custom.trader.watchlist.service;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
import com.custom.trader.kis.config.KisProperties;
import com.custom.trader.kis.dto.watchlist.WatchlistGroupResponse;
import com.custom.trader.kis.dto.watchlist.WatchlistStockResponse;
import com.custom.trader.kis.service.KisWatchlistService;
import com.custom.trader.watchlist.entity.WatchlistGroup;
import com.custom.trader.watchlist.entity.WatchlistStock;
import com.custom.trader.watchlist.repository.WatchlistGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 관심종목 동기화 서비스.
 *
 * <p><b>3-way 동기화 (Three-Way Synchronization) 구현:</b></p>
 * <ul>
 *   <li><b>API</b>: 한국투자증권 오픈 API에서 관심종목 데이터 조회</li>
 *   <li><b>DB</b>: 로컬 MySQL에 저장된 기존 관심종목 데이터</li>
 *   <li><b>병합</b>: API와 DB의 데이터를 비교하여 추가/수정/삭제 수행</li>
 * </ul>
 *
 * <p><b>동기화 흐름:</b></p>
 * <ol>
 *   <li>API에서 모든 그룹 조회 (getWatchlistGroups)</li>
 *   <li>API에서 각 그룹의 종목 조회 (getStocksByGroup)</li>
 *   <li>DB의 기존 데이터와 비교 (diffDetection)</li>
 *   <li>변경사항 적용 (Upsert: Create/Update, Delete)</li>
 *   <li>백필 플래그 보존 (backfillCompleted 상태 유지)</li>
 * </ol>
 *
 * <p><b>트랜잭션 처리:</b></p>
 * <ul>
 *   <li>전체 동기화 작업이 하나의 트랜잭션으로 처리 (원자성 보장)</li>
 *   <li>부분 실패 시 전체 롤백 (데이터 일관성)</li>
 *   <li>Cascade 삭제: 그룹 삭제 시 포함된 모든 종목도 삭제</li>
 * </ul>
 *
 * @see WatchlistGroup 관심종목 그룹 엔티티
 * @see WatchlistStock 관심종목 엔티티
 * @see WatchlistScheduler 정기 동기화 스케줄러
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final KisWatchlistService kisWatchlistService;
    private final WatchlistGroupRepository watchlistGroupRepository;
    private final KisProperties kisProperties;

    @Transactional
    public void syncWatchlist() {
        String userId = kisProperties.userId();
        log.info("Starting watchlist sync for user: {}", userId);

        // 1. API에서 모든 그룹 조회
        List<WatchlistGroupResponse.GroupItem> apiGroups = kisWatchlistService.getWatchlistGroups();
        log.info("Found {} groups from API", apiGroups.size());

        // 2. API 그룹 코드 추출
        List<String> apiGroupCodes = apiGroups.stream()
                .map(WatchlistGroupResponse.GroupItem::interGrpCode)
                .toList();

        // 3. API에 없는 그룹 삭제 (Cascade로 종목도 함께 삭제)
        if (!apiGroupCodes.isEmpty()) {
            watchlistGroupRepository.deleteByUserIdAndGroupCodeNotIn(userId, apiGroupCodes);
            log.info("Deleted groups not in API response");
        } else {
            watchlistGroupRepository.deleteByUserId(userId);
            log.info("API returned empty groups, deleted all groups");
            return;
        }

        // 4. DB에서 API에 있는 그룹만 조회
        Map<String, WatchlistGroup> existingGroups = watchlistGroupRepository
                .findByUserIdAndGroupCodeIn(userId, apiGroupCodes)
                .stream()
                .collect(Collectors.toMap(WatchlistGroup::getGroupCode, Function.identity()));

        // 5. 각 그룹별 동기화
        // 의도된 순차 호출: KIS API는 그룹별 종목 조회 엔드포인트만 제공하므로 그룹 단위 순차 호출이 필수
        List<WatchlistGroup> groupsToSave = new ArrayList<>();
        for (WatchlistGroupResponse.GroupItem apiGroup : apiGroups) {
            WatchlistGroup group = syncGroup(userId, apiGroup, existingGroups);
            groupsToSave.add(group);
        }

        // 6. 배치 저장
        watchlistGroupRepository.saveAll(groupsToSave);
        log.info("Watchlist sync completed for user: {}", userId);
    }

    private WatchlistGroup syncGroup(String userId,
                                     WatchlistGroupResponse.GroupItem groupItem,
                                     Map<String, WatchlistGroup> existingGroups) {
        // 1. 그룹 생성 또는 조회
        WatchlistGroup group = existingGroups.getOrDefault(
                groupItem.interGrpCode(),
                WatchlistGroup.builder()
                        .userId(userId)
                        .groupCode(groupItem.interGrpCode())
                        .groupName(groupItem.interGrpName())
                        .type("1")
                        .build()
        );

        // 2. 그룹명 업데이트
        group.updateGroupName(groupItem.interGrpName());

        // 3. API에서 종목 목록 조회
        List<WatchlistStockResponse.StockItem> apiStocks =
                kisWatchlistService.getStocksByGroup(groupItem.interGrpCode());

        // 4. 종목 3-way 동기화 (핵심 개선)
        int syncedCount = syncStocks(group, apiStocks);

        log.info("Synced group '{}' with {} stocks (from {} raw API items)",
                groupItem.interGrpName(), syncedCount, apiStocks.size());
        return group;
    }

    /**
     * 그룹 내 종목 3-way 동기화.
     *
     * <ul>
     *   <li>API와 DB 모두에 있는 종목: updateStockInfo() 호출, backfillCompleted 보존</li>
     *   <li>API에만 있는 종목: addStock() 호출 (backfillCompleted=false)</li>
     *   <li>DB에만 있는 종목: removeStock() 호출 (orphanRemoval로 자동 삭제)</li>
     * </ul>
     *
     * <p><b>방어적 처리:</b> API가 null stockCode 또는 중복 stockCode를 반환할 경우
     * 해당 데이터를 무시하고 경고 로그를 기록한다.</p>
     *
     * @return 실제로 동기화된 종목 개수 (null/중복 제거 후)
     */
    private int syncStocks(WatchlistGroup group, List<WatchlistStockResponse.StockItem> apiStocks) {
        // 1. API 종목을 Map으로 변환 (stockCode → StockItem)
        // 방어적 처리: null stockCode 필터링, 중복 시 나중 값 우선 (merge 함수)
        Map<String, WatchlistStockResponse.StockItem> apiStockMap = apiStocks.stream()
                .filter(stock -> {
                    if (stock.jongCode() == null || stock.jongCode().isBlank()) {
                        log.warn("Skipping stock with null/blank stockCode in group '{}': {}",
                                group.getGroupCode(), stock);
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toMap(
                        WatchlistStockResponse.StockItem::jongCode,
                        Function.identity(),
                        (existing, duplicate) -> {
                            log.warn("Duplicate stockCode '{}' in group '{}'. API returned: existing={}, duplicate={}. Using latest value.",
                                    existing.jongCode(), group.getGroupCode(),
                                    existing.htsKorIsnm(), duplicate.htsKorIsnm());
                            return duplicate; // 나중 값 우선 (API 응답 순서 기준)
                        }
                ));

        // 2. DB 종목을 Map으로 변환 (stockCode → WatchlistStock)
        Map<String, WatchlistStock> dbStockMap = group.getStocks().stream()
                .collect(Collectors.toMap(
                        WatchlistStock::getStockCode,
                        Function.identity()
                ));

        // 3. 삭제할 종목 식별 (DB에만 있고 API에 없는 종목)
        List<WatchlistStock> stocksToRemove = dbStockMap.values().stream()
                .filter(dbStock -> !apiStockMap.containsKey(dbStock.getStockCode()))
                .toList();

        // 4. 삭제 실행
        stocksToRemove.forEach(group::removeStock);
        if (!stocksToRemove.isEmpty()) {
            log.debug("Removed {} stocks from group '{}'", stocksToRemove.size(), group.getGroupName());
        }

        // 5. 추가/업데이트할 종목 처리 (필터링된 Map 기준)
        for (WatchlistStockResponse.StockItem apiStock : apiStockMap.values()) {
            String stockCode = apiStock.jongCode();
            WatchlistStock dbStock = dbStockMap.get(stockCode);

            MarketCode marketCode = MarketCode.fromExcdOrDefault(apiStock.exchCode(), MarketCode.KRX);
            AssetType assetType = AssetType.fromFidMrktClsCode(apiStock.fidMrktClsCode());

            if (dbStock != null) {
                // 기존 종목: 정보만 업데이트, backfillCompleted 보존
                dbStock.updateStockInfo(apiStock.htsKorIsnm(), marketCode, assetType);
                log.debug("Updated stock '{}' (backfillCompleted={})",
                        stockCode, dbStock.isBackfillCompleted());
            } else {
                // 신규 종목: 추가 (backfillCompleted=false로 자동 초기화)
                WatchlistStock newStock = WatchlistStock.builder()
                        .stockCode(stockCode)
                        .stockName(apiStock.htsKorIsnm())
                        .marketCode(marketCode)
                        .assetType(assetType)
                        .build();
                group.addStock(newStock);
                log.debug("Added new stock '{}' (backfillCompleted=false)", stockCode);
            }
        }

        return apiStockMap.size();
    }

    @Transactional(readOnly = true)
    public List<WatchlistGroup> getWatchlistGroups(String userId) {
        return watchlistGroupRepository.findByUserId(userId);
    }
}
