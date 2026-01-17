package com.custom.trader.kis.client;

import com.custom.trader.kis.config.KisAccountProperties;
import com.custom.trader.kis.config.KisApiEndpoint;
import com.custom.trader.kis.dto.KisApiResponse;
import com.custom.trader.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class KisRestClient {

    private static final String CUSTTYPE = "P";
    private static final String SUCCESS_CODE = "0";

    private final RestClient kisApiRestClient;

    public <T extends KisApiResponse> T get(
            KisApiEndpoint endpoint,
            Function<UriBuilder, URI> uriFunction,
            String accessToken,
            KisAccountProperties account,
            Class<T> responseType
    ) {
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
