package com.custom.trader.watchlist.service;

import com.custom.trader.kis.config.KisProperties;
import com.custom.trader.kis.dto.watchlist.WatchlistGroupResponse;
import com.custom.trader.kis.dto.watchlist.WatchlistStockResponse;
import com.custom.trader.kis.exception.KisApiException;
import com.custom.trader.kis.service.KisWatchlistService;
import com.custom.trader.watchlist.entity.WatchlistGroup;
import com.custom.trader.watchlist.repository.WatchlistGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private KisWatchlistService kisWatchlistService;

    @Mock
    private WatchlistGroupRepository watchlistGroupRepository;

    @Mock
    private KisProperties kisProperties;

    @Captor
    private ArgumentCaptor<List<WatchlistGroup>> groupListCaptor;

    private WatchlistService watchlistService;

    private static final String TEST_USER_ID = "testUser";

    @BeforeEach
    void setUp() {
        watchlistService = new WatchlistService(kisWatchlistService, watchlistGroupRepository, kisProperties);
    }

    @Nested
    @DisplayName("syncWatchlist 메소드")
    class SyncWatchlist {

        @Test
        @DisplayName("관심종목 동기화 정상 처리")
        void 관심종목_동기화_정상_처리() {
            // given
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "관심그룹1"),
                    new WatchlistGroupResponse.GroupItem("002", "관심그룹2")
            );
            var stockItems1 = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX"),
                    new WatchlistStockResponse.StockItem("J", "000660", "SK하이닉스", "KRX")
            );
            var stockItems2 = List.of(
                    new WatchlistStockResponse.StockItem("J", "035420", "NAVER", "KRX")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(Collections.emptyList());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems1);
            given(kisWatchlistService.getStocksByGroup("002")).willReturn(stockItems2);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(kisWatchlistService).getWatchlistGroups();
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());

            var savedGroups = groupListCaptor.getValue();
            assertThat(savedGroups).hasSize(2);

            var group1 = savedGroups.get(0);
            assertThat(group1.getGroupCode()).isEqualTo("001");
            assertThat(group1.getGroupName()).isEqualTo("관심그룹1");
            assertThat(group1.getStocks()).hasSize(2);

            var group2 = savedGroups.get(1);
            assertThat(group2.getGroupCode()).isEqualTo("002");
            assertThat(group2.getGroupName()).isEqualTo("관심그룹2");
            assertThat(group2.getStocks()).hasSize(1);
        }

        @Test
        @DisplayName("빈 그룹 목록이면 모든 그룹 삭제 후 종료")
        void 빈_그룹목록이면_모든_그룹_삭제() {
            // given
            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(Collections.emptyList());

            // when
            watchlistService.syncWatchlist();

            // then
            verify(kisWatchlistService).getWatchlistGroups();
            verify(watchlistGroupRepository).deleteByUserId(TEST_USER_ID); // 모든 그룹 삭제
            verify(watchlistGroupRepository, never()).deleteByUserIdAndGroupCodeNotIn(any(), any());
            verify(watchlistGroupRepository, never()).saveAll(any()); // saveAll 호출 안 됨
            verify(kisWatchlistService, never()).getStocksByGroup(any());
        }
    }

    @Nested
    @DisplayName("syncGroup 메소드 (private, syncWatchlist 통해 테스트)")
    class SyncGroup {

        @Test
        @DisplayName("신규 그룹 생성")
        void 신규_그룹_생성() {
            // given
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("003", "신규그룹")
            );
            var stockItems = List.of(
                    new WatchlistStockResponse.StockItem("J", "005380", "현대차", "KRX")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(Collections.emptyList());
            given(kisWatchlistService.getStocksByGroup("003")).willReturn(stockItems);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());

            var savedGroup = groupListCaptor.getValue().get(0);
            assertThat(savedGroup.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(savedGroup.getGroupCode()).isEqualTo("003");
            assertThat(savedGroup.getGroupName()).isEqualTo("신규그룹");
            assertThat(savedGroup.getType()).isEqualTo("1");
        }

        @Test
        @DisplayName("기존 그룹 업데이트")
        void 기존_그룹_업데이트() {
            // given
            var existingGroup = WatchlistGroup.builder()
                    .userId(TEST_USER_ID)
                    .groupCode("001")
                    .groupName("기존그룹명")
                    .type("1")
                    .build();

            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "변경된그룹명")
            );
            var stockItems = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(List.of(existingGroup));
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());

            var savedGroup = groupListCaptor.getValue().get(0);
            assertThat(savedGroup.getGroupName()).isEqualTo("변경된그룹명");
        }

        @Test
        @DisplayName("종목 추가 및 기존 종목 제거")
        void 종목_추가_및_기존_종목_제거() {
            // given
            var existingGroup = WatchlistGroup.builder()
                    .userId(TEST_USER_ID)
                    .groupCode("001")
                    .groupName("테스트그룹")
                    .type("1")
                    .build();

            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var newStockItems = List.of(
                    new WatchlistStockResponse.StockItem("J", "035720", "카카오", "KRX"),
                    new WatchlistStockResponse.StockItem("J", "035420", "NAVER", "KRX")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(List.of(existingGroup));
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(newStockItems);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());

            var savedGroup = groupListCaptor.getValue().get(0);
            assertThat(savedGroup.getStocks()).hasSize(2);
            assertThat(savedGroup.getStocks())
                    .extracting("stockCode")
                    .containsExactlyInAnyOrder("035720", "035420");
            assertThat(savedGroup.getStocks())
                    .extracting("stockName")
                    .containsExactlyInAnyOrder("카카오", "NAVER");
        }
    }

    @Nested
    @DisplayName("getWatchlistGroups 메소드")
    class GetWatchlistGroups {

        @Test
        @DisplayName("사용자별 그룹 목록 조회")
        void 사용자별_그룹_목록_조회() {
            // given
            var group1 = WatchlistGroup.builder()
                    .userId(TEST_USER_ID)
                    .groupCode("001")
                    .groupName("관심그룹1")
                    .type("1")
                    .build();
            var group2 = WatchlistGroup.builder()
                    .userId(TEST_USER_ID)
                    .groupCode("002")
                    .groupName("관심그룹2")
                    .type("1")
                    .build();

            given(watchlistGroupRepository.findByUserId(TEST_USER_ID))
                    .willReturn(List.of(group1, group2));

            // when
            var result = watchlistService.getWatchlistGroups(TEST_USER_ID);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getGroupCode()).isEqualTo("001");
            assertThat(result.get(1).getGroupCode()).isEqualTo("002");
            verify(watchlistGroupRepository).findByUserId(TEST_USER_ID);
        }

        @Test
        @DisplayName("데이터 없으면 빈 리스트 반환")
        void 데이터_없으면_빈_리스트_반환() {
            // given
            given(watchlistGroupRepository.findByUserId(TEST_USER_ID))
                    .willReturn(Collections.emptyList());

            // when
            var result = watchlistService.getWatchlistGroups(TEST_USER_ID);

            // then
            assertThat(result).isEmpty();
            verify(watchlistGroupRepository).findByUserId(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("보안 테스트 - 사용자 데이터 격리")
    class SecurityMultiTenancy {

        private static final String OTHER_USER_ID = "otherUser";

        @Test
        @DisplayName("다른 사용자 그룹 접근 불가")
        void 다른_사용자_그룹_접근_불가() {
            // given
            var userAGroup = WatchlistGroup.builder()
                    .userId(TEST_USER_ID)
                    .groupCode("001")
                    .groupName("UserA 그룹")
                    .type("1")
                    .build();

            given(watchlistGroupRepository.findByUserId(TEST_USER_ID))
                    .willReturn(List.of(userAGroup));
            given(watchlistGroupRepository.findByUserId(OTHER_USER_ID))
                    .willReturn(Collections.emptyList());

            // when
            var userAResult = watchlistService.getWatchlistGroups(TEST_USER_ID);
            var userBResult = watchlistService.getWatchlistGroups(OTHER_USER_ID);

            // then
            assertThat(userAResult).hasSize(1);
            assertThat(userAResult.get(0).getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(userBResult).isEmpty();
            verify(watchlistGroupRepository).findByUserId(TEST_USER_ID);
            verify(watchlistGroupRepository).findByUserId(OTHER_USER_ID);
        }

        @Test
        @DisplayName("syncWatchlist는 설정된 userId만 사용")
        void syncWatchlist_올바른_userId_사용() {
            // given
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var stockItems = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(Collections.emptyList());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());
            var savedGroup = groupListCaptor.getValue().get(0);
            assertThat(savedGroup.getUserId()).isEqualTo(TEST_USER_ID);
            verify(watchlistGroupRepository, never()).findByUserIdAndGroupCodeIn(eq(OTHER_USER_ID), any());
        }
    }

    @Nested
    @DisplayName("보안 테스트 - 입력값 검증")
    class SecurityInputValidation {

        @Test
        @DisplayName("userId SQL 인젝션 방지")
        void userId_SQL_인젝션_방지() {
            // given
            String maliciousUserId = "' OR '1'='1";
            given(watchlistGroupRepository.findByUserId(maliciousUserId))
                    .willReturn(Collections.emptyList());

            // when
            var result = watchlistService.getWatchlistGroups(maliciousUserId);

            // then
            assertThat(result).isEmpty();
            verify(watchlistGroupRepository).findByUserId(maliciousUserId);
        }

        @Test
        @DisplayName("groupCode SQL 인젝션 방지")
        void groupCode_SQL_인젝션_방지() {
            // given
            String maliciousGroupCode = "'; DROP TABLE watchlist_group; --";
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem(maliciousGroupCode, "악성그룹")
            );
            var stockItems = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(Collections.emptyList());
            given(kisWatchlistService.getStocksByGroup(maliciousGroupCode)).willReturn(stockItems);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());
            var savedGroup = groupListCaptor.getValue().get(0);
            assertThat(savedGroup.getGroupCode()).isEqualTo(maliciousGroupCode);
        }

        @Test
        @DisplayName("stockCode 특수문자 안전 처리")
        void stockCode_특수문자_안전_처리() {
            // given
            String specialStockCode = "<script>alert('xss')</script>";
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var stockItems = List.of(
                    new WatchlistStockResponse.StockItem("J", specialStockCode, "테스트종목", "KRX")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(Collections.emptyList());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());
            var savedGroup = groupListCaptor.getValue().get(0);
            assertThat(savedGroup.getStocks()).hasSize(1);
            assertThat(savedGroup.getStocks().get(0).getStockCode()).isEqualTo(specialStockCode);
        }
    }

    @Nested
    @DisplayName("방어적 프로그래밍 - API 데이터 품질")
    class DefensiveProgramming {

        @Test
        @DisplayName("중복 stockCode 처리 - 나중 값 우선")
        void 중복_stockCode_처리() {
            // given
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var stockItems = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX"),
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자(중복)", "KRX"), // 중복
                    new WatchlistStockResponse.StockItem("J", "000660", "SK하이닉스", "KRX")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(Collections.emptyList());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());
            var savedGroup = groupListCaptor.getValue().get(0);

            // 중복 제거되어 2개만 저장됨
            assertThat(savedGroup.getStocks()).hasSize(2);

            // 나중 값(중복)이 우선됨
            var duplicateStock = savedGroup.getStocks().stream()
                    .filter(s -> "005930".equals(s.getStockCode()))
                    .findFirst()
                    .orElseThrow();
            assertThat(duplicateStock.getStockName()).isEqualTo("삼성전자(중복)");
        }

        @Test
        @DisplayName("null stockCode 필터링")
        void null_stockCode_필터링() {
            // given
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var stockItems = List.of(
                    new WatchlistStockResponse.StockItem("J", null, "Null종목", "KRX"), // 필터링됨
                    new WatchlistStockResponse.StockItem("J", "", "빈문자열종목", "KRX"), // 필터링됨
                    new WatchlistStockResponse.StockItem("J", "   ", "공백종목", "KRX"), // 필터링됨
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX") // 정상
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(Collections.emptyList());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());
            var savedGroup = groupListCaptor.getValue().get(0);

            // null/blank stockCode는 필터링되고 정상 종목만 저장됨
            assertThat(savedGroup.getStocks()).hasSize(1);
            assertThat(savedGroup.getStocks().get(0).getStockCode()).isEqualTo("005930");
            assertThat(savedGroup.getStocks().get(0).getStockName()).isEqualTo("삼성전자");
        }

        @Test
        @DisplayName("빈 종목 목록 처리")
        void 빈_종목_목록_처리() {
            // given
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "빈그룹")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(Collections.emptyList());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(Collections.emptyList());
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());
            var savedGroup = groupListCaptor.getValue().get(0);

            assertThat(savedGroup.getGroupCode()).isEqualTo("001");
            assertThat(savedGroup.getStocks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("보안 테스트 - API 응답 검증")
    class SecurityApiResponseValidation {

        @Test
        @DisplayName("API null groupCode 처리")
        void API_null_groupCode_처리() {
            // given
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem(null, "Null코드그룹")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(Collections.emptyList());
            given(kisWatchlistService.getStocksByGroup(null)).willReturn(Collections.emptyList());
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());
            var savedGroup = groupListCaptor.getValue().get(0);
            assertThat(savedGroup.getGroupCode()).isNull();
        }

        @Test
        @DisplayName("API 빈 문자열 groupName 처리")
        void API_빈_문자열_groupName_처리() {
            // given
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "")
            );
            var stockItems = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(Collections.emptyList());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());
            var savedGroup = groupListCaptor.getValue().get(0);
            assertThat(savedGroup.getGroupName()).isEmpty();
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandling {

        @Test
        @DisplayName("API 예외시 KisApiException 전파")
        void API_예외시_KisApiException_전파() {
            // given
            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            willThrow(new KisApiException("API 호출 실패"))
                    .given(kisWatchlistService).getWatchlistGroups();

            // when & then
            assertThatThrownBy(() -> watchlistService.syncWatchlist())
                    .isInstanceOf(KisApiException.class)
                    .hasMessageContaining("API 호출 실패");
        }

        @Test
        @DisplayName("종목조회 예외시 예외 전파")
        void 종목조회_예외시_예외_전파() {
            // given
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "그룹1"),
                    new WatchlistGroupResponse.GroupItem("002", "그룹2")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(Collections.emptyList());
            willThrow(new KisApiException("종목 조회 실패"))
                    .given(kisWatchlistService).getStocksByGroup("001");

            // when & then
            assertThatThrownBy(() -> watchlistService.syncWatchlist())
                    .isInstanceOf(KisApiException.class)
                    .hasMessageContaining("종목 조회 실패");
        }

        @Test
        @DisplayName("저장 실패시 예외 전파")
        void 저장_실패시_예외_전파() {
            // given
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var stockItems = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), any()))
                    .willReturn(Collections.emptyList());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems);
            willThrow(new DataIntegrityViolationException("무결성 제약조건 위반"))
                    .given(watchlistGroupRepository).saveAll(any());

            // when & then
            assertThatThrownBy(() -> watchlistService.syncWatchlist())
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("무결성 제약조건 위반");
        }
    }

    @Nested
    @DisplayName("관심종목 편집 반영 기능 테스트")
    class WatchlistEditReflection {

        @Test
        @DisplayName("1. 그룹 삭제 감지 - API에 없는 그룹이 DB에서 삭제되는지 확인")
        void API에_없는_그룹_삭제() {
            // given
            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "유지되는 그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), eq(List.of("001"))))
                    .willReturn(Collections.emptyList());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).deleteByUserIdAndGroupCodeNotIn(TEST_USER_ID, List.of("001"));
        }

        @Test
        @DisplayName("2. 종목 업데이트 시 백필 플래그 보존")
        void 종목_업데이트_시_백필_플래그_보존() {
            // given
            var existingGroup = WatchlistGroup.builder()
                    .userId(TEST_USER_ID)
                    .groupCode("001")
                    .groupName("테스트그룹")
                    .type("1")
                    .build();

            // 백필 완료된 기존 종목 추가
            var existingStock = com.custom.trader.watchlist.entity.WatchlistStock.builder()
                    .stockCode("005930")
                    .stockName("삼성전자")
                    .marketCode(com.custom.trader.common.enums.MarketCode.KRX)
                    .assetType(com.custom.trader.common.enums.AssetType.DOMESTIC_STOCK)
                    .build();
            existingStock.markBackfillCompleted(); // 백필 완료 상태로 설정
            existingGroup.addStock(existingStock);

            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자(변경됨)", "KRX") // 종목명 변경
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), eq(List.of("001"))))
                    .willReturn(List.of(existingGroup));
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());
            var savedGroup = groupListCaptor.getValue().get(0);
            var updatedStock = savedGroup.getStocks().stream()
                    .filter(s -> "005930".equals(s.getStockCode()))
                    .findFirst()
                    .orElseThrow();

            assertThat(updatedStock.getStockName()).isEqualTo("삼성전자(변경됨)"); // 이름은 변경됨
            assertThat(updatedStock.isBackfillCompleted()).isTrue(); // 백필 플래그는 보존됨
        }

        @Test
        @DisplayName("3. 신규 종목 추가 시 백필 플래그 false 확인")
        void 신규_종목_추가_시_백필_플래그_false() {
            // given
            var existingGroup = WatchlistGroup.builder()
                    .userId(TEST_USER_ID)
                    .groupCode("001")
                    .groupName("테스트그룹")
                    .type("1")
                    .build();

            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "000660", "SK하이닉스", "KRX") // 신규 종목
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), eq(List.of("001"))))
                    .willReturn(List.of(existingGroup));
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());
            var savedGroup = groupListCaptor.getValue().get(0);
            var newStock = savedGroup.getStocks().stream()
                    .filter(s -> "000660".equals(s.getStockCode()))
                    .findFirst()
                    .orElseThrow();

            assertThat(newStock.getStockCode()).isEqualTo("000660");
            assertThat(newStock.getStockName()).isEqualTo("SK하이닉스");
            assertThat(newStock.isBackfillCompleted()).isFalse(); // 신규 종목은 백필 필요
        }

        @Test
        @DisplayName("4. 종목 삭제 감지 - API에 없는 종목이 DB에서 삭제되는지 확인")
        void API에_없는_종목_삭제() {
            // given
            var existingGroup = WatchlistGroup.builder()
                    .userId(TEST_USER_ID)
                    .groupCode("001")
                    .groupName("테스트그룹")
                    .type("1")
                    .build();

            // 기존 종목 2개 추가
            var stock1 = com.custom.trader.watchlist.entity.WatchlistStock.builder()
                    .stockCode("005930")
                    .stockName("삼성전자")
                    .marketCode(com.custom.trader.common.enums.MarketCode.KRX)
                    .assetType(com.custom.trader.common.enums.AssetType.DOMESTIC_STOCK)
                    .build();
            var stock2 = com.custom.trader.watchlist.entity.WatchlistStock.builder()
                    .stockCode("000660")
                    .stockName("SK하이닉스")
                    .marketCode(com.custom.trader.common.enums.MarketCode.KRX)
                    .assetType(com.custom.trader.common.enums.AssetType.DOMESTIC_STOCK)
                    .build();
            existingGroup.addStock(stock1);
            existingGroup.addStock(stock2);

            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX") // stock2는 API에 없음
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), eq(List.of("001"))))
                    .willReturn(List.of(existingGroup));
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());
            var savedGroup = groupListCaptor.getValue().get(0);

            assertThat(savedGroup.getStocks()).hasSize(1); // SK하이닉스 삭제됨
            assertThat(savedGroup.getStocks().get(0).getStockCode()).isEqualTo("005930");
        }

        @Test
        @DisplayName("5. 복합 시나리오 - 추가/업데이트/삭제가 동시에 발생")
        void 복합_시나리오_추가_업데이트_삭제() {
            // given
            var existingGroup = WatchlistGroup.builder()
                    .userId(TEST_USER_ID)
                    .groupCode("001")
                    .groupName("테스트그룹")
                    .type("1")
                    .build();

            // 기존 종목 2개 (1개는 백필 완료, 1개는 미완료)
            var existingStock1 = com.custom.trader.watchlist.entity.WatchlistStock.builder()
                    .stockCode("005930")
                    .stockName("삼성전자")
                    .marketCode(com.custom.trader.common.enums.MarketCode.KRX)
                    .assetType(com.custom.trader.common.enums.AssetType.DOMESTIC_STOCK)
                    .build();
            existingStock1.markBackfillCompleted();

            var existingStock2 = com.custom.trader.watchlist.entity.WatchlistStock.builder()
                    .stockCode("000660")
                    .stockName("SK하이닉스")
                    .marketCode(com.custom.trader.common.enums.MarketCode.KRX)
                    .assetType(com.custom.trader.common.enums.AssetType.DOMESTIC_STOCK)
                    .build();

            existingGroup.addStock(existingStock1);
            existingGroup.addStock(existingStock2);

            var apiGroups = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자(수정됨)", "KRX"), // 업데이트
                    new WatchlistStockResponse.StockItem("J", "035420", "NAVER", "KRX") // 신규 추가
                    // 000660(SK하이닉스)는 API에 없음 → 삭제
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(apiGroups);
            given(watchlistGroupRepository.findByUserIdAndGroupCodeIn(eq(TEST_USER_ID), eq(List.of("001"))))
                    .willReturn(List.of(existingGroup));
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(apiStocks);
            given(watchlistGroupRepository.saveAll(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).saveAll(groupListCaptor.capture());
            var savedGroup = groupListCaptor.getValue().get(0);

            assertThat(savedGroup.getStocks()).hasSize(2); // 총 2개 (1개 업데이트, 1개 추가, 1개 삭제)

            // 업데이트된 종목: 백필 플래그 보존 확인
            var updatedStock = savedGroup.getStocks().stream()
                    .filter(s -> "005930".equals(s.getStockCode()))
                    .findFirst()
                    .orElseThrow();
            assertThat(updatedStock.getStockName()).isEqualTo("삼성전자(수정됨)");
            assertThat(updatedStock.isBackfillCompleted()).isTrue(); // 백필 플래그 보존

            // 신규 추가된 종목
            var newStock = savedGroup.getStocks().stream()
                    .filter(s -> "035420".equals(s.getStockCode()))
                    .findFirst()
                    .orElseThrow();
            assertThat(newStock.getStockName()).isEqualTo("NAVER");
            assertThat(newStock.isBackfillCompleted()).isFalse(); // 신규는 백필 필요

            // 삭제된 종목 확인
            var deletedStock = savedGroup.getStocks().stream()
                    .filter(s -> "000660".equals(s.getStockCode()))
                    .findFirst();
            assertThat(deletedStock).isEmpty(); // SK하이닉스 삭제됨
        }

        @Test
        @DisplayName("API가 빈 그룹 목록 반환 시 모든 그룹 삭제")
        void API_빈_그룹_목록_반환시_모든_그룹_삭제() {
            // given
            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(Collections.emptyList());

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).deleteByUserId(TEST_USER_ID);
            verify(watchlistGroupRepository, never()).deleteByUserIdAndGroupCodeNotIn(any(), any());
            verify(watchlistGroupRepository, never()).saveAll(any());
        }
    }
}
