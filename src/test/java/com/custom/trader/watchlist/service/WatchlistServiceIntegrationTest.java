package com.custom.trader.watchlist.service;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
import com.custom.trader.testcontainers.MySQLTestcontainersConfig;
import com.custom.trader.watchlist.entity.WatchlistGroup;
import com.custom.trader.watchlist.entity.WatchlistStock;
import com.custom.trader.watchlist.repository.WatchlistGroupRepository;
import com.custom.trader.watchlist.repository.WatchlistStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(MySQLTestcontainersConfig.class)
@Transactional
class WatchlistServiceIntegrationTest {

    @Autowired
    private WatchlistGroupRepository watchlistGroupRepository;

    @Autowired
    private WatchlistStockRepository watchlistStockRepository;

    private static final String TEST_USER_ID = "testUser";

    @BeforeEach
    void setUp() {
        watchlistStockRepository.deleteAll();
        watchlistGroupRepository.deleteAll();
    }

    @Test
    @DisplayName("orphanRemoval 검증 - removeStock() 호출 시 DB에서 자동 삭제")
    void orphanRemoval_검증() {
        // given: 그룹과 종목 생성 및 저장
        WatchlistGroup group = WatchlistGroup.builder()
                .userId(TEST_USER_ID)
                .groupCode("001")
                .groupName("테스트그룹")
                .type("1")
                .build();

        WatchlistStock stock1 = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();

        WatchlistStock stock2 = WatchlistStock.builder()
                .stockCode("000660")
                .stockName("SK하이닉스")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();

        group.addStock(stock1);
        group.addStock(stock2);

        WatchlistGroup savedGroup = watchlistGroupRepository.save(group);
        watchlistGroupRepository.flush();

        Long stock1Id = savedGroup.getStocks().get(0).getId();
        Long stock2Id = savedGroup.getStocks().get(1).getId();

        // when: removeStock() 호출 (orphanRemoval 트리거)
        savedGroup.removeStock(savedGroup.getStocks().get(0)); // stock1 제거
        watchlistGroupRepository.saveAndFlush(savedGroup);

        // then: DB에서 실제로 삭제되었는지 확인
        assertThat(watchlistStockRepository.findById(stock1Id)).isEmpty(); // stock1 삭제됨
        assertThat(watchlistStockRepository.findById(stock2Id)).isPresent(); // stock2는 유지됨
        assertThat(savedGroup.getStocks()).hasSize(1);
    }

    @Test
    @DisplayName("Cascade 삭제 검증 - 그룹 삭제 시 종목도 함께 삭제")
    void Cascade_삭제_검증() {
        // given: 그룹과 종목 생성 및 저장
        WatchlistGroup group = WatchlistGroup.builder()
                .userId(TEST_USER_ID)
                .groupCode("001")
                .groupName("테스트그룹")
                .type("1")
                .build();

        WatchlistStock stock1 = WatchlistStock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();

        WatchlistStock stock2 = WatchlistStock.builder()
                .stockCode("000660")
                .stockName("SK하이닉스")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();

        group.addStock(stock1);
        group.addStock(stock2);

        WatchlistGroup savedGroup = watchlistGroupRepository.save(group);
        watchlistGroupRepository.flush();

        Long stock1Id = savedGroup.getStocks().get(0).getId();
        Long stock2Id = savedGroup.getStocks().get(1).getId();

        // when: 그룹 삭제 (Cascade 트리거)
        watchlistGroupRepository.delete(savedGroup);
        watchlistGroupRepository.flush();

        // then: 종목도 함께 삭제되었는지 확인
        assertThat(watchlistGroupRepository.findById(savedGroup.getId())).isEmpty(); // 그룹 삭제됨
        assertThat(watchlistStockRepository.findById(stock1Id)).isEmpty(); // stock1 삭제됨
        assertThat(watchlistStockRepository.findById(stock2Id)).isEmpty(); // stock2 삭제됨
        assertThat(watchlistStockRepository.count()).isZero(); // 모든 종목 삭제됨
    }

