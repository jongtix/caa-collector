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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
}
