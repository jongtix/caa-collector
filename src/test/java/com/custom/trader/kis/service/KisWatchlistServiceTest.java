package com.custom.trader.kis.service;

import com.custom.trader.kis.client.KisRestClient;
import com.custom.trader.kis.config.KisAccountProperties;
import com.custom.trader.kis.config.KisApiEndpoint;
import com.custom.trader.kis.config.KisProperties;
import com.custom.trader.kis.dto.watchlist.WatchlistGroupResponse;
import com.custom.trader.kis.dto.watchlist.WatchlistStockResponse;
import com.custom.trader.kis.exception.KisApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KisWatchlistServiceTest {

    @Mock
    private KisRestClient kisRestClient;

    @Mock
    private KisAuthService kisAuthService;

    @Mock
    private KisProperties kisProperties;

    private KisWatchlistService kisWatchlistService;

    private KisAccountProperties testAccount;

    @BeforeEach
    void setUp() {
        kisWatchlistService = new KisWatchlistService(kisRestClient, kisAuthService, kisProperties);
        testAccount = new KisAccountProperties("테스트계정", "12345678", "appKey123", "appSecret123");
    }

    @Nested
    @DisplayName("getWatchlistGroups 메소드")
    class GetWatchlistGroups {

        @Test
        @DisplayName("관심그룹 목록 정상 반환")
        void 관심그룹_목록_정상_반환() {
            // given
            var groupItems = List.of(
                    new WatchlistGroupResponse.GroupItem("001", "관심그룹1"),
                    new WatchlistGroupResponse.GroupItem("002", "관심그룹2")
            );
            var response = new WatchlistGroupResponse("0", "00000000", "정상", groupItems);

            given(kisAuthService.getDefaultAccount()).willReturn(testAccount);
            given(kisAuthService.getAccessToken(testAccount.name())).willReturn("test-token");
            given(kisProperties.userId()).willReturn("testUser");
            given(kisRestClient.get(
                    eq(KisApiEndpoint.WATCHLIST_GROUP),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(WatchlistGroupResponse.class)
            )).willReturn(response);

            // when
            var result = kisWatchlistService.getWatchlistGroups();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).interGrpCode()).isEqualTo("001");
            assertThat(result.get(0).interGrpName()).isEqualTo("관심그룹1");
            assertThat(result.get(1).interGrpCode()).isEqualTo("002");
            assertThat(result.get(1).interGrpName()).isEqualTo("관심그룹2");
        }

        @Test
        @DisplayName("output2가 null이면 빈 리스트 반환")
        void output2가_null이면_빈_리스트_반환() {
            // given
            var response = new WatchlistGroupResponse("0", "00000000", "정상", null);

            given(kisAuthService.getDefaultAccount()).willReturn(testAccount);
            given(kisAuthService.getAccessToken(testAccount.name())).willReturn("test-token");
            given(kisProperties.userId()).willReturn("testUser");
            given(kisRestClient.get(
                    eq(KisApiEndpoint.WATCHLIST_GROUP),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(WatchlistGroupResponse.class)
            )).willReturn(response);

            // when
            var result = kisWatchlistService.getWatchlistGroups();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getStocksByGroup 메소드")
    class GetStocksByGroup {

        @Test
        @DisplayName("그룹코드로 종목 목록 정상 반환")
        void 그룹코드로_종목_목록_정상_반환() {
            // given
            var stockItems = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX"),
                    new WatchlistStockResponse.StockItem("J", "000660", "SK하이닉스", "KRX")
            );
            var response = new WatchlistStockResponse("0", "00000000", "정상", stockItems);
            String groupCode = "001";

            given(kisAuthService.getDefaultAccount()).willReturn(testAccount);
            given(kisAuthService.getAccessToken(testAccount.name())).willReturn("test-token");
            given(kisRestClient.get(
                    eq(KisApiEndpoint.WATCHLIST_STOCK),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(WatchlistStockResponse.class)
            )).willReturn(response);

            // when
            var result = kisWatchlistService.getStocksByGroup(groupCode);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).jongCode()).isEqualTo("005930");
            assertThat(result.get(0).htsKorIsnm()).isEqualTo("삼성전자");
            assertThat(result.get(1).jongCode()).isEqualTo("000660");
            assertThat(result.get(1).htsKorIsnm()).isEqualTo("SK하이닉스");
        }

        @Test
        @DisplayName("종목 output2가 null이면 빈 리스트 반환")
        void 종목_output2가_null이면_빈_리스트_반환() {
            // given
            var response = new WatchlistStockResponse("0", "00000000", "정상", null);
            String groupCode = "001";

            given(kisAuthService.getDefaultAccount()).willReturn(testAccount);
            given(kisAuthService.getAccessToken(testAccount.name())).willReturn("test-token");
            given(kisRestClient.get(
                    eq(KisApiEndpoint.WATCHLIST_STOCK),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(WatchlistStockResponse.class)
            )).willReturn(response);

            // when
            var result = kisWatchlistService.getStocksByGroup(groupCode);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDefaultAccount 메소드 호출 검증 (KisAuthService 위임)")
    class GetDefaultAccount {

        @Test
        @DisplayName("계정이 없으면 예외 발생")
        void 계정이_없으면_예외_발생() {
            // given
            given(kisAuthService.getDefaultAccount()).willThrow(new KisApiException("No accounts configured"));

            // when & then
            assertThatThrownBy(() -> kisWatchlistService.getWatchlistGroups())
                    .isInstanceOf(KisApiException.class)
                    .hasMessageContaining("No accounts configured");
        }

        @Test
        @DisplayName("첫 번째 계정 사용")
        void 첫번째_계정_사용() {
            // given
            var secondAccount = new KisAccountProperties("두번째계정", "87654321", "appKey456", "appSecret456");
            var groupItems = List.of(new WatchlistGroupResponse.GroupItem("001", "관심그룹1"));
            var response = new WatchlistGroupResponse("0", "00000000", "정상", groupItems);

            given(kisAuthService.getDefaultAccount()).willReturn(testAccount);
            given(kisAuthService.getAccessToken(testAccount.name())).willReturn("test-token");
            given(kisProperties.userId()).willReturn("testUser");
            given(kisRestClient.get(
                    eq(KisApiEndpoint.WATCHLIST_GROUP),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(WatchlistGroupResponse.class)
            )).willReturn(response);

            // when
            kisWatchlistService.getWatchlistGroups();

            // then
            verify(kisAuthService).getDefaultAccount();
            verify(kisAuthService).getAccessToken(testAccount.name());
            verify(kisRestClient).get(
                    eq(KisApiEndpoint.WATCHLIST_GROUP),
                    any(),
                    eq("test-token"),
                    eq(testAccount),
                    eq(WatchlistGroupResponse.class)
            );
        }
    }
}