    @Test
    @DisplayName("deleteByUserIdAndGroupCodeNotIn 검증 - API에 없는 그룹 삭제")
    void deleteByUserIdAndGroupCodeNotIn_검증() {
        // given: 3개 그룹 생성
        WatchlistGroup group1 = createAndSaveGroup("001", "그룹1");
        WatchlistGroup group2 = createAndSaveGroup("002", "그룹2");
        WatchlistGroup group3 = createAndSaveGroup("003", "그룹3");

        // 각 그룹에 종목 추가
        addStockToGroup(group1, "005930", "삼성전자");
        addStockToGroup(group2, "000660", "SK하이닉스");
        addStockToGroup(group3, "035420", "NAVER");

        watchlistGroupRepository.flush();

        // when: API에는 001, 002만 있고 003은 없음 → 003 삭제
        watchlistGroupRepository.deleteByUserIdAndGroupCodeNotIn(TEST_USER_ID, java.util.List.of("001", "002"));
        watchlistGroupRepository.flush();

        // then
        var remainingGroups = watchlistGroupRepository.findByUserId(TEST_USER_ID);
        assertThat(remainingGroups).hasSize(2);
        assertThat(remainingGroups)
                .extracting(WatchlistGroup::getGroupCode)
                .containsExactlyInAnyOrder("001", "002");

        // 003 그룹의 종목도 함께 삭제되었는지 확인 (Cascade)
        var allStocks = watchlistStockRepository.findAll();
        assertThat(allStocks).hasSize(2); // 001, 002 그룹의 종목만 남음
        assertThat(allStocks)
                .extracting(WatchlistStock::getStockCode)
                .containsExactlyInAnyOrder("005930", "000660");
    }

    @Test
    @DisplayName("deleteByUserId 검증 - 특정 사용자의 모든 그룹 삭제")
    void deleteByUserId_검증() {
        // given: 2명의 사용자 데이터 생성
        WatchlistGroup userAGroup = createAndSaveGroup("001", "UserA 그룹");
        addStockToGroup(userAGroup, "005930", "삼성전자");

        WatchlistGroup userBGroup = WatchlistGroup.builder()
                .userId("otherUser")
                .groupCode("002")
                .groupName("UserB 그룹")
                .type("1")
                .build();
        WatchlistStock userBStock = WatchlistStock.builder()
                .stockCode("000660")
                .stockName("SK하이닉스")
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();
        userBGroup.addStock(userBStock);
        watchlistGroupRepository.save(userBGroup);
        watchlistGroupRepository.flush();

        // when: TEST_USER_ID의 모든 그룹 삭제
        watchlistGroupRepository.deleteByUserId(TEST_USER_ID);
        watchlistGroupRepository.flush();

        // then
        var testUserGroups = watchlistGroupRepository.findByUserId(TEST_USER_ID);
        assertThat(testUserGroups).isEmpty(); // TEST_USER_ID 그룹 삭제됨

        var otherUserGroups = watchlistGroupRepository.findByUserId("otherUser");
        assertThat(otherUserGroups).hasSize(1); // 다른 사용자 그룹은 유지됨

        // TEST_USER_ID 종목도 함께 삭제되었는지 확인
        var allStocks = watchlistStockRepository.findAll();
        assertThat(allStocks).hasSize(1); // otherUser의 종목만 남음
        assertThat(allStocks.get(0).getStockCode()).isEqualTo("000660");
    }

    private WatchlistGroup createAndSaveGroup(String groupCode, String groupName) {
        WatchlistGroup group = WatchlistGroup.builder()
                .userId(TEST_USER_ID)
                .groupCode(groupCode)
                .groupName(groupName)
                .type("1")
                .build();
        return watchlistGroupRepository.save(group);
    }

    private void addStockToGroup(WatchlistGroup group, String stockCode, String stockName) {
        WatchlistStock stock = WatchlistStock.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();
        group.addStock(stock);
        watchlistGroupRepository.save(group);
    }
}
