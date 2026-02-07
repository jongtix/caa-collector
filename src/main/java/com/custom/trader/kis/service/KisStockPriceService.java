package com.custom.trader.kis.service;

import static com.custom.trader.common.constant.DateFormatConstants.DATE_FORMATTER;

import com.custom.trader.kis.client.KisRestClient;
import com.custom.trader.kis.config.KisAccountProperties;
import com.custom.trader.kis.config.KisApiEndpoint;
import com.custom.trader.kis.config.KisProperties;
import com.custom.trader.kis.dto.KisApiResponse;
import com.custom.trader.kis.dto.stockprice.DomesticStockDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.DomesticIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasStockDailyPriceResponse;
import com.custom.trader.kis.exception.KisApiException;
import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisStockPriceService {

    private final KisRestClient kisRestClient;
    private final KisAuthService kisAuthService;
    private final KisProperties kisProperties;

    /**
     * API 응답에서 output2를 안전하게 추출합니다.
     *
     * <p>output2가 null인 경우 빈 리스트를 반환하여 NullPointerException을 방지합니다.</p>
     *
     * @param <T> KisApiResponse를 구현하는 응답 타입
     * @param <I> output2에 포함된 아이템 타입
     * @param response API 응답 객체
     * @param outputExtractor output2 추출 함수
     * @return output2 리스트 (null인 경우 빈 리스트)
     */
    private <T extends KisApiResponse, I> List<I> safeExtractOutput(
            T response,
            Function<T, List<I>> outputExtractor) {
        var output = outputExtractor.apply(response);
        return output != null ? output : Collections.emptyList();
    }

    /**
     * 일간 시세 데이터를 조회하는 공통 메서드
     *
     * <p>모든 AssetType(국내/해외 주식/지수)의 일간 시세 조회를 통합 처리합니다.</p>
     *
     * @param <T> KisApiResponse를 구현하는 응답 타입
     * @param <I> output2에 포함된 아이템 타입
     * @param endpoint API 엔드포인트
     * @param uriBuilderFunction URI 생성 함수 (쿼리 파라미터 포함)
     * @param responseType 응답 타입 클래스
     * @param outputExtractor output2 추출 함수 (null-safe)
     * @param assetTypeDescription 자산 타입 설명 (로그 출력용)
     * @param code 종목/지수 코드
     * @return 시세 데이터 리스트 (빈 리스트는 Collections.emptyList())
     */
    private <T extends KisApiResponse, I> List<I> fetchDailyPrices(
            KisApiEndpoint endpoint,
            Function<UriBuilder, URI> uriBuilderFunction,
            Class<T> responseType,
            Function<T, List<I>> outputExtractor,
            String assetTypeDescription,
            String code) {
        // 계정 정보 획득
        var account = kisAuthService.getDefaultAccount();

        // 액세스 토큰 획득
        var accessToken = kisAuthService.getAccessToken(account.name());

        // 요청 로그 출력
        log.info("Fetching {} daily prices for: {}", assetTypeDescription, code);

        // API 호출
        var response = kisRestClient.get(
                endpoint,
                uriBuilderFunction,
                accessToken,
                account,
                responseType
        );

        // output2 추출
        var items = outputExtractor.apply(response);

        // 응답 로그 출력
        log.info("Fetched {} {} daily prices for {}", items.size(), assetTypeDescription, code);

        return items;
    }

    public List<DomesticStockDailyPriceResponse.PriceItem> getDomesticStockDailyPrices(
            String stockCode, LocalDate startDate, LocalDate endDate) {
        return fetchDailyPrices(
                KisApiEndpoint.DOMESTIC_STOCK_DAILY_PRICE,
                uriBuilder -> uriBuilder
                        .path(KisApiEndpoint.DOMESTIC_STOCK_DAILY_PRICE.getPath())
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", stockCode)
                        .queryParam("FID_INPUT_DATE_1", startDate.format(DATE_FORMATTER))
                        .queryParam("FID_INPUT_DATE_2", endDate.format(DATE_FORMATTER))
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .queryParam("FID_ORG_ADJ_PRC", "0")
                        .build(),
                DomesticStockDailyPriceResponse.class,
                response -> safeExtractOutput(response, DomesticStockDailyPriceResponse::output2),
                "domestic stock",
                stockCode
        );
    }

    public List<DomesticIndexDailyPriceResponse.PriceItem> getDomesticIndexDailyPrices(
            String indexCode, LocalDate startDate, LocalDate endDate) {
        return fetchDailyPrices(
                KisApiEndpoint.DOMESTIC_INDEX_DAILY_PRICE,
                uriBuilder -> uriBuilder
                        .path(KisApiEndpoint.DOMESTIC_INDEX_DAILY_PRICE.getPath())
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_ISCD", indexCode)
                        .queryParam("FID_INPUT_DATE_1", startDate.format(DATE_FORMATTER))
                        .queryParam("FID_INPUT_DATE_2", endDate.format(DATE_FORMATTER))
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .build(),
                DomesticIndexDailyPriceResponse.class,
                response -> safeExtractOutput(response, DomesticIndexDailyPriceResponse::output2),
                "domestic index",
                indexCode
        );
    }

    public List<OverseasStockDailyPriceResponse.PriceItem> getOverseasStockDailyPrices(
            String stockCode, String exchangeCode, LocalDate startDate, LocalDate endDate) {
        return fetchDailyPrices(
                KisApiEndpoint.OVERSEAS_STOCK_DAILY_PRICE,
                uriBuilder -> uriBuilder
                        .path(KisApiEndpoint.OVERSEAS_STOCK_DAILY_PRICE.getPath())
                        .queryParam("EXCD", exchangeCode)
                        .queryParam("SYMB", stockCode)
                        .queryParam("GUBN", "0")
                        .queryParam("BYMD", endDate.format(DATE_FORMATTER))
                        .queryParam("MODP", "1")
                        .build(),
                OverseasStockDailyPriceResponse.class,
                response -> safeExtractOutput(response, OverseasStockDailyPriceResponse::output2),
                "overseas stock",
                stockCode
        );
    }

    public List<OverseasIndexDailyPriceResponse.PriceItem> getOverseasIndexDailyPrices(
            String indexCode, String exchangeCode, LocalDate startDate, LocalDate endDate) {
        return fetchDailyPrices(
                KisApiEndpoint.OVERSEAS_INDEX_DAILY_PRICE,
                uriBuilder -> uriBuilder
                        .path(KisApiEndpoint.OVERSEAS_INDEX_DAILY_PRICE.getPath())
                        .queryParam("FID_COND_MRKT_DIV_CODE", "N")
                        .queryParam("FID_INPUT_ISCD", indexCode)
                        .queryParam("FID_INPUT_DATE_1", startDate.format(DATE_FORMATTER))
                        .queryParam("FID_INPUT_DATE_2", endDate.format(DATE_FORMATTER))
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .build(),
                OverseasIndexDailyPriceResponse.class,
                response -> safeExtractOutput(response, OverseasIndexDailyPriceResponse::output2),
                "overseas index",
                indexCode
        );
    }

}
