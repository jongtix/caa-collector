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
import com.custom.trader.watchlist.repository.WatchlistStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * WatchlistService의 3-way 동기화 로직 통합 테스트.
 *
 * <p>목적: syncWatchlist()의 복잡한 3-way 동기화 로직(추가/업데이트/삭제)을
 * 실제 DB 상태로 검증합니다.</p>
 *
 * <p>기술 스택:
 * <ul>
 *   <li>@SpringBootTest: 전체 컨텍스트 로드 (JPA, 트랜잭션 지원)</li>
 *   <li>@MockBean: KisWatchlistService API 호출만 모킹</li>
 *   <li>실제 Repository: WatchlistGroupRepository, WatchlistStockRepository는 실제 DB 사용</li>
 *   <li>H2 In-Memory DB: application-test.yml에서 설정</li>
 * </ul>
 * </p>
 *
 * <p>검증 범위:
 * <ul>
 *   <li>신규 종목 추가 (API에만 있음)</li>
 *   <li>기존 종목 업데이트 (API ↔ DB 모두 있음)</li>
 *   <li>삭제된 종목 제거 (DB에만 있음)</li>
 *   <li>백필 플래그 보존 (업데이트 시 backfillCompleted=true 유지)</li>
 *   <li>Cascade & orphanRemoval 동작 확인</li>
 *   <li>복합 시나리오 (추가/업데이트/삭제 동시 발생)</li>
 * </ul>
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WatchlistServiceSyncIntegrationTest {

    @Autowired
    private WatchlistService watchlistService;

    @Autowired
    private WatchlistGroupRepository watchlistGroupRepository;

    @Autowired
    private WatchlistStockRepository watchlistStockRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private KisWatchlistService kisWatchlistService;

    @MockBean
    private KisProperties kisProperties;

    private static final String TEST_USER_ID = "testUser";

    @BeforeEach
    void setUp() {
        // 각 테스트 시작 전 DB 초기화
        watchlistStockRepository.deleteAll();
        watchlistGroupRepository.deleteAll();

        // KisProperties 모킹
        given(kisProperties.userId()).willReturn(TEST_USER_ID);
    }

    @Nested
    @DisplayName("기본 3-way 동기화 시나리오")
    class BasicThreeWaySync {

        @Test
        @DisplayName("신규 종목 추가 - API에만 있는 종목이 DB에 추가됨")
        void 신규_종목_추가() {
            // given: DB는 비어있음
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "신규그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX"),
                    new WatchlistStockResponse.StockItem("J", "000660", "SK하이닉스", "KRX")
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);

            // when
            watchlistService.syncWatchlist();

            // then: 실제 DB 상태 확인
            var groups = watchlistGroupRepository.findByUserId(TEST_USER_ID);
            assertThat(groups).hasSize(1);

            var group = groups.get(0);
            assertThat(group.getGroupCode()).isEqualTo("001");
            assertThat(group.getGroupName()).isEqualTo("신규그룹");

            // 종목도 DB에 저장되었는지 확인
            var stocks = watchlistStockRepository.findAll();
            assertThat(stocks).hasSize(2);
            assertThat(stocks)
                    .extracting(WatchlistStock::getStockCode)
                    .containsExactlyInAnyOrder("005930", "000660");
            assertThat(stocks)
                    .extracting(WatchlistStock::getStockName)
                    .containsExactlyInAnyOrder("삼성전자", "SK하이닉스");

            // 신규 종목은 backfillCompleted=false
            assertThat(stocks).allMatch(stock -> !stock.isBackfillCompleted());
        }

        @Test
        @DisplayName("기존 종목 업데이트 - API와 DB 모두 있는 종목의 정보 업데이트")
        void 기존_종목_업데이트() {
            // given: DB에 기존 그룹 및 종목 저장
            var existingGroup = createAndSaveGroup("001", "기존그룹");
            createAndAddStock(existingGroup, "005930", "삼성전자(구버전)");
            watchlistGroupRepository.flush();

            // flush 후 ID 조회
            var stocks = watchlistStockRepository.findAll();
            Long stockId = stocks.get(0).getId();

            // API는 종목명을 변경하여 반환
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "기존그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자(신버전)", "KRX")
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);

            // when
            watchlistService.syncWatchlist();

            // then: 실제 DB에서 업데이트 확인
            var updatedStock = watchlistStockRepository.findById(stockId).orElseThrow();
            assertThat(updatedStock.getStockName()).isEqualTo("삼성전자(신버전)");
            assertThat(updatedStock.getStockCode()).isEqualTo("005930");

            // 종목 개수는 그대로 1개
            assertThat(watchlistStockRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("삭제된 종목 제거 - DB에만 있고 API에 없는 종목이 DB에서 삭제됨")
        void 삭제된_종목_제거() {
            // given: DB에 2개 종목 저장
            var existingGroup = createAndSaveGroup("001", "테스트그룹");
            createAndAddStock(existingGroup, "005930", "삼성전자");
            createAndAddStock(existingGroup, "000660", "SK하이닉스");
            watchlistGroupRepository.flush();

            // flush 후 ID 조회
            var stocks = watchlistStockRepository.findAll();
            Long stock1Id = stocks.stream().filter(s -> "005930".equals(s.getStockCode())).findFirst().get().getId();
            Long stock2Id = stocks.stream().filter(s -> "000660".equals(s.getStockCode())).findFirst().get().getId();

            // API는 stock1만 반환 (stock2는 삭제됨)
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);

            // when
            watchlistService.syncWatchlist();
            entityManager.flush(); // orphanRemoval 트리거
            entityManager.clear(); // 영속성 컨텍스트 초기화

            // then: 실제 DB에서 삭제 확인
            assertThat(watchlistStockRepository.findById(stock1Id)).isPresent(); // stock1 유지
            assertThat(watchlistStockRepository.findById(stock2Id)).isEmpty(); // stock2 삭제됨
            assertThat(watchlistStockRepository.count()).isEqualTo(1);

            var remainingStocks = watchlistStockRepository.findAll();
            assertThat(remainingStocks).hasSize(1);
            assertThat(remainingStocks.get(0).getStockCode()).isEqualTo("005930");
        }
    }

    @Nested
    @DisplayName("백필 플래그 보존 검증")
    class BackfillFlagPreservation {

        @Test
        @DisplayName("기존 종목 업데이트 시 backfillCompleted=true 보존")
        void 백필_완료_종목_업데이트_시_플래그_보존() {
            // given: 백필 완료된 종목 저장
            var existingGroup = createAndSaveGroup("001", "테스트그룹");
            createAndAddStock(existingGroup, "005930", "삼성전자");
            watchlistGroupRepository.flush();

            // flush 후 백필 완료 표시 및 ID 조회
            var existingStock = watchlistStockRepository.findAll().get(0);
            existingStock.markBackfillCompleted();
            watchlistStockRepository.flush();
            Long stockId = existingStock.getId();

            // API는 종목명을 변경하여 반환
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자(변경됨)", "KRX")
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);

            // when
            watchlistService.syncWatchlist();

            // then: 백필 플래그 보존 확인
            var updatedStock = watchlistStockRepository.findById(stockId).orElseThrow();
            assertThat(updatedStock.getStockName()).isEqualTo("삼성전자(변경됨)"); // 이름 업데이트
            assertThat(updatedStock.isBackfillCompleted()).isTrue(); // 백필 플래그 보존
        }

        @Test
        @DisplayName("신규 종목 추가 시 backfillCompleted=false 초기화")
        void 신규_종목_추가_시_플래그_false() {
            // given: 기존 그룹만 있고 종목 없음
            createAndSaveGroup("001", "테스트그룹");

            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);

            // when
            watchlistService.syncWatchlist();

            // then: 신규 종목은 backfillCompleted=false
            var stocks = watchlistStockRepository.findAll();
            assertThat(stocks).hasSize(1);
            assertThat(stocks.get(0).isBackfillCompleted()).isFalse();
        }

        @Test
        @DisplayName("백필 완료/미완료 혼재 시 각각 올바르게 처리")
        void 백필_완료_미완료_혼재_처리() {
            // given: 백필 완료된 종목 1개, 미완료 종목 1개
            var existingGroup = createAndSaveGroup("001", "테스트그룹");
            createAndAddStock(existingGroup, "005930", "삼성전자");
            createAndAddStock(existingGroup, "000660", "SK하이닉스");
            watchlistGroupRepository.flush();

            // flush 후 백필 완료 표시 및 ID 조회
            var stocks = watchlistStockRepository.findAll();
            var completedStock = stocks.stream().filter(s -> "005930".equals(s.getStockCode())).findFirst().get();
            completedStock.markBackfillCompleted();
            watchlistStockRepository.flush();

            Long completedStockId = completedStock.getId();
            Long incompletedStockId = stocks.stream().filter(s -> "000660".equals(s.getStockCode())).findFirst().get().getId();

            // API는 두 종목 모두 반환 (이름 변경)
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자(변경)", "KRX"),
                    new WatchlistStockResponse.StockItem("J", "000660", "SK하이닉스(변경)", "KRX")
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);

            // when
            watchlistService.syncWatchlist();

            // then: 각각 백필 플래그 보존 확인
            var updatedCompletedStock = watchlistStockRepository.findById(completedStockId).orElseThrow();
            assertThat(updatedCompletedStock.getStockName()).isEqualTo("삼성전자(변경)");
            assertThat(updatedCompletedStock.isBackfillCompleted()).isTrue(); // 완료 상태 보존

            var updatedIncompletedStock = watchlistStockRepository.findById(incompletedStockId).orElseThrow();
            assertThat(updatedIncompletedStock.getStockName()).isEqualTo("SK하이닉스(변경)");
            assertThat(updatedIncompletedStock.isBackfillCompleted()).isFalse(); // 미완료 상태 보존
        }
    }

    @Nested
    @DisplayName("Cascade & orphanRemoval 통합 검증")
    class CascadeAndOrphanRemoval {

        @Test
        @DisplayName("removeStock() 호출 시 orphanRemoval로 DB에서 삭제")
        void removeStock_호출시_DB_삭제() {
            // given: 그룹에 2개 종목 추가
            var group = createAndSaveGroup("001", "테스트그룹");
            createAndAddStock(group, "005930", "삼성전자");
            createAndAddStock(group, "000660", "SK하이닉스");
            watchlistGroupRepository.flush();

            // flush 후 ID 조회
            var stocks = watchlistStockRepository.findAll();
            Long stock1Id = stocks.stream().filter(s -> "005930".equals(s.getStockCode())).findFirst().get().getId();
            Long stock2Id = stocks.stream().filter(s -> "000660".equals(s.getStockCode())).findFirst().get().getId();

            // API는 stock1만 반환 (stock2 제거됨)
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);

            // when
            watchlistService.syncWatchlist();
            entityManager.flush(); // orphanRemoval 트리거
            entityManager.clear(); // 영속성 컨텍스트 초기화

            // then: orphanRemoval로 stock2가 DB에서 삭제됨
            assertThat(watchlistStockRepository.findById(stock1Id)).isPresent();
            assertThat(watchlistStockRepository.findById(stock2Id)).isEmpty(); // orphanRemoval로 삭제
            assertThat(watchlistStockRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("그룹 삭제 시 Cascade로 모든 종목 삭제")
        void 그룹_삭제시_Cascade로_종목_삭제() {
            // given: 그룹에 종목 2개 추가
            var group = createAndSaveGroup("001", "삭제될그룹");
            createAndAddStock(group, "005930", "삼성전자");
            createAndAddStock(group, "000660", "SK하이닉스");
            watchlistGroupRepository.flush();

            // flush 후 ID 조회
            Long groupId = group.getId();
            var stocks = watchlistStockRepository.findAll();
            Long stock1Id = stocks.get(0).getId();
            Long stock2Id = stocks.get(1).getId();

            // API는 빈 그룹 목록 반환 (모든 그룹 삭제)
            given(kisWatchlistService.getWatchlistGroups()).willReturn(List.of());

            // when
            watchlistService.syncWatchlist();

            // then: Cascade로 그룹과 종목 모두 삭제됨
            assertThat(watchlistGroupRepository.findById(groupId)).isEmpty();
            assertThat(watchlistStockRepository.findById(stock1Id)).isEmpty();
            assertThat(watchlistStockRepository.findById(stock2Id)).isEmpty();
            assertThat(watchlistGroupRepository.count()).isZero();
            assertThat(watchlistStockRepository.count()).isZero();
        }

        @Test
        @DisplayName("API에 없는 그룹 삭제 시 해당 그룹의 종목도 Cascade 삭제")
        void API에_없는_그룹_삭제시_종목_Cascade_삭제() {
            // given: 2개 그룹 생성, 각각 종목 1개씩
            var group1 = createAndSaveGroup("001", "유지될그룹");
            createAndAddStock(group1, "005930", "삼성전자");

            var group2 = createAndSaveGroup("002", "삭제될그룹");
            createAndAddStock(group2, "000660", "SK하이닉스");
            watchlistGroupRepository.flush();

            // flush 후 ID 조회
            Long group1Id = group1.getId();
            Long group2Id = group2.getId();
            var stocks = watchlistStockRepository.findAll();
            Long stock1Id = stocks.stream().filter(s -> "005930".equals(s.getStockCode())).findFirst().get().getId();
            Long stock2Id = stocks.stream().filter(s -> "000660".equals(s.getStockCode())).findFirst().get().getId();

            // API는 group1만 반환 (group2 삭제됨)
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "유지될그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);

            // when
            watchlistService.syncWatchlist();

            // then: group2와 stock2 삭제, group1과 stock1 유지
            assertThat(watchlistGroupRepository.findById(group1Id)).isPresent();
            assertThat(watchlistGroupRepository.findById(group2Id)).isEmpty(); // group2 삭제
            assertThat(watchlistStockRepository.findById(stock1Id)).isPresent();
            assertThat(watchlistStockRepository.findById(stock2Id)).isEmpty(); // stock2 Cascade 삭제

            assertThat(watchlistGroupRepository.count()).isEqualTo(1);
            assertThat(watchlistStockRepository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("복합 시나리오 - 추가/업데이트/삭제 동시 발생")
    class ComplexScenario {

        @Test
        @DisplayName("한 그룹에서 추가/업데이트/삭제 동시 발생")
        void 한_그룹_복합_시나리오() {
            // given: 초기 DB 상태 - Stock A (백필 완료), Stock B, Stock C
            var existingGroup = createAndSaveGroup("001", "복합테스트그룹");
            createAndAddStock(existingGroup, "005930", "삼성전자");
            createAndAddStock(existingGroup, "000660", "SK하이닉스");
            createAndAddStock(existingGroup, "035420", "NAVER");
            watchlistGroupRepository.flush();

            // flush 후 백필 완료 표시 및 ID 조회
            var stocks = watchlistStockRepository.findAll();
            var stockA = stocks.stream().filter(s -> "005930".equals(s.getStockCode())).findFirst().get();
            stockA.markBackfillCompleted();
            watchlistStockRepository.flush();

            Long stockAId = stockA.getId();
            Long stockBId = stocks.stream().filter(s -> "000660".equals(s.getStockCode())).findFirst().get().getId();
            Long stockCId = stocks.stream().filter(s -> "035420".equals(s.getStockCode())).findFirst().get().getId();

            // API: Stock A (수정), Stock D (신규)
            // Stock B, Stock C는 API에 없음 → 삭제됨
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "복합테스트그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자(수정)", "KRX"), // 업데이트
                    new WatchlistStockResponse.StockItem("J", "035720", "카카오", "KRX") // 신규 추가
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);

            // when
            watchlistService.syncWatchlist();

            // then: 실제 DB 상태 확인
            var updatedStocks = watchlistStockRepository.findAll();
            assertThat(updatedStocks).hasSize(2); // Stock A (업데이트), Stock D (신규), Stock B/C (삭제)

            // Stock A: 업데이트 + 백필 플래그 보존
            var updatedStockA = watchlistStockRepository.findById(stockAId).orElseThrow();
            assertThat(updatedStockA.getStockName()).isEqualTo("삼성전자(수정)");
            assertThat(updatedStockA.isBackfillCompleted()).isTrue(); // 백필 플래그 보존

            // Stock D: 신규 추가
            var newStocks = updatedStocks.stream()
                    .filter(s -> "035720".equals(s.getStockCode()))
                    .toList();
            assertThat(newStocks).hasSize(1);
            assertThat(newStocks.get(0).getStockName()).isEqualTo("카카오");
            assertThat(newStocks.get(0).isBackfillCompleted()).isFalse(); // 신규는 백필 필요

            // Stock B, C: 삭제됨
            assertThat(watchlistStockRepository.findById(stockBId)).isEmpty();
            assertThat(watchlistStockRepository.findById(stockCId)).isEmpty();
        }

        @Test
        @DisplayName("여러 그룹에서 복합 시나리오")
        void 여러_그룹_복합_시나리오() {
            // given: Group1 (유지 + 종목 수정), Group2 (삭제), Group3 (신규)
            var group1 = createAndSaveGroup("001", "유지그룹");
            createAndAddStock(group1, "005930", "삼성전자");

            var group2 = createAndSaveGroup("002", "삭제될그룹");
            createAndAddStock(group2, "000660", "SK하이닉스");
            watchlistGroupRepository.flush();

            // flush 후 백필 완료 표시 및 ID 조회
            var stocks = watchlistStockRepository.findAll();
            var stock1 = stocks.stream().filter(s -> "005930".equals(s.getStockCode())).findFirst().get();
            stock1.markBackfillCompleted();
            watchlistStockRepository.flush();

            Long group1Id = group1.getId();
            Long group2Id = group2.getId();
            Long stock1Id = stock1.getId();
            Long stock2Id = stocks.stream().filter(s -> "000660".equals(s.getStockCode())).findFirst().get().getId();

            // API: Group1 (종목 수정), Group3 (신규)
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "유지그룹"),
                    new WatchlistGroupResponse.GroupItem("003", "신규그룹") // 신규 그룹
            );
            var apiStocks1 = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자(수정)", "KRX")
            );
            var apiStocks3 = List.of(
                    new WatchlistStockResponse.StockItem("J", "035420", "NAVER", "KRX")
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks1);
            given(kisWatchlistService.getStocksByGroup("003")).willReturn(apiStocks3);

            // when
            watchlistService.syncWatchlist();

            // then
            var groups = watchlistGroupRepository.findAll();
            assertThat(groups).hasSize(2); // Group1 (유지), Group3 (신규), Group2 (삭제)

            // Group1 유지, 종목 업데이트
            assertThat(watchlistGroupRepository.findById(group1Id)).isPresent();
            var updatedStock1 = watchlistStockRepository.findById(stock1Id).orElseThrow();
            assertThat(updatedStock1.getStockName()).isEqualTo("삼성전자(수정)");
            assertThat(updatedStock1.isBackfillCompleted()).isTrue(); // 백필 플래그 보존

            // Group2 삭제, 종목도 Cascade 삭제
            assertThat(watchlistGroupRepository.findById(group2Id)).isEmpty();
            assertThat(watchlistStockRepository.findById(stock2Id)).isEmpty();

            // Group3 신규 추가
            var group3 = groups.stream()
                    .filter(g -> "003".equals(g.getGroupCode()))
                    .findFirst()
                    .orElseThrow();
            assertThat(group3.getGroupName()).isEqualTo("신규그룹");

            var stocks3 = watchlistStockRepository.findAll().stream()
                    .filter(s -> "035420".equals(s.getStockCode()))
                    .toList();
            assertThat(stocks3).hasSize(1);
            assertThat(stocks3.get(0).getStockName()).isEqualTo("NAVER");
            assertThat(stocks3.get(0).isBackfillCompleted()).isFalse(); // 신규는 백필 필요
        }
    }

    @Nested
    @DisplayName("다중 그룹 동기화 및 격리 검증")
    class MultipleGroupsSync {

        @Test
        @DisplayName("여러 그룹의 동기화가 올바르게 수행")
        void 여러_그룹_동기화() {
            // given: 3개 그룹, 각각 종목 1개씩
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "그룹1"),
                    new WatchlistGroupResponse.GroupItem("002", "그룹2"),
                    new WatchlistGroupResponse.GroupItem("003", "그룹3")
            );
            var apiStocks1 = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );
            var apiStocks2 = List.of(
                    new WatchlistStockResponse.StockItem("J", "000660", "SK하이닉스", "KRX")
            );
            var apiStocks3 = List.of(
                    new WatchlistStockResponse.StockItem("J", "035420", "NAVER", "KRX")
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks1);
            given(kisWatchlistService.getStocksByGroup("002")).willReturn(apiStocks2);
            given(kisWatchlistService.getStocksByGroup("003")).willReturn(apiStocks3);

            // when
            watchlistService.syncWatchlist();

            // then: 3개 그룹, 각각 1개 종목씩 저장됨
            var groups = watchlistGroupRepository.findByUserId(TEST_USER_ID);
            assertThat(groups).hasSize(3);
            assertThat(groups)
                    .extracting(WatchlistGroup::getGroupCode)
                    .containsExactlyInAnyOrder("001", "002", "003");

            var stocks = watchlistStockRepository.findAll();
            assertThat(stocks).hasSize(3);
            assertThat(stocks)
                    .extracting(WatchlistStock::getStockCode)
                    .containsExactlyInAnyOrder("005930", "000660", "035420");
        }

        @Test
        @DisplayName("그룹 간 종목 격리 확인 - 같은 종목 코드가 다른 그룹에 존재 가능")
        void 그룹_간_종목_격리() {
            // given: 2개 그룹에 동일한 종목 코드 추가
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "그룹1"),
                    new WatchlistGroupResponse.GroupItem("002", "그룹2")
            );
            var apiStocks1 = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );
            var apiStocks2 = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX") // 동일 종목
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks1);
            given(kisWatchlistService.getStocksByGroup("002")).willReturn(apiStocks2);

            // when
            watchlistService.syncWatchlist();

            // then: 같은 종목이 두 그룹에 각각 존재
            var stocks = watchlistStockRepository.findAll();
            assertThat(stocks).hasSize(2); // 2개 그룹에 각각 1개씩
            assertThat(stocks).allMatch(s -> "005930".equals(s.getStockCode()));

            // 각 그룹에 1개씩 종목이 있어야 함
            var groups = watchlistGroupRepository.findByUserId(TEST_USER_ID);
            assertThat(groups).hasSize(2);
            assertThat(groups).allMatch(g -> g.getStocks().size() == 1);
        }

        @Test
        @DisplayName("한 그룹만 변경되어도 다른 그룹은 영향받지 않음")
        void 한_그룹_변경시_다른_그룹_영향_없음() {
            // given: 2개 그룹 생성
            var group1 = createAndSaveGroup("001", "변경될그룹");
            createAndAddStock(group1, "005930", "삼성전자");

            var group2 = createAndSaveGroup("002", "유지될그룹");
            createAndAddStock(group2, "000660", "SK하이닉스");
            watchlistGroupRepository.flush();

            // flush 후 ID 조회
            Long group2Id = group2.getId();
            var stocks = watchlistStockRepository.findAll();
            Long stock2Id = stocks.stream().filter(s -> "000660".equals(s.getStockCode())).findFirst().get().getId();

            // API는 group1의 종목만 변경
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "변경될그룹"),
                    new WatchlistGroupResponse.GroupItem("002", "유지될그룹")
            );
            var apiStocks1 = List.of(
                    new WatchlistStockResponse.StockItem("J", "035420", "NAVER", "KRX") // 종목 변경
            );
            var apiStocks2 = List.of(
                    new WatchlistStockResponse.StockItem("J", "000660", "SK하이닉스", "KRX") // 유지
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks1);
            given(kisWatchlistService.getStocksByGroup("002")).willReturn(apiStocks2);

            // when
            watchlistService.syncWatchlist();

            // then: group2와 stock2는 변경되지 않음
            var unchangedGroup2 = watchlistGroupRepository.findById(group2Id).orElseThrow();
            assertThat(unchangedGroup2.getGroupName()).isEqualTo("유지될그룹");

            var unchangedStock2 = watchlistStockRepository.findById(stock2Id).orElseThrow();
            assertThat(unchangedStock2.getStockCode()).isEqualTo("000660");
            assertThat(unchangedStock2.getStockName()).isEqualTo("SK하이닉스");
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("빈 종목 목록 처리 - 그룹은 유지되고 종목만 삭제")
        void 빈_종목_목록_처리() {
            // given: 기존 그룹에 종목 2개
            var existingGroup = createAndSaveGroup("001", "빈그룹");
            createAndAddStock(existingGroup, "005930", "삼성전자");
            createAndAddStock(existingGroup, "000660", "SK하이닉스");
            watchlistGroupRepository.flush();

            // flush 후 ID 조회
            Long groupId = existingGroup.getId();
            var stocks = watchlistStockRepository.findAll();
            Long stock1Id = stocks.get(0).getId();
            Long stock2Id = stocks.get(1).getId();

            // API는 빈 종목 목록 반환
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "빈그룹")
            );
            var apiStocks = List.<WatchlistStockResponse.StockItem>of(); // 빈 리스트

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);

            // when
            watchlistService.syncWatchlist();
            entityManager.flush(); // orphanRemoval 트리거
            entityManager.clear(); // 영속성 컨텍스트 초기화

            // then: 그룹은 유지, 종목은 모두 삭제
            assertThat(watchlistGroupRepository.findById(groupId)).isPresent();
            assertThat(watchlistStockRepository.findById(stock1Id)).isEmpty();
            assertThat(watchlistStockRepository.findById(stock2Id)).isEmpty();
            assertThat(watchlistStockRepository.count()).isZero();

            var group = watchlistGroupRepository.findById(groupId).orElseThrow();
            assertThat(group.getStocks()).isEmpty();
        }

        @Test
        @DisplayName("API 그룹 코드 변경 시 기존 그룹 삭제 후 신규 생성")
        void API_그룹_코드_변경() {
            // given: 기존 그룹 "001"
            var existingGroup = createAndSaveGroup("001", "기존그룹");
            createAndAddStock(existingGroup, "005930", "삼성전자");
            watchlistGroupRepository.flush();

            // flush 후 ID 조회
            Long oldGroupId = existingGroup.getId();
            Long oldStockId = watchlistStockRepository.findAll().get(0).getId();

            // API는 그룹 코드를 "002"로 변경
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("002", "신규그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("002")).willReturn(apiStocks);

            // when
            watchlistService.syncWatchlist();

            // then: 기존 그룹("001") 삭제, 신규 그룹("002") 생성
            assertThat(watchlistGroupRepository.findById(oldGroupId)).isEmpty(); // 기존 그룹 삭제
            assertThat(watchlistStockRepository.findById(oldStockId)).isEmpty(); // 기존 종목 Cascade 삭제

            var groups = watchlistGroupRepository.findByUserId(TEST_USER_ID);
            assertThat(groups).hasSize(1);
            assertThat(groups.get(0).getGroupCode()).isEqualTo("002");
            assertThat(groups.get(0).getGroupName()).isEqualTo("신규그룹");

            var stocks = watchlistStockRepository.findAll();
            assertThat(stocks).hasSize(1);
            assertThat(stocks.get(0).getStockCode()).isEqualTo("005930");
            assertThat(stocks.get(0).isBackfillCompleted()).isFalse(); // 신규 종목이므로 백필 필요
        }

        @Test
        @DisplayName("API에서 중복 종목 반환 시 나중 값 우선 (방어적 처리)")
        void API_중복_종목_필터링() {
            // given: API가 중복 종목 반환
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자(첫번째)", "KRX"),
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자(두번째)", "KRX"), // 중복
                    new WatchlistStockResponse.StockItem("J", "000660", "SK하이닉스", "KRX")
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);

            // when
            watchlistService.syncWatchlist();

            // then: 중복 제거되어 2개만 저장, 나중 값 우선
            var stocks = watchlistStockRepository.findAll();
            assertThat(stocks).hasSize(2);

            var samsung = stocks.stream()
                    .filter(s -> "005930".equals(s.getStockCode()))
                    .findFirst()
                    .orElseThrow();
            assertThat(samsung.getStockName()).isEqualTo("삼성전자(두번째)"); // 나중 값 우선
        }

        @Test
        @DisplayName("null/blank stockCode 필터링 (방어적 처리)")
        void null_blank_stockCode_필터링() {
            // given: API가 null/blank stockCode 반환
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", null, "Null종목", "KRX"), // 필터링됨
                    new WatchlistStockResponse.StockItem("J", "", "빈문자열종목", "KRX"), // 필터링됨
                    new WatchlistStockResponse.StockItem("J", "   ", "공백종목", "KRX"), // 필터링됨
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX") // 정상
            );

            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);

            // when
            watchlistService.syncWatchlist();

            // then: 정상 종목만 저장
            var stocks = watchlistStockRepository.findAll();
            assertThat(stocks).hasSize(1);
            assertThat(stocks.get(0).getStockCode()).isEqualTo("005930");
            assertThat(stocks.get(0).getStockName()).isEqualTo("삼성전자");
        }

        @Test
        @DisplayName("API가 빈 그룹 목록 반환 시 모든 그룹 삭제")
        void API_빈_그룹_목록_반환시_모든_그룹_삭제() {
            // given: 기존 그룹 2개
            var group1 = createAndSaveGroup("001", "그룹1");
            createAndAddStock(group1, "005930", "삼성전자");

            var group2 = createAndSaveGroup("002", "그룹2");
            createAndAddStock(group2, "000660", "SK하이닉스");
            watchlistGroupRepository.flush();

            // API는 빈 그룹 목록 반환
            given(kisWatchlistService.getWatchlistGroups()).willReturn(List.of());

            // when
            watchlistService.syncWatchlist();

            // then: 모든 그룹과 종목 삭제
            assertThat(watchlistGroupRepository.count()).isZero();
            assertThat(watchlistStockRepository.count()).isZero();
        }
    }

    // === 헬퍼 메서드 ===

    /**
     * 그룹 생성 및 저장.
     */
    private WatchlistGroup createAndSaveGroup(String groupCode, String groupName) {
        WatchlistGroup group = WatchlistGroup.builder()
                .userId(TEST_USER_ID)
                .groupCode(groupCode)
                .groupName(groupName)
                .type("1")
                .build();
        return watchlistGroupRepository.save(group);
    }

    /**
     * 그룹에 종목 추가 및 저장.
     */
    private WatchlistStock createAndAddStock(WatchlistGroup group, String stockCode, String stockName) {
        WatchlistStock stock = WatchlistStock.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .marketCode(MarketCode.KRX)
                .assetType(AssetType.DOMESTIC_STOCK)
                .build();
        group.addStock(stock);
        watchlistGroupRepository.save(group);
        return stock;
    }
}
