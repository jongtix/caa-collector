package com.custom.trader.kis.service;

import com.custom.trader.common.exception.ErrorCode;
import com.custom.trader.common.util.RedisKeyHasher;
import com.custom.trader.common.util.TokenEncryptor;
import com.custom.trader.kis.config.KisAccountProperties;
import com.custom.trader.kis.config.KisProperties;
import com.custom.trader.kis.dto.auth.KisTokenRequest;
import com.custom.trader.kis.dto.auth.KisTokenResponse;
import com.custom.trader.kis.exception.KisApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KisAuthServiceTest {

    private static final String REDIS_KEY_PREFIX = "kis:token:";
    private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mock
    private RestClient kisRestClient;

    @Mock
    private KisProperties kisProperties;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisKeyHasher redisKeyHasher;

    @Mock
    private TokenEncryptor tokenEncryptor;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private KisAuthService kisAuthService;

    private KisAccountProperties testAccount;

    @BeforeEach
    void setUp() {
        kisAuthService = new KisAuthService(kisRestClient, kisProperties, redisTemplate, redisKeyHasher, tokenEncryptor);
        testAccount = new KisAccountProperties("테스트계정", "12345678", "appKey123", "appSecret123");
    }

    @Nested
    @DisplayName("getAccessToken 메소드")
    class GetAccessToken {

        @Test
        @DisplayName("캐시된 토큰이 있으면 바로 반환")
        void 캐시된_토큰이_있으면_바로_반환() {
            // given
            String cachedToken = "cached-access-token";
            String encryptedToken = "encrypted-token";
            String hashedKey = "hashed-key";
            String cacheKey = REDIS_KEY_PREFIX + hashedKey;

            given(kisProperties.accounts()).willReturn(List.of(testAccount));
            given(redisKeyHasher.hash(testAccount.accountNumber())).willReturn(hashedKey);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(cacheKey)).willReturn(encryptedToken);
            given(tokenEncryptor.decrypt(encryptedToken)).willReturn(cachedToken);

            // when
            String result = kisAuthService.getAccessToken(testAccount.name());

            // then
            assertThat(result).isEqualTo(cachedToken);
            verify(kisRestClient, never()).post();
        }

        @Test
        @DisplayName("캐시 미스 시 새 토큰 요청 후 반환")
        void 캐시_미스시_새_토큰_요청_후_반환() {
            // given
            String newToken = "new-access-token";
            String encryptedToken = "encrypted-new-token";
            String hashedKey = "hashed-key";
            String cacheKey = REDIS_KEY_PREFIX + hashedKey;
            String expiryTime = LocalDateTime.now().plusHours(24).format(EXPIRY_FORMATTER);
            KisTokenResponse tokenResponse = new KisTokenResponse(newToken, expiryTime, "Bearer", 86400L);

            given(kisProperties.accounts()).willReturn(List.of(testAccount));
            given(redisKeyHasher.hash(testAccount.accountNumber())).willReturn(hashedKey);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(cacheKey)).willReturn(null);
            given(tokenEncryptor.encrypt(newToken)).willReturn(encryptedToken);
            setupRestClientMock(tokenResponse);

            // when
            String result = kisAuthService.getAccessToken(testAccount.name());

            // then
            assertThat(result).isEqualTo(newToken);
            verify(kisRestClient).post();
            verify(valueOperations).set(eq(cacheKey), eq(encryptedToken), any(Duration.class));
        }

        @Test
        @DisplayName("계정명으로 계정 찾기 성공")
        void 계정명으로_계정_찾기_성공() {
            // given
            String cachedToken = "cached-token";
            String encryptedToken = "encrypted-cached-token";
            String hashedKey = "hashed-key";
            String cacheKey = REDIS_KEY_PREFIX + hashedKey;

            given(kisProperties.accounts()).willReturn(List.of(testAccount));
            given(redisKeyHasher.hash(testAccount.accountNumber())).willReturn(hashedKey);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(cacheKey)).willReturn(encryptedToken);
            given(tokenEncryptor.decrypt(encryptedToken)).willReturn(cachedToken);

            // when
            String result = kisAuthService.getAccessToken(testAccount.name());

            // then
            assertThat(result).isEqualTo(cachedToken);
        }

        @Test
        @DisplayName("계정번호로 계정 찾기 성공")
        void 계정번호로_계정_찾기_성공() {
            // given
            String cachedToken = "cached-token";
            String encryptedToken = "encrypted-cached-token";
            String hashedKey = "hashed-key";
            String cacheKey = REDIS_KEY_PREFIX + hashedKey;

            given(kisProperties.accounts()).willReturn(List.of(testAccount));
            given(redisKeyHasher.hash(testAccount.accountNumber())).willReturn(hashedKey);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(cacheKey)).willReturn(encryptedToken);
            given(tokenEncryptor.decrypt(encryptedToken)).willReturn(cachedToken);

            // when
            String result = kisAuthService.getAccessToken(testAccount.accountNumber());

            // then
            assertThat(result).isEqualTo(cachedToken);
        }

        @Test
        @DisplayName("계정 없으면 예외 발생")
        void 계정_없으면_예외_발생() {
            // given
            given(kisProperties.accounts()).willReturn(List.of(testAccount));

            // when & then
            assertThatThrownBy(() -> kisAuthService.getAccessToken("존재하지않는계정"))
                    .isInstanceOf(KisApiException.class)
                    .hasMessageContaining(ErrorCode.KIS_NO_ACCOUNT.getMessage());
        }
    }

    @Nested
    @DisplayName("getDefaultAccount 메소드")
    class GetDefaultAccount {

        @Test
        @DisplayName("계정이 있으면 첫 번째 계정 반환")
        void testGetDefaultAccount_Success() {
            // given
            var secondAccount = new KisAccountProperties("두번째계정", "87654321", "appKey456", "appSecret456");
            given(kisProperties.accounts()).willReturn(List.of(testAccount, secondAccount));

            // when
            var result = kisAuthService.getDefaultAccount();

            // then
            assertThat(result).isEqualTo(testAccount);
            assertThat(result.name()).isEqualTo("테스트계정");
        }

        @Test
        @DisplayName("계정이 null이면 예외 발생")
        void testGetDefaultAccount_NullAccounts() {
            // given
            given(kisProperties.accounts()).willReturn(null);

            // when & then
            assertThatThrownBy(() -> kisAuthService.getDefaultAccount())
                    .isInstanceOf(KisApiException.class)
                    .hasMessageContaining("No accounts configured");
        }

        @Test
        @DisplayName("계정이 빈 리스트면 예외 발생")
        void testGetDefaultAccount_EmptyAccounts() {
            // given
            given(kisProperties.accounts()).willReturn(Collections.emptyList());

            // when & then
            assertThatThrownBy(() -> kisAuthService.getDefaultAccount())
                    .isInstanceOf(KisApiException.class)
                    .hasMessageContaining("No accounts configured");
        }
    }

    @Nested
    @DisplayName("refreshToken 메소드 (private, getAccessToken 통해 테스트)")
    class RefreshToken {

        @Test
        @DisplayName("토큰 응답이 null이면 예외 발생")
        void 토큰_응답이_null이면_예외_발생() {
            // given
            String hashedKey = "hashed-key";
            String cacheKey = REDIS_KEY_PREFIX + hashedKey;

            given(kisProperties.accounts()).willReturn(List.of(testAccount));
            given(redisKeyHasher.hash(testAccount.accountNumber())).willReturn(hashedKey);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(cacheKey)).willReturn(null);
            setupRestClientMock(null);

            // when & then
            assertThatThrownBy(() -> kisAuthService.getAccessToken(testAccount.name()))
                    .isInstanceOf(KisApiException.class)
                    .hasMessageContaining(ErrorCode.KIS_AUTH_ERROR.getMessage());
        }

        @Test
        @DisplayName("토큰 응답의 accessToken이 null이면 예외 발생")
        void 토큰_응답의_accessToken이_null이면_예외_발생() {
            // given
            String hashedKey = "hashed-key";
            String cacheKey = REDIS_KEY_PREFIX + hashedKey;
            String expiryTime = LocalDateTime.now().plusHours(24).format(EXPIRY_FORMATTER);
            KisTokenResponse tokenResponse = new KisTokenResponse(null, expiryTime, "Bearer", 86400L);

            given(kisProperties.accounts()).willReturn(List.of(testAccount));
            given(redisKeyHasher.hash(testAccount.accountNumber())).willReturn(hashedKey);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(cacheKey)).willReturn(null);
            setupRestClientMock(tokenResponse);

            // when & then
            assertThatThrownBy(() -> kisAuthService.getAccessToken(testAccount.name()))
                    .isInstanceOf(KisApiException.class)
                    .hasMessageContaining(ErrorCode.KIS_AUTH_ERROR.getMessage());
        }

        @Test
        @DisplayName("TTL이 양수일 때 Redis에 저장")
        void TTL이_양수일때_Redis에_저장() {
            // given
            String newToken = "new-access-token";
            String encryptedToken = "encrypted-new-token";
            String hashedKey = "hashed-key";
            String cacheKey = REDIS_KEY_PREFIX + hashedKey;
            String expiryTime = LocalDateTime.now().plusHours(24).format(EXPIRY_FORMATTER);
            KisTokenResponse tokenResponse = new KisTokenResponse(newToken, expiryTime, "Bearer", 86400L);

            given(kisProperties.accounts()).willReturn(List.of(testAccount));
            given(redisKeyHasher.hash(testAccount.accountNumber())).willReturn(hashedKey);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(cacheKey)).willReturn(null);
            given(tokenEncryptor.encrypt(newToken)).willReturn(encryptedToken);
            setupRestClientMock(tokenResponse);

            // when
            String result = kisAuthService.getAccessToken(testAccount.name());

            // then
            assertThat(result).isEqualTo(newToken);
            verify(valueOperations).set(eq(cacheKey), eq(encryptedToken), any(Duration.class));
        }

        @Test
        @DisplayName("TTL이 음수일 때 Redis에 저장 안 함")
        void TTL이_음수일때_Redis에_저장안함() {
            // given
            String newToken = "new-access-token";
            String hashedKey = "hashed-key";
            String cacheKey = REDIS_KEY_PREFIX + hashedKey;
            String expiryTime = LocalDateTime.now().minusMinutes(10).format(EXPIRY_FORMATTER);
            KisTokenResponse tokenResponse = new KisTokenResponse(newToken, expiryTime, "Bearer", 0L);

            given(kisProperties.accounts()).willReturn(List.of(testAccount));
            given(redisKeyHasher.hash(testAccount.accountNumber())).willReturn(hashedKey);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(cacheKey)).willReturn(null);
            setupRestClientMock(tokenResponse);

            // when
            String result = kisAuthService.getAccessToken(testAccount.name());

            // then
            assertThat(result).isEqualTo(newToken);
            verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        }
    }

    private void setupRestClientMock(KisTokenResponse response) {
        given(kisRestClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
        given(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(KisTokenRequest.class))).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(KisTokenResponse.class)).willReturn(response);
    }

    @Nested
    @DisplayName("동시성 제어 테스트")
    class ConcurrencyControl {

        @Test
        @DisplayName("동일 계정에 대해 10개 스레드 동시 요청 시 API 호출은 1회만 발생")
        void shouldCallApiOnceWhen10ThreadsRequestSameAccountToken() throws InterruptedException {
            // given
            int threadCount = 10;
            String newToken = "new-access-token";
            String encryptedToken = "encrypted-new-token";
            String hashedKey = "hashed-key";
            String cacheKey = REDIS_KEY_PREFIX + hashedKey;
            String expiryTime = LocalDateTime.now().plusHours(24).format(EXPIRY_FORMATTER);
            KisTokenResponse tokenResponse = new KisTokenResponse(newToken, expiryTime, "Bearer", 86400L);

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);

            // In-memory cache simulation
            AtomicInteger apiCallCount = new AtomicInteger(0);
            ConcurrentHashMap<String, String> inMemoryCache = new ConcurrentHashMap<>();

            given(kisProperties.accounts()).willReturn(List.of(testAccount));
            given(redisKeyHasher.hash(testAccount.accountNumber())).willReturn(hashedKey);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // valueOperations.get(): InMemory Cache에서 조회
            given(valueOperations.get(cacheKey)).willAnswer(inv -> inMemoryCache.get(cacheKey));

            // valueOperations.set(): InMemory Cache에 저장
            willAnswer(inv -> {
                inMemoryCache.put(cacheKey, encryptedToken);
                return null;
            }).given(valueOperations).set(eq(cacheKey), eq(encryptedToken), any(Duration.class));

            given(tokenEncryptor.encrypt(newToken)).willReturn(encryptedToken);
            given(tokenEncryptor.decrypt(encryptedToken)).willReturn(newToken);

            // RestClient Mock 설정
            given(kisRestClient.post()).willReturn(requestBodyUriSpec);
            given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
            given(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).willReturn(requestBodySpec);
            given(requestBodySpec.body(any(KisTokenRequest.class))).willReturn(requestBodySpec);
            given(requestBodySpec.retrieve()).willAnswer(inv -> {
                apiCallCount.incrementAndGet();
                Thread.sleep(50);  // API 호출 시뮬레이션
                return responseSpec;
            });
            given(responseSpec.body(KisTokenResponse.class)).willReturn(tokenResponse);

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

            // when
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        String token = kisAuthService.getAccessToken(testAccount.name());
                        assertThat(token).isEqualTo(newToken);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean finished = endLatch.await(5, TimeUnit.SECONDS);
            executorService.shutdown();

            // then
            assertThat(finished).isTrue();
            assertThat(apiCallCount.get()).isEqualTo(1);  // API 호출은 정확히 1회
            verify(valueOperations, times(1)).set(eq(cacheKey), eq(encryptedToken), any(Duration.class));
        }

        @Test
        @DisplayName("서로 다른 3개 계정에 대해 각 3개 스레드씩 동시 요청 시 API 호출은 계정별 1회씩 총 3회 발생")
        void shouldCallApiThreeTimesWhenThreeAccountsRequestedConcurrently() throws InterruptedException {
            // given
            int accountCount = 3;
            int threadsPerAccount = 3;
            int totalThreads = accountCount * threadsPerAccount;

            KisAccountProperties account1 = new KisAccountProperties("계정1", "11111111", "key1", "secret1");
            KisAccountProperties account2 = new KisAccountProperties("계정2", "22222222", "key2", "secret2");
            KisAccountProperties account3 = new KisAccountProperties("계정3", "33333333", "key3", "secret3");

            String expiryTime = LocalDateTime.now().plusHours(24).format(EXPIRY_FORMATTER);
            KisTokenResponse response1 = new KisTokenResponse("token1", expiryTime, "Bearer", 86400L);
            KisTokenResponse response2 = new KisTokenResponse("token2", expiryTime, "Bearer", 86400L);
            KisTokenResponse response3 = new KisTokenResponse("token3", expiryTime, "Bearer", 86400L);

            AtomicInteger apiCallCount = new AtomicInteger(0);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(totalThreads);

            // In-memory cache simulation
            ConcurrentHashMap<String, String> inMemoryCache = new ConcurrentHashMap<>();

            given(kisProperties.accounts()).willReturn(List.of(account1, account2, account3));
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            given(redisKeyHasher.hash(account1.accountNumber())).willReturn("hash1");
            given(redisKeyHasher.hash(account2.accountNumber())).willReturn("hash2");
            given(redisKeyHasher.hash(account3.accountNumber())).willReturn("hash3");

            // valueOperations.get(): InMemory Cache에서 조회
            given(valueOperations.get(anyString())).willAnswer(inv -> {
                String key = inv.getArgument(0);
                return inMemoryCache.get(key);
            });

            // valueOperations.set(): InMemory Cache에 저장
            willAnswer(inv -> {
                String key = inv.getArgument(0);
                String value = inv.getArgument(1);
                inMemoryCache.put(key, value);
                return null;
            }).given(valueOperations).set(anyString(), anyString(), any(Duration.class));

            given(tokenEncryptor.encrypt("token1")).willReturn("encrypted-token1");
            given(tokenEncryptor.encrypt("token2")).willReturn("encrypted-token2");
            given(tokenEncryptor.encrypt("token3")).willReturn("encrypted-token3");

            given(tokenEncryptor.decrypt("encrypted-token1")).willReturn("token1");
            given(tokenEncryptor.decrypt("encrypted-token2")).willReturn("token2");
            given(tokenEncryptor.decrypt("encrypted-token3")).willReturn("token3");

            // RestClient Mock 설정
            given(kisRestClient.post()).willReturn(requestBodyUriSpec);
            given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
            given(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).willReturn(requestBodySpec);
            given(requestBodySpec.body(any(KisTokenRequest.class))).willReturn(requestBodySpec);
            given(requestBodySpec.retrieve()).willAnswer(invocation -> {
                apiCallCount.incrementAndGet();
                Thread.sleep(50);
                return responseSpec;
            });
            given(responseSpec.body(KisTokenResponse.class))
                    .willReturn(response1)
                    .willReturn(response2)
                    .willReturn(response3);

            ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);

            // when
            List<KisAccountProperties> accounts = List.of(account1, account2, account3);
            for (KisAccountProperties account : accounts) {
                for (int i = 0; i < threadsPerAccount; i++) {
                    executorService.submit(() -> {
                        try {
                            startLatch.await();
                            kisAuthService.getAccessToken(account.name());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            endLatch.countDown();
                        }
                    });
                }
            }

            startLatch.countDown();
            boolean finished = endLatch.await(5, TimeUnit.SECONDS);

            executorService.shutdown();

            // then
            assertThat(finished).isTrue();
            assertThat(apiCallCount.get()).isEqualTo(3);  // 계정별 1회씩 총 3회
        }

        @Test
        @DisplayName("Lock 획득 후 Redis 캐시 재확인하여 중복 API 호출 방지 (Double-Checked Locking)")
        void shouldRecheckRedisCacheAfterAcquiringLock() throws InterruptedException {
            // given
            String newToken = "new-access-token";
            String encryptedToken = "encrypted-new-token";
            String hashedKey = "hashed-key";
            String cacheKey = REDIS_KEY_PREFIX + hashedKey;

            CountDownLatch firstThreadLatch = new CountDownLatch(1);
            CountDownLatch secondThreadLatch = new CountDownLatch(1);

            given(kisProperties.accounts()).willReturn(List.of(testAccount));
            given(redisKeyHasher.hash(testAccount.accountNumber())).willReturn(hashedKey);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(tokenEncryptor.decrypt(encryptedToken)).willReturn(newToken);

            AtomicInteger apiCallCount = new AtomicInteger(0);

            // 첫 번째 스레드: Lock 획득 전/후 모두 null → API 호출
            // 두 번째 스레드: Lock 획득 전 null, Lock 획득 후 캐시 히트 → API 호출 안 함
            given(valueOperations.get(cacheKey))
                    .willAnswer(invocation -> {
                        // 첫 번째 스레드의 Lock 획득 전 조회
                        return null;
                    })
                    .willAnswer(invocation -> {
                        // 첫 번째 스레드의 Lock 획득 후 재조회
                        firstThreadLatch.countDown();  // 첫 번째 스레드가 Lock 획득했음을 알림
                        return null;
                    })
                    .willAnswer(invocation -> {
                        // 두 번째 스레드의 Lock 획득 전 조회
                        return null;
                    })
                    .willAnswer(invocation -> {
                        // 두 번째 스레드의 Lock 획득 후 재조회 (첫 번째 스레드가 저장한 토큰)
                        return encryptedToken;
                    });

            String expiryTime = LocalDateTime.now().plusHours(24).format(EXPIRY_FORMATTER);
            KisTokenResponse tokenResponse = new KisTokenResponse(newToken, expiryTime, "Bearer", 86400L);

            given(kisRestClient.post()).willReturn(requestBodyUriSpec);
            given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
            given(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).willReturn(requestBodySpec);
            given(requestBodySpec.body(any(KisTokenRequest.class))).willReturn(requestBodySpec);
            given(requestBodySpec.retrieve()).willAnswer(invocation -> {
                apiCallCount.incrementAndGet();
                Thread.sleep(100);  // API 호출 시뮬레이션
                secondThreadLatch.countDown();  // API 호출 완료 알림
                return responseSpec;
            });
            given(responseSpec.body(KisTokenResponse.class)).willReturn(tokenResponse);
            given(tokenEncryptor.encrypt(newToken)).willReturn(encryptedToken);

            ExecutorService executorService = Executors.newFixedThreadPool(2);

            // when
            // 첫 번째 스레드: API 호출
            executorService.submit(() -> {
                try {
                    kisAuthService.getAccessToken(testAccount.name());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            firstThreadLatch.await();  // 첫 번째 스레드가 Lock을 획득할 때까지 대기

            // 두 번째 스레드: Lock 대기 후 Redis 재확인하여 캐시된 값 사용
            executorService.submit(() -> {
                try {
                    secondThreadLatch.await();  // 첫 번째 스레드의 API 호출이 완료될 때까지 대기
                    String token = kisAuthService.getAccessToken(testAccount.name());
                    assertThat(token).isEqualTo(newToken);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            executorService.shutdown();
            boolean finished = executorService.awaitTermination(5, TimeUnit.SECONDS);

            // then
            assertThat(finished).isTrue();
            assertThat(apiCallCount.get()).isEqualTo(1);  // API 호출은 1회만
        }

        @Test
        @DisplayName("API 호출 중 예외 발생 시 Lock이 정상적으로 해제되어 다음 요청 처리 가능")
        void shouldReleaseLockWhenExceptionOccursDuringApiCall() throws InterruptedException {
            // given
            String hashedKey = "hashed-key";
            String cacheKey = REDIS_KEY_PREFIX + hashedKey;

            given(kisProperties.accounts()).willReturn(List.of(testAccount));
            given(redisKeyHasher.hash(testAccount.accountNumber())).willReturn(hashedKey);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(cacheKey)).willReturn(null);

            AtomicInteger attemptCount = new AtomicInteger(0);

            String expiryTime = LocalDateTime.now().plusHours(24).format(EXPIRY_FORMATTER);
            KisTokenResponse tokenResponse = new KisTokenResponse("new-token", expiryTime, "Bearer", 86400L);

            given(kisRestClient.post()).willReturn(requestBodyUriSpec);
            given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
            given(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).willReturn(requestBodySpec);
            given(requestBodySpec.body(any(KisTokenRequest.class))).willReturn(requestBodySpec);

            // 첫 번째 시도: 예외 발생
            // 두 번째 시도: 정상 응답
            given(requestBodySpec.retrieve()).willAnswer(invocation -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt == 1) {
                    throw new RuntimeException("API 호출 실패");
                }
                return responseSpec;
            });

            given(responseSpec.body(KisTokenResponse.class)).willReturn(tokenResponse);
            given(tokenEncryptor.encrypt("new-token")).willReturn("encrypted-new-token");

            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CountDownLatch firstAttemptLatch = new CountDownLatch(1);
            CountDownLatch secondAttemptLatch = new CountDownLatch(1);

            // when
            // 첫 번째 스레드: 예외 발생
            executorService.submit(() -> {
                try {
                    kisAuthService.getAccessToken(testAccount.name());
                } catch (Exception e) {
                    // 예외 발생 예상
                    assertThat(e).hasMessageContaining("API 호출 실패");
                } finally {
                    firstAttemptLatch.countDown();
                }
            });

            firstAttemptLatch.await();  // 첫 번째 스레드 완료 대기

            // 두 번째 스레드: Lock이 해제되어 정상 처리되어야 함
            executorService.submit(() -> {
                try {
                    String token = kisAuthService.getAccessToken(testAccount.name());
                    assertThat(token).isEqualTo("new-token");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    secondAttemptLatch.countDown();
                }
            });

            boolean finished = secondAttemptLatch.await(5, TimeUnit.SECONDS);

            executorService.shutdown();

            // then
            assertThat(finished).isTrue();  // 두 번째 요청이 Lock 대기 없이 처리됨
            assertThat(attemptCount.get()).isEqualTo(2);  // 총 2회 시도
        }
    }
}
