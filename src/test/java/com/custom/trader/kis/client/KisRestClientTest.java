package com.custom.trader.kis.client;

import com.custom.trader.kis.config.KisAccountProperties;
import com.custom.trader.kis.config.KisApiEndpoint;
import com.custom.trader.kis.dto.KisApiResponse;
import com.custom.trader.kis.exception.KisApiException;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("UnstableApiUsage")
class KisRestClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RateLimiter kisApiRateLimiter;

    @Mock
    private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private KisRestClient kisRestClient;

    private KisAccountProperties account;
    private static final String ACCESS_TOKEN = "test-access-token";

    @BeforeEach
    void setUp() {
        kisRestClient = new KisRestClient(restClient, kisApiRateLimiter);
        account = new KisAccountProperties("테스트", "12345678", "appKey", "appSecret");
        given(kisApiRateLimiter.acquire()).willReturn(0.0);
    }

    @Test
    @DisplayName("API_호출_성공시_응답_반환")
    void API_호출_성공시_응답_반환() {
        // given
        var expectedResponse = new TestKisApiResponse("0", "정상처리 되었습니다");
        setupMockRestClient(expectedResponse);

        // when
        var result = kisRestClient.get(
                KisApiEndpoint.WATCHLIST_GROUP,
                uriBuilder -> URI.create("/test"),
                ACCESS_TOKEN,
                account,
                TestKisApiResponse.class
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.rtCd()).isEqualTo("0");
        assertThat(result.msg1()).isEqualTo("정상처리 되었습니다");
        verify(kisApiRateLimiter, times(1)).acquire();
    }

    @Test
    @DisplayName("응답이_null이면_예외_발생")
    void 응답이_null이면_예외_발생() {
        // given
        setupMockRestClient(null);

        // when & then
        assertThatThrownBy(() -> kisRestClient.get(
                KisApiEndpoint.WATCHLIST_GROUP,
                uriBuilder -> URI.create("/test"),
                ACCESS_TOKEN,
                account,
                TestKisApiResponse.class
        ))
                .isInstanceOf(KisApiException.class)
                .hasMessageContaining("Unknown error");
    }

    @Test
    @DisplayName("응답코드가_실패면_예외_발생")
    void 응답코드가_실패면_예외_발생() {
        // given
        var errorResponse = new TestKisApiResponse("1", "잘못된 요청입니다");
        setupMockRestClient(errorResponse);

        // when & then
        assertThatThrownBy(() -> kisRestClient.get(
                KisApiEndpoint.WATCHLIST_GROUP,
                uriBuilder -> URI.create("/test"),
                ACCESS_TOKEN,
                account,
                TestKisApiResponse.class
        ))
                .isInstanceOf(KisApiException.class)
                .hasMessageContaining("잘못된 요청입니다");
    }

    @SuppressWarnings("unchecked")
    private void setupMockRestClient(TestKisApiResponse response) {
        given(restClient.get()).willReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).willReturn(requestHeadersSpec);
        doReturn(requestHeadersSpec).when(requestHeadersSpec).headers(any());
        doReturn(requestHeadersSpec).when(requestHeadersSpec).accept(any());
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(any(Class.class))).willReturn(response);
    }

    record TestKisApiResponse(String rtCd, String msg1) implements KisApiResponse {}
}
