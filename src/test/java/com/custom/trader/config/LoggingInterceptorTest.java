package com.custom.trader.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * LoggingInterceptor 민감 헤더 마스킹 및 로깅 동작 테스트
 *
 * <p>HTTP 요청/응답 로깅 시 민감한 헤더(appkey, appsecret, authorization)가
 * 올바르게 마스킹되는지, 그리고 로그 레벨에 따라 조건부 로깅이 동작하는지 검증한다.</p>
 */
class LoggingInterceptorTest {

    private LoggingInterceptor interceptor;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        interceptor = new LoggingInterceptor();

        // Logback 로거 및 Appender 설정
        logger = (Logger) LoggerFactory.getLogger(LoggingInterceptor.class);
        originalLevel = logger.getLevel();

        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        // 원래 로그 레벨 복원
        logger.setLevel(originalLevel);
        logger.detachAppender(listAppender);
    }

    @Nested
    @DisplayName("민감 헤더 마스킹 테스트")
    class SensitiveHeaderMaskingTest {

        @Test
        @DisplayName("appkey 헤더는 마스킹됨")
        void appkey_헤더_마스킹됨() throws IOException {
            // given
            logger.setLevel(Level.DEBUG);
            var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com/api"));
            request.getHeaders().add("appkey", "secretkey123");

            var execution = mockExecution();

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("appkey: ***MASKED***"));
            assertThat(logMessages)
                    .noneMatch(event -> event.getFormattedMessage().contains("secretkey123"));
        }

        @Test
        @DisplayName("appsecret 헤더는 마스킹됨")
        void appsecret_헤더_마스킹됨() throws IOException {
            // given
            logger.setLevel(Level.DEBUG);
            var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com/api"));
            request.getHeaders().add("appsecret", "verysecret456");

            var execution = mockExecution();

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("appsecret: ***MASKED***"));
            assertThat(logMessages)
                    .noneMatch(event -> event.getFormattedMessage().contains("verysecret456"));
        }

        @Test
        @DisplayName("authorization 헤더는 마스킹됨")
        void authorization_헤더_마스킹됨() throws IOException {
            // given
            logger.setLevel(Level.DEBUG);
            var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com/api"));
            request.getHeaders().add("authorization", "Bearer token789");

            var execution = mockExecution();

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("authorization: ***MASKED***"));
            assertThat(logMessages)
                    .noneMatch(event -> event.getFormattedMessage().contains("Bearer token789"));
        }
    }

    @Nested
    @DisplayName("대소문자 무관 마스킹 테스트")
    class CaseInsensitiveMaskingTest {

        @Test
        @DisplayName("AppKey(대문자 포함)도 마스킹됨")
        void AppKey_대문자포함_마스킹됨() throws IOException {
            // given
            logger.setLevel(Level.DEBUG);
            var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com/api"));
            request.getHeaders().add("AppKey", "secretkey123");

            var execution = mockExecution();

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("AppKey: ***MASKED***"));
            assertThat(logMessages)
                    .noneMatch(event -> event.getFormattedMessage().contains("secretkey123"));
        }

        @Test
        @DisplayName("APPSECRET(모두 대문자)도 마스킹됨")
        void APPSECRET_모두대문자_마스킹됨() throws IOException {
            // given
            logger.setLevel(Level.DEBUG);
            var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com/api"));
            request.getHeaders().add("APPSECRET", "verysecret456");

            var execution = mockExecution();

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("APPSECRET: ***MASKED***"));
            assertThat(logMessages)
                    .noneMatch(event -> event.getFormattedMessage().contains("verysecret456"));
        }

        @Test
        @DisplayName("Authorization(혼합 케이스)도 마스킹됨")
        void Authorization_혼합케이스_마스킹됨() throws IOException {
            // given
            logger.setLevel(Level.DEBUG);
            var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com/api"));
            request.getHeaders().add("Authorization", "Bearer token789");

            var execution = mockExecution();

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("Authorization: ***MASKED***"));
            assertThat(logMessages)
                    .noneMatch(event -> event.getFormattedMessage().contains("Bearer token789"));
        }
    }

    @Nested
    @DisplayName("일반 헤더는 마스킹 안 됨")
    class NormalHeaderNotMaskedTest {

        @Test
        @DisplayName("Content-Type 헤더는 그대로 로깅됨")
        void ContentType_헤더_그대로_로깅됨() throws IOException {
            // given
            logger.setLevel(Level.DEBUG);
            var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com/api"));
            request.getHeaders().add("Content-Type", "application/json");

            var execution = mockExecution();

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("Content-Type: application/json"));
            assertThat(logMessages)
                    .noneMatch(event -> event.getFormattedMessage().contains("***MASKED***")
                            && event.getFormattedMessage().contains("Content-Type"));
        }

        @Test
        @DisplayName("Accept 헤더는 그대로 로깅됨")
        void Accept_헤더_그대로_로깅됨() throws IOException {
            // given
            logger.setLevel(Level.DEBUG);
            var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com/api"));
            request.getHeaders().add("Accept", "application/json");

            var execution = mockExecution();

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("Accept: application/json"));
            assertThat(logMessages)
                    .noneMatch(event -> event.getFormattedMessage().contains("***MASKED***")
                            && event.getFormattedMessage().contains("Accept"));
        }
    }

    @Nested
    @DisplayName("로그 레벨 조건부 로깅 테스트")
    class ConditionalLoggingTest {

        @Test
        @DisplayName("DEBUG 레벨이 아닐 때 로깅 안 됨")
        void DEBUG_레벨_아닐때_로깅안됨() throws IOException {
            // given
            logger.setLevel(Level.INFO);
            var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com/api"));
            request.getHeaders().add("appkey", "secretkey123");

            var execution = mockExecution();

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages).isEmpty();
        }

        @Test
        @DisplayName("DEBUG 레벨일 때 로깅됨")
        void DEBUG_레벨일때_로깅됨() throws IOException {
            // given
            logger.setLevel(Level.DEBUG);
            var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com/api"));
            request.getHeaders().add("Content-Type", "application/json");

            var execution = mockExecution();

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages).isNotEmpty();
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("HTTP Request"));
        }
    }

    @Nested
    @DisplayName("응답 상태 코드 로깅 테스트")
    class ResponseStatusCodeLoggingTest {

        @Test
        @DisplayName("200 OK 상태 코드가 로깅됨")
        void OK_200_상태코드_로깅됨() throws IOException {
            // given
            logger.setLevel(Level.DEBUG);
            var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com/api"));

            var execution = mockExecution(HttpStatus.OK);

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("Status: 200"));
        }

        @Test
        @DisplayName("401 Unauthorized 상태 코드가 로깅됨")
        void Unauthorized_401_상태코드_로깅됨() throws IOException {
            // given
            logger.setLevel(Level.DEBUG);
            var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com/api"));

            var execution = mockExecution(HttpStatus.UNAUTHORIZED);

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("Status: 401"));
        }

        @Test
        @DisplayName("500 Internal Server Error 상태 코드가 로깅됨")
        void InternalServerError_500_상태코드_로깅됨() throws IOException {
            // given
            logger.setLevel(Level.DEBUG);
            var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com/api"));

            var execution = mockExecution(HttpStatus.INTERNAL_SERVER_ERROR);

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("Status: 500"));
        }
    }

    @Nested
    @DisplayName("RestClient 통합 테스트 (MockWebServer)")
    class RestClientIntegrationTest {

        private okhttp3.mockwebserver.MockWebServer mockWebServer;
        private org.springframework.web.client.RestClient restClient;

        @BeforeEach
        void setUpMockWebServer() throws Exception {
            mockWebServer = new okhttp3.mockwebserver.MockWebServer();
            mockWebServer.start();

            // RestClient에 LoggingInterceptor 등록
            restClient = org.springframework.web.client.RestClient.builder()
                    .baseUrl(mockWebServer.url("/").toString())
                    .requestInterceptor(interceptor)
                    .build();

            logger.setLevel(Level.DEBUG);
        }

        @AfterEach
        void tearDownMockWebServer() throws Exception {
            if (mockWebServer != null) {
                mockWebServer.shutdown();
            }
        }

        @Test
        @DisplayName("RestClient가 LoggingInterceptor를 통해 요청/응답 로깅")
        void RestClient_LoggingInterceptor_요청응답_로깅() {
            // given
            mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"result\": \"success\"}")
                    .addHeader("Content-Type", "application/json"));

            // when
            restClient.get()
                    .uri("/api/test")
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .body(String.class);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("HTTP Request"));
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("GET"));
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("/api/test"));
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("HTTP Response"));
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("Status: 200"));
        }

        @Test
        @DisplayName("민감한 헤더(authorization)가 RestClient 통합 환경에서도 마스킹됨")
        void 민감한_헤더_RestClient_통합_마스킹() {
            // given
            mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                    .setResponseCode(200)
                    .setBody("{}"));

            // when
            restClient.get()
                    .uri("/api/protected")
                    .header("authorization", "Bearer secret-token-12345")
                    .retrieve()
                    .body(String.class);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("authorization: ***MASKED***"));
            assertThat(logMessages)
                    .noneMatch(event -> event.getFormattedMessage().contains("Bearer secret-token-12345"));
        }

        @Test
        @DisplayName("응답 상태 코드가 RestClient 통합 환경에서 올바르게 로깅됨")
        void 응답_상태코드_RestClient_통합_로깅() {
            // given
            mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                    .setResponseCode(401)
                    .setBody("{\"error\": \"unauthorized\"}"));

            // when & then
            try {
                restClient.get()
                        .uri("/api/unauthorized")
                        .retrieve()
                        .body(String.class);
            } catch (Exception ignored) {
                // 401 에러 무시 (로깅만 확인)
            }

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("Status: 401"));
        }

        @Test
        @DisplayName("POST 요청도 RestClient 통합 환경에서 로깅됨")
        void POST_요청_RestClient_통합_로깅() {
            // given
            mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                    .setResponseCode(201)
                    .setBody("{\"id\": 123}"));

            // when
            restClient.post()
                    .uri("/api/create")
                    .header("Content-Type", "application/json")
                    .body("{\"name\": \"test\"}")
                    .retrieve()
                    .body(String.class);

            // then
            var logMessages = listAppender.list;
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("POST"));
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("/api/create"));
            assertThat(logMessages)
                    .anyMatch(event -> event.getFormattedMessage().contains("Status: 201"));
        }
    }

    /**
     * 기본 200 OK 응답을 반환하는 Mock Execution 생성
     */
    private ClientHttpRequestExecution mockExecution() throws IOException {
        return mockExecution(HttpStatus.OK);
    }

    /**
     * 지정된 상태 코드를 반환하는 Mock Execution 생성
     */
    private ClientHttpRequestExecution mockExecution(HttpStatus status) throws IOException {
        var execution = mock(ClientHttpRequestExecution.class);
        var response = new MockClientHttpResponse(new byte[0], status);

        given(execution.execute(any(), any())).willReturn(response);

        return execution;
    }
}
