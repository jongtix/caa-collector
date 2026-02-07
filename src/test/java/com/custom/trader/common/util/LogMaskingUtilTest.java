package com.custom.trader.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogMaskingUtil 테스트")
class LogMaskingUtilTest {

    @Nested
    @DisplayName("maskUserId 테스트")
    class MaskUserIdTest {

        @Test
        @DisplayName("정상 케이스: 앞 2자 + 8개 별표")
        void maskUserId_normal() {
            // given
            String userId = "P123456789";

            // when
            String masked = LogMaskingUtil.maskUserId(userId);

            // then
            assertThat(masked).isEqualTo("P1********");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"P", "P1"})
        @DisplayName("경계값: null, 빈 문자열, 2자 이하")
        void maskUserId_boundary(String userId) {
            // when
            String masked = LogMaskingUtil.maskUserId(userId);

            // then
            assertThat(masked).isEqualTo("***");
        }

        @Test
        @DisplayName("최소 유효값: 3자리")
        void maskUserId_minValid() {
            // given
            String userId = "P12";

            // when
            String masked = LogMaskingUtil.maskUserId(userId);

            // then
            assertThat(masked).isEqualTo("P1********");
        }

        @Test
        @DisplayName("동시성: 100개 스레드에서 동시 호출")
        void maskUserId_concurrency() throws InterruptedException {
            // given
            int threadCount = 100;
            String userId = "P123456789";
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<String> results = Collections.synchronizedList(new ArrayList<>());

            // when
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        results.add(LogMaskingUtil.maskUserId(userId));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            executorService.shutdown();

            // then
            assertThat(results).hasSize(threadCount);
            assertThat(results).allMatch(result -> result.equals("P1********"));
        }
    }

    @Nested
    @DisplayName("maskAccountNumber 테스트")
    class MaskAccountNumberTest {

        @Test
        @DisplayName("정상 케이스: 7개 별표 + 뒤 4자")
        void maskAccountNumber_normal() {
            // given
            String accountNumber = "12345678-01";

            // when
            String masked = LogMaskingUtil.maskAccountNumber(accountNumber);

            // then
            assertThat(masked).isEqualTo("*******8-01");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"1", "12", "123", "1234"})
        @DisplayName("경계값: null, 빈 문자열, 4자 이하")
        void maskAccountNumber_boundary(String accountNumber) {
            // when
            String masked = LogMaskingUtil.maskAccountNumber(accountNumber);

            // then
            assertThat(masked).isEqualTo("***");
        }

        @Test
        @DisplayName("최소 유효값: 5자리")
        void maskAccountNumber_minValid() {
            // given
            String accountNumber = "12345";

            // when
            String masked = LogMaskingUtil.maskAccountNumber(accountNumber);

            // then
            assertThat(masked).isEqualTo("*******2345");
        }

        @Test
        @DisplayName("동시성: 100개 스레드에서 동시 호출")
        void maskAccountNumber_concurrency() throws InterruptedException {
            // given
            int threadCount = 100;
            String accountNumber = "12345678-01";
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<String> results = Collections.synchronizedList(new ArrayList<>());

            // when
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        results.add(LogMaskingUtil.maskAccountNumber(accountNumber));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            executorService.shutdown();

            // then
            assertThat(results).hasSize(threadCount);
            assertThat(results).allMatch(result -> result.equals("*******8-01"));
        }
    }

    @Nested
    @DisplayName("maskAppKey 테스트")
    class MaskAppKeyTest {

        @Test
        @DisplayName("정상 케이스: 앞 4자 + 12개 별표")
        void maskAppKey_normal() {
            // given
            String appKey = "PSabcdefghijklmnop";

            // when
            String masked = LogMaskingUtil.maskAppKey(appKey);

            // then
            assertThat(masked).isEqualTo("PSab************");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"P", "PS", "PSa", "PSab"})
        @DisplayName("경계값: null, 빈 문자열, 4자 이하")
        void maskAppKey_boundary(String appKey) {
            // when
            String masked = LogMaskingUtil.maskAppKey(appKey);

            // then
            assertThat(masked).isEqualTo("***");
        }

        @Test
        @DisplayName("최소 유효값: 5자리")
        void maskAppKey_minValid() {
            // given
            String appKey = "PSabc";

            // when
            String masked = LogMaskingUtil.maskAppKey(appKey);

            // then
            assertThat(masked).isEqualTo("PSab************");
        }

        @Test
        @DisplayName("동시성: 100개 스레드에서 동시 호출")
        void maskAppKey_concurrency() throws InterruptedException {
            // given
            int threadCount = 100;
            String appKey = "PSabcdefghijklmnop";
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<String> results = Collections.synchronizedList(new ArrayList<>());

            // when
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        results.add(LogMaskingUtil.maskAppKey(appKey));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            executorService.shutdown();

            // then
            assertThat(results).hasSize(threadCount);
            assertThat(results).allMatch(result -> result.equals("PSab************"));
        }
    }

    @Nested
    @DisplayName("maskAppSecret 테스트")
    class MaskAppSecretTest {

        @Test
        @DisplayName("정상 케이스: 22개 별표")
        void maskAppSecret_normal() {
            // given
            String appSecret = "abcdefghijklmnopqrstuvwxyz1234567890";

            // when
            String masked = LogMaskingUtil.maskAppSecret(appSecret);

            // then
            assertThat(masked).isEqualTo("**********************");
        }

        @Test
        @DisplayName("null 케이스")
        void maskAppSecret_null() {
            // when
            String masked = LogMaskingUtil.maskAppSecret(null);

            // then
            assertThat(masked).isEqualTo("***");
        }

        @Test
        @DisplayName("빈 문자열 케이스")
        void maskAppSecret_empty() {
            // when
            String masked = LogMaskingUtil.maskAppSecret("");

            // then
            assertThat(masked).isEqualTo("**********************");
        }

        @Test
        @DisplayName("동시성: 100개 스레드에서 동시 호출")
        void maskAppSecret_concurrency() throws InterruptedException {
            // given
            int threadCount = 100;
            String appSecret = "abcdefghijklmnopqrstuvwxyz1234567890";
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<String> results = Collections.synchronizedList(new ArrayList<>());

            // when
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        results.add(LogMaskingUtil.maskAppSecret(appSecret));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            executorService.shutdown();

            // then
            assertThat(results).hasSize(threadCount);
            assertThat(results).allMatch(result -> result.equals("**********************"));
        }
    }

    @Nested
    @DisplayName("maskAccessToken 테스트")
    class MaskAccessTokenTest {

        @Test
        @DisplayName("정상 케이스: 앞 4자 + 16개 별표")
        void maskAccessToken_normal() {
            // given
            String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0";

            // when
            String masked = LogMaskingUtil.maskAccessToken(accessToken);

            // then
            assertThat(masked).isEqualTo("eyJh****************");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"e", "ey", "eyJ", "eyJh"})
        @DisplayName("경계값: null, 빈 문자열, 4자 이하")
        void maskAccessToken_boundary(String accessToken) {
            // when
            String masked = LogMaskingUtil.maskAccessToken(accessToken);

            // then
            assertThat(masked).isEqualTo("***");
        }

        @Test
        @DisplayName("최소 유효값: 5자리")
        void maskAccessToken_minValid() {
            // given
            String accessToken = "eyJhb";

            // when
            String masked = LogMaskingUtil.maskAccessToken(accessToken);

            // then
            assertThat(masked).isEqualTo("eyJh****************");
        }

        @Test
        @DisplayName("고정 길이 출력: 토큰 길이에 관계없이 동일한 마스킹 길이")
        void maskAccessToken_fixedLengthOutput() {
            // given
            String shortToken = "eyJhb";
            String longToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0";

            // when
            String maskedShort = LogMaskingUtil.maskAccessToken(shortToken);
            String maskedLong = LogMaskingUtil.maskAccessToken(longToken);

            // then
            assertThat(maskedShort).hasSize(maskedLong.length());
            assertThat(maskedShort).isEqualTo("eyJh****************");
            assertThat(maskedLong).isEqualTo("eyJh****************");
        }

        @Test
        @DisplayName("원본 토큰이 마스킹 결과에 포함되지 않음")
        void maskAccessToken_doesNotContainOriginal() {
            // given
            String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";

            // when
            String masked = LogMaskingUtil.maskAccessToken(accessToken);

            // then
            assertThat(masked).doesNotContain("bGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
            assertThat(masked).doesNotContain(accessToken);
        }

        @Test
        @DisplayName("동시성: 100개 스레드에서 동시 호출")
        void maskAccessToken_concurrency() throws InterruptedException {
            // given
            int threadCount = 100;
            String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<String> results = Collections.synchronizedList(new ArrayList<>());

            // when
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        results.add(LogMaskingUtil.maskAccessToken(accessToken));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            executorService.shutdown();

            // then
            assertThat(results).hasSize(threadCount);
            assertThat(results).allMatch(result -> result.equals("eyJh****************"));
        }
    }

    @Nested
    @DisplayName("UTF-8 멀티바이트 및 특수문자 처리 (MA-08)")
    class UTF8MultibyteTest {

        @Test
        @DisplayName("이모지 마스킹: 서로게이트 페어 처리")
        void maskUserId_emoji() {
            // given
            var userId = "😀😀😀😀😀"; // 5개 이모지 (각 이모지는 char 2개로 구성)

            // when
            var masked = LogMaskingUtil.maskUserId(userId);

            // then
            // Java substring()은 char 기반이므로 이모지 1개(char 2개) 노출 + 8개 별표
            assertThat(masked).hasSize(10);
            assertThat(masked).startsWith("😀");
            assertThat(masked).endsWith("********");
        }

        @Test
        @DisplayName("한글 마스킹: 길이 일관성")
        void maskUserId_korean() {
            // given
            var userId = "한글계좌번호"; // 6자 한글 (각 3바이트이지만 char 1개)

            // when
            var masked = LogMaskingUtil.maskUserId(userId);

            // then
            // 앞 2자 + 8개 별표 = 10자
            assertThat(masked).hasSize(10);
            assertThat(masked).startsWith("한글");
            assertThat(masked).endsWith("********");
        }

        @Test
        @DisplayName("특수문자 마스킹: 보안 문자 포함")
        void maskUserId_specialCharacters() {
            // given
            var userId = "<script>alert('xss')</script>";

            // when
            var masked = LogMaskingUtil.maskUserId(userId);

            // then
            // 원본이 마스킹 결과에 포함되지 않아야 함
            assertThat(masked).doesNotContain("script");
            assertThat(masked).doesNotContain("alert");
            assertThat(masked).doesNotContain("xss");
            assertThat(masked).hasSize(10);
            assertThat(masked).startsWith("<s");
            assertThat(masked).endsWith("********");
        }

        @Test
        @DisplayName("혼합 문자 마스킹: 다양한 인코딩 결합")
        void maskUserId_mixed_encoding() {
            // given
            var userId = "😀한A1"; // 이모지(char 2개) + 한글(char 1개) + 영문 + 숫자

            // when
            var masked = LogMaskingUtil.maskUserId(userId);

            // then
            // substring(0, 2)는 char 2개를 추출하므로 이모지 1개만 포함
            assertThat(masked).hasSize(10);
            assertThat(masked).startsWith("😀");
            assertThat(masked).endsWith("********");
        }

        @Test
        @DisplayName("계좌번호 한글 마스킹")
        void maskAccountNumber_korean() {
            // given
            var accountNumber = "한글계좌1234";

            // when
            var masked = LogMaskingUtil.maskAccountNumber(accountNumber);

            // then
            // 7개 별표 + 뒤 4자 = 11자
            assertThat(masked).hasSize(11);
            assertThat(masked).startsWith("*******");
            assertThat(masked).endsWith("1234");
        }

        @Test
        @DisplayName("AppKey 이모지 마스킹: 서로게이트 페어 처리")
        void maskAppKey_emoji() {
            // given
            var appKey = "😀😀😀😀😀😀"; // 6개 이모지 (각 char 2개)

            // when
            var masked = LogMaskingUtil.maskAppKey(appKey);

            // then
            // substring(0, 4)는 char 4개를 추출하므로 이모지 2개
            assertThat(masked).hasSize(16);
            assertThat(masked).startsWith("😀😀");
            assertThat(masked).endsWith("************");
        }

        @Test
        @DisplayName("긴 멀티바이트 문자열: 메모리 안정성")
        void maskUserId_long_multibyte() {
            // given
            var userId = "한".repeat(1000); // 1000자 한글

            // when
            var masked = LogMaskingUtil.maskUserId(userId);

            // then
            assertThat(masked).hasSize(10);
            assertThat(masked).startsWith("한한");
            assertThat(masked).endsWith("********");
        }

        @Test
        @DisplayName("제어 문자 마스킹: 로그 인젝션 방어")
        void maskUserId_control_characters() {
            // given
            var userId = "user\n\r\tInjection"; // 제어 문자 포함

            // when
            var masked = LogMaskingUtil.maskUserId(userId);

            // then
            // 원본이 마스킹 결과에 포함되지 않아야 함
            assertThat(masked).doesNotContain("Injection");
            assertThat(masked).hasSize(10);
        }
    }
}
