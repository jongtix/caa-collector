package com.custom.trader.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Set;

/**
 * HTTP 요청/응답 로깅 인터셉터 (민감 헤더 마스킹).
 *
 * <p>RestClient의 HTTP 요청/응답을 로깅하되, 민감한 헤더 값은 마스킹 처리합니다.
 * 개발 환경(local, dev)에서만 활성화되어 디버깅을 지원하며,
 * 운영 환경에서는 등록하지 않아 성능 영향을 최소화합니다.</p>
 *
 * <p>마스킹 대상 헤더:
 * <ul>
 *   <li>{@code appkey} - 한국투자증권 API 앱 키</li>
 *   <li>{@code appsecret} - 한국투자증권 API 앱 시크릿</li>
 *   <li>{@code authorization} - OAuth 2.0 Bearer 토큰</li>
 * </ul>
 * </p>
 *
 * @see RestClientConfig
 */
@Slf4j
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final String MASK = "***MASKED***";
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "appkey",
            "appsecret",
            "authorization"
    );

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        logRequest(request);

        var response = execution.execute(request, body);

        logResponse(response);

        return response;
    }

    private void logRequest(HttpRequest request) {
        if (!log.isDebugEnabled()) {
            return;
        }

        var maskedHeaders = new StringBuilder();
        request.getHeaders().forEach((name, values) -> {
            var displayValue = isSensitiveHeader(name)
                    ? MASK
                    : String.join(", ", values);
            maskedHeaders.append("  ").append(name).append(": ").append(displayValue).append("\n");
        });

        log.debug("[HTTP Request] {} {}\nHeaders:\n{}",
                request.getMethod(),
                request.getURI(),
                maskedHeaders
        );
    }

    private void logResponse(ClientHttpResponse response) throws IOException {
        if (!log.isDebugEnabled()) {
            return;
        }

        log.debug("[HTTP Response] Status: {} {}",
                response.getStatusCode().value(),
                response.getStatusText()
        );
    }

    private boolean isSensitiveHeader(String headerName) {
        return SENSITIVE_HEADERS.contains(headerName.toLowerCase());
    }
}
