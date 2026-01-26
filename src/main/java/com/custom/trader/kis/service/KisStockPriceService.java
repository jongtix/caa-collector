package com.custom.trader.kis.service;

import com.custom.trader.kis.client.KisRestClient;
import com.custom.trader.kis.config.KisAccountProperties;
import com.custom.trader.kis.config.KisApiEndpoint;
import com.custom.trader.kis.config.KisProperties;
import com.custom.trader.kis.dto.stockprice.DomesticStockDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.DomesticIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasStockDailyPriceResponse;
import com.custom.trader.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static com.custom.trader.stockprice.constant.StockPriceConstants.DATE_FORMATTER;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisStockPriceService {

    private final KisRestClient kisRestClient;
    private final KisAuthService kisAuthService;
    private final KisProperties kisProperties;

    public List<DomesticStockDailyPriceResponse.PriceItem> getDomesticStockDailyPrices(
            String stockCode, LocalDate startDate, LocalDate endDate) {
        var account = getDefaultAccount();
        var accessToken = kisAuthService.getAccessToken(account.name());

        log.info("Fetching domestic stock daily prices for: {} ({} ~ {})",
                stockCode, startDate, endDate);

        var response = kisRestClient.get(
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
                accessToken,
                account,
                DomesticStockDailyPriceResponse.class
        );

        log.info("Fetched {} domestic stock daily prices for {}",
                response.output2() != null ? response.output2().size() : 0, stockCode);
        return response.output2() != null ? response.output2() : Collections.emptyList();
    }

    public List<DomesticIndexDailyPriceResponse.PriceItem> getDomesticIndexDailyPrices(
            String indexCode, LocalDate startDate, LocalDate endDate) {
        var account = getDefaultAccount();
        var accessToken = kisAuthService.getAccessToken(account.name());

        log.info("Fetching domestic index daily prices for: {} ({} ~ {})",
                indexCode, startDate, endDate);

        var response = kisRestClient.get(
                KisApiEndpoint.DOMESTIC_INDEX_DAILY_PRICE,
                uriBuilder -> uriBuilder
                        .path(KisApiEndpoint.DOMESTIC_INDEX_DAILY_PRICE.getPath())
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_ISCD", indexCode)
                        .queryParam("FID_INPUT_DATE_1", startDate.format(DATE_FORMATTER))
                        .queryParam("FID_INPUT_DATE_2", endDate.format(DATE_FORMATTER))
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .build(),
                accessToken,
                account,
                DomesticIndexDailyPriceResponse.class
        );

        log.info("Fetched {} domestic index daily prices for {}",
                response.output2() != null ? response.output2().size() : 0, indexCode);
        return response.output2() != null ? response.output2() : Collections.emptyList();
    }

    public List<OverseasStockDailyPriceResponse.PriceItem> getOverseasStockDailyPrices(
            String stockCode, String exchangeCode, LocalDate startDate, LocalDate endDate) {
        var account = getDefaultAccount();
        var accessToken = kisAuthService.getAccessToken(account.name());

        log.info("Fetching overseas stock daily prices for: {} on {} ({} ~ {})",
                stockCode, exchangeCode, startDate, endDate);

        var response = kisRestClient.get(
                KisApiEndpoint.OVERSEAS_STOCK_DAILY_PRICE,
                uriBuilder -> uriBuilder
                        .path(KisApiEndpoint.OVERSEAS_STOCK_DAILY_PRICE.getPath())
                        .queryParam("EXCD", exchangeCode)
                        .queryParam("SYMB", stockCode)
                        .queryParam("GUBN", "0")
                        .queryParam("BYMD", endDate.format(DATE_FORMATTER))
                        .queryParam("MODP", "1")
                        .build(),
                accessToken,
                account,
                OverseasStockDailyPriceResponse.class
        );

        log.info("Fetched {} overseas stock daily prices for {} on {}",
                response.output2() != null ? response.output2().size() : 0, stockCode, exchangeCode);
        return response.output2() != null ? response.output2() : Collections.emptyList();
    }

    public List<OverseasIndexDailyPriceResponse.PriceItem> getOverseasIndexDailyPrices(
            String indexCode, String exchangeCode, LocalDate startDate, LocalDate endDate) {
        var account = getDefaultAccount();
        var accessToken = kisAuthService.getAccessToken(account.name());

        log.info("Fetching overseas index daily prices for: {} on {} ({} ~ {})",
                indexCode, exchangeCode, startDate, endDate);

        var response = kisRestClient.get(
                KisApiEndpoint.OVERSEAS_INDEX_DAILY_PRICE,
                uriBuilder -> uriBuilder
                        .path(KisApiEndpoint.OVERSEAS_INDEX_DAILY_PRICE.getPath())
                        .queryParam("FID_COND_MRKT_DIV_CODE", "N")
                        .queryParam("FID_INPUT_ISCD", indexCode)
                        .queryParam("FID_INPUT_DATE_1", startDate.format(DATE_FORMATTER))
                        .queryParam("FID_INPUT_DATE_2", endDate.format(DATE_FORMATTER))
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .build(),
                accessToken,
                account,
                OverseasIndexDailyPriceResponse.class
        );

        log.info("Fetched {} overseas index daily prices for {} on {}",
                response.output2() != null ? response.output2().size() : 0, indexCode, exchangeCode);
        return response.output2() != null ? response.output2() : Collections.emptyList();
    }

    private KisAccountProperties getDefaultAccount() {
        if (kisProperties.accounts().isEmpty()) {
            throw new KisApiException("No accounts configured");
        }
        return kisProperties.accounts().getFirst();
    }
}
