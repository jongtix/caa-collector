package com.custom.trader.kis.client;

import com.custom.trader.kis.config.KisAccountProperties;
import com.custom.trader.kis.config.KisApiEndpoint;
import com.custom.trader.kis.dto.KisApiResponse;
import com.custom.trader.kis.exception.KisApiException;
import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.function.Function;

/**
 * 한국투자증권 Open API 호출을 위한 REST 클라이언트.
 *
 * <p>KIS API에 대한 모든 HTTP GET 요청을 처리하며, 다음 기능을 제공합니다:
 * <ul>
 *   <li>Rate Limiting: 초당 20회 요청 제한 (자동 적용)</li>
 *   <li>인증 헤더 자동 추가 (Bearer Token, AppKey, AppSecret)</li>
 *   <li>응답 검증 (성공 코드 확인, 에러 처리)</li>
 * </ul>
 * </p>
 *
 * <p>Rate Limiting:
 * <ul>
 *   <li>모든 {@link #get} 호출은 자동으로 {@link RateLimiter#acquire()}를 통해 제한됨</li>
 *   <li>호출자는 Rate Limiting을 신경 쓸 필요 없음</li>
 *   <li>초당 20회 제한을 넘어가면 자동으로 대기</li>
 * </ul>
 * </p>
 *
 * <p>사용 예시:
 * <pre>{@code
 * var response = kisRestClient.get(
 *     KisApiEndpoint.DOMESTIC_STOCK_DAILY_PRICE,
 *     uriBuilder -> uriBuilder.path(...).build(),
 *     accessToken,
 *     account,
 *     DomesticStockDailyPriceResponse.class
 * );
 * }</pre>
 * </p>
 *
 * @see RateLimiter
 * @see KisApiEndpoint
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings("UnstableApiUsage")
public class KisRestClient {

    private static final String CUSTTYPE = "P";
    private static final String SUCCESS_CODE = "0";

    private final RestClient kisApiRestClient;
    private final RateLimiter kisApiRateLimiter;

    /**
     * KIS API에 HTTP GET 요청을 보냅니다.
     *
     * <p>Rate Limiter를 통해 초당 20회로 요청을 제한합니다.
     * 호출자는 Rate Limiting을 신경 쓸 필요가 없습니다.</p>
     *
     * @param endpoint API 엔드포인트 (TR ID 포함)
     * @param uriFunction URI 빌더 함수 (쿼리 파라미터 설정)
     * @param accessToken OAuth 2.0 액세스 토큰
     * @param account 계좌 정보 (AppKey, AppSecret 포함)
     * @param responseType 응답 타입 클래스
     * @param <T> 응답 타입 (KisApiResponse 구현체)
     * @return API 응답 객체
     * @throws KisApiException API 호출 실패 시 (응답 코드가 "0"이 아닌 경우)
     */
    public <T extends KisApiResponse> T get(
            KisApiEndpoint endpoint,
            Function<UriBuilder, URI> uriFunction,
            String accessToken,
            KisAccountProperties account,
            Class<T> responseType
    ) {
        kisApiRateLimiter.acquire();

        var response = kisApiRestClient.get()
                .uri(uriFunction)
                .headers(headers -> {
                    headers.set("authorization", "Bearer " + accessToken);
                    headers.set("appkey", account.appKey());
                    headers.set("appsecret", account.appSecret());
                    headers.set("tr_id", endpoint.getTrId());
                    headers.set("custtype", CUSTTYPE);
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(responseType);

        if (response == null || !SUCCESS_CODE.equals(response.rtCd())) {
            var errorMsg = response != null ? response.msg1() : "Unknown error";
            throw new KisApiException(errorMsg);
        }

        return response;
    }
}
