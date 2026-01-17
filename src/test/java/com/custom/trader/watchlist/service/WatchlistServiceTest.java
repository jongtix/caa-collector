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
import java.util.Optional;

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
    private ArgumentCaptor<WatchlistGroup> groupCaptor;

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
                    new WatchlistStockResponse.StockItem("005930", "삼성전자", "STK"),
                    new WatchlistStockResponse.StockItem("000660", "SK하이닉스", "STK")
            );
            var stockItems2 = List.of(
                    new WatchlistStockResponse.StockItem("035420", "NAVER", "STK")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCode(TEST_USER_ID, "001"))
                    .willReturn(Optional.empty());
            given(watchlistGroupRepository.findByUserIdAndGroupCode(TEST_USER_ID, "002"))
                    .willReturn(Optional.empty());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems1);
            given(kisWatchlistService.getStocksByGroup("002")).willReturn(stockItems2);
            given(watchlistGroupRepository.save(any(WatchlistGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(kisWatchlistService).getWatchlistGroups();
            verify(watchlistGroupRepository, times(2)).save(groupCaptor.capture());

            var savedGroups = groupCaptor.getAllValues();
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
        @DisplayName("빈 그룹 목록이면 동기화 스킵")
        void 빈_그룹목록이면_동기화_스킵() {
            // given
            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(Collections.emptyList());

            // when
            watchlistService.syncWatchlist();

            // then
            verify(kisWatchlistService).getWatchlistGroups();
            verify(watchlistGroupRepository, never()).save(any());
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
                    new WatchlistStockResponse.StockItem("005380", "현대차", "STK")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCode(TEST_USER_ID, "003"))
                    .willReturn(Optional.empty());
            given(kisWatchlistService.getStocksByGroup("003")).willReturn(stockItems);
            given(watchlistGroupRepository.save(any(WatchlistGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).save(groupCaptor.capture());

            var savedGroup = groupCaptor.getValue();
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
                    new WatchlistStockResponse.StockItem("005930", "삼성전자", "STK")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCode(TEST_USER_ID, "001"))
                    .willReturn(Optional.of(existingGroup));
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems);
            given(watchlistGroupRepository.save(any(WatchlistGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).save(groupCaptor.capture());

            var savedGroup = groupCaptor.getValue();
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
                    new WatchlistStockResponse.StockItem("035720", "카카오", "STK"),
                    new WatchlistStockResponse.StockItem("035420", "NAVER", "STK")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCode(TEST_USER_ID, "001"))
                    .willReturn(Optional.of(existingGroup));
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(newStockItems);
            given(watchlistGroupRepository.save(any(WatchlistGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).save(groupCaptor.capture());

            var savedGroup = groupCaptor.getValue();
            assertThat(savedGroup.getStocks()).hasSize(2);
            assertThat(savedGroup.getStocks())
                    .extracting("stockCode")
                    .containsExactly("035720", "035420");
            assertThat(savedGroup.getStocks())
                    .extracting("stockName")
                    .containsExactly("카카오", "NAVER");
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
                    new WatchlistStockResponse.StockItem("005930", "삼성전자", "STK")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCode(TEST_USER_ID, "001"))
                    .willReturn(Optional.empty());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems);
            given(watchlistGroupRepository.save(any(WatchlistGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).save(groupCaptor.capture());
            var savedGroup = groupCaptor.getValue();
            assertThat(savedGroup.getUserId()).isEqualTo(TEST_USER_ID);
            verify(watchlistGroupRepository, never()).findByUserIdAndGroupCode(eq(OTHER_USER_ID), any());
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
                    new WatchlistStockResponse.StockItem("005930", "삼성전자", "STK")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCode(TEST_USER_ID, maliciousGroupCode))
                    .willReturn(Optional.empty());
            given(kisWatchlistService.getStocksByGroup(maliciousGroupCode)).willReturn(stockItems);
            given(watchlistGroupRepository.save(any(WatchlistGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).save(groupCaptor.capture());
            var savedGroup = groupCaptor.getValue();
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
                    new WatchlistStockResponse.StockItem(specialStockCode, "테스트종목", "STK")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCode(TEST_USER_ID, "001"))
                    .willReturn(Optional.empty());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems);
            given(watchlistGroupRepository.save(any(WatchlistGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).save(groupCaptor.capture());
            var savedGroup = groupCaptor.getValue();
            assertThat(savedGroup.getStocks()).hasSize(1);
            assertThat(savedGroup.getStocks().get(0).getStockCode()).isEqualTo(specialStockCode);
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
            given(watchlistGroupRepository.findByUserIdAndGroupCode(TEST_USER_ID, null))
                    .willReturn(Optional.empty());
            given(kisWatchlistService.getStocksByGroup(null)).willReturn(Collections.emptyList());
            given(watchlistGroupRepository.save(any(WatchlistGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).save(groupCaptor.capture());
            var savedGroup = groupCaptor.getValue();
            assertThat(savedGroup.getGroupCode()).isNull();
        }

        @Test
        @DisplayName("API null stockCode 처리")
        void API_null_stockCode_처리() {
            // given
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "테스트그룹")
            );
            var stockItems = List.of(
                    new WatchlistStockResponse.StockItem(null, "Null코드종목", "STK")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCode(TEST_USER_ID, "001"))
                    .willReturn(Optional.empty());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems);
            given(watchlistGroupRepository.save(any(WatchlistGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).save(groupCaptor.capture());
            var savedGroup = groupCaptor.getValue();
            assertThat(savedGroup.getStocks()).hasSize(1);
            assertThat(savedGroup.getStocks().get(0).getStockCode()).isNull();
        }

        @Test
        @DisplayName("API 빈 문자열 groupName 처리")
        void API_빈_문자열_groupName_처리() {
            // given
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "")
            );
            var stockItems = List.of(
                    new WatchlistStockResponse.StockItem("005930", "삼성전자", "STK")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCode(TEST_USER_ID, "001"))
                    .willReturn(Optional.empty());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems);
            given(watchlistGroupRepository.save(any(WatchlistGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            watchlistService.syncWatchlist();

            // then
            verify(watchlistGroupRepository).save(groupCaptor.capture());
            var savedGroup = groupCaptor.getValue();
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
            given(watchlistGroupRepository.findByUserIdAndGroupCode(TEST_USER_ID, "001"))
                    .willReturn(Optional.empty());
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
                    new WatchlistStockResponse.StockItem("005930", "삼성전자", "STK")
            );

            given(kisProperties.userId()).willReturn(TEST_USER_ID);
            given(kisWatchlistService.getWatchlistGroups()).willReturn(groupItems);
            given(watchlistGroupRepository.findByUserIdAndGroupCode(TEST_USER_ID, "001"))
                    .willReturn(Optional.empty());
            given(kisWatchlistService.getStocksByGroup("001")).willReturn(stockItems);
            willThrow(new DataIntegrityViolationException("무결성 제약조건 위반"))
                    .given(watchlistGroupRepository).save(any(WatchlistGroup.class));

            // when & then
            assertThatThrownBy(() -> watchlistService.syncWatchlist())
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("무결성 제약조건 위반");
        }
    }
}
