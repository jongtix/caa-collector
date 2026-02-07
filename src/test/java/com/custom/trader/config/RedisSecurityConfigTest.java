package com.custom.trader.config;

import com.custom.trader.common.util.RedisKeyHasher;
import com.custom.trader.common.util.TokenEncryptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RedisSecurityConfig의 Bean 생성 예외 처리 테스트.
 *
 * <p>잘못된 환경변수(null, 빈 문자열, 짧은 키 등)에 대해
 * 애플리케이션 시작 시점에 실패하는지 검증한다.</p>
 *
 * @since 2026-02-03
 */
@DisplayName("RedisSecurityConfig Bean 생성 검증")
class RedisSecurityConfigTest {

    @Nested
    @DisplayName("RedisKeyHasher Bean 생성 검증")
    class RedisKeyHasherBeanCreationTest {

        @Test
        @DisplayName("HMAC secret이 null이면 Bean 생성 실패")
        void testRedisKeyHasher_NullSecret() {
            // Given
            String nullSecret = null;

            // When & Then
            assertThatThrownBy(() -> new RedisKeyHasher(nullSecret))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("HMAC secret must not be null or blank");
        }

        @Test
        @DisplayName("HMAC secret이 빈 문자열이면 Bean 생성 실패")
        void testRedisKeyHasher_EmptySecret() {
            // Given
            String emptySecret = "";

            // When & Then
            assertThatThrownBy(() -> new RedisKeyHasher(emptySecret))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("HMAC secret must not be null or blank");
        }

        @Test
        @DisplayName("HMAC secret이 공백 문자열이면 Bean 생성 실패")
        void testRedisKeyHasher_BlankSecret() {
            // Given
            String blankSecret = "   ";

            // When & Then
            assertThatThrownBy(() -> new RedisKeyHasher(blankSecret))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("HMAC secret must not be null or blank");
        }

        @Test
        @DisplayName("HMAC secret이 32자 미만이면 Bean 생성 실패")
        void testRedisKeyHasher_ShortSecret() {
            // Given
            String shortSecret = "too-short-secret-key";  // 20자

            // When & Then
            assertThatThrownBy(() -> new RedisKeyHasher(shortSecret))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("HMAC secret must be at least 32 characters")
                    .hasMessageContaining("Current length: 20");
        }

        @Test
        @DisplayName("HMAC secret이 정확히 31자이면 Bean 생성 실패")
        void testRedisKeyHasher_ExactlyOneCharShort() {
            // Given
            String secret31Chars = "1234567890123456789012345678901";  // 31자

            // When & Then
            assertThatThrownBy(() -> new RedisKeyHasher(secret31Chars))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("HMAC secret must be at least 32 characters")
                    .hasMessageContaining("Current length: 31");
        }

        @Test
        @DisplayName("HMAC secret이 정확히 32자이면 Bean 생성 성공")
        void testRedisKeyHasher_ExactlyMinimumLength() {
            // Given
            String secret32Chars = "12345678901234567890123456789012";  // 정확히 32자

            // When
            RedisKeyHasher hasher = new RedisKeyHasher(secret32Chars);

            // Then
            assertThat(hasher).isNotNull();
            assertThat(hasher.hash("test-account")).isNotNull();
        }

        @Test
        @DisplayName("HMAC secret이 32자 초과이면 Bean 생성 성공")
        void testRedisKeyHasher_LongSecret() {
            // Given
            String longSecret = "this-is-a-very-long-secret-key-for-hmac-sha256-hashing";  // 58자

            // When
            RedisKeyHasher hasher = new RedisKeyHasher(longSecret);

            // Then
            assertThat(hasher).isNotNull();
            assertThat(hasher.hash("test-account")).isNotNull();
        }
    }

    @Nested
    @DisplayName("TokenEncryptor Bean 생성 검증")
    class TokenEncryptorBeanCreationTest {

        @Test
        @DisplayName("Encryption key가 null이면 Bean 생성 실패")
        void testTokenEncryptor_NullKey() {
            // Given
            String nullKey = null;

            // When & Then
            assertThatThrownBy(() -> new TokenEncryptor(nullKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Encryption key must not be null or blank");
        }

        @Test
        @DisplayName("Encryption key가 빈 문자열이면 Bean 생성 실패")
        void testTokenEncryptor_EmptyKey() {
            // Given
            String emptyKey = "";

            // When & Then
            assertThatThrownBy(() -> new TokenEncryptor(emptyKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Encryption key must not be null or blank");
        }

        @Test
        @DisplayName("Encryption key가 공백 문자열이면 Bean 생성 실패")
        void testTokenEncryptor_BlankKey() {
            // Given
            String blankKey = "   ";

            // When & Then
            assertThatThrownBy(() -> new TokenEncryptor(blankKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Encryption key must not be null or blank");
        }

        @Test
        @DisplayName("Encryption key가 유효하지 않은 Base64이면 Bean 생성 실패")
        void testTokenEncryptor_InvalidBase64() {
            // Given
            String invalidBase64 = "this-is-not-valid-base64!@#$%";

            // When & Then
            assertThatThrownBy(() -> new TokenEncryptor(invalidBase64))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Base64 디코딩 후 16바이트이면 Bean 생성 실패")
        void testTokenEncryptor_16ByteKey() {
            // Given
            byte[] bytes16 = new byte[16];  // AES-128 크기
            String base64Key = Base64.getEncoder().encodeToString(bytes16);

            // When & Then
            assertThatThrownBy(() -> new TokenEncryptor(base64Key))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("AES-256 key must be exactly 32 bytes");
        }

        @Test
        @DisplayName("Base64 디코딩 후 24바이트이면 Bean 생성 실패")
        void testTokenEncryptor_24ByteKey() {
            // Given
            byte[] bytes24 = new byte[24];  // AES-192 크기
            String base64Key = Base64.getEncoder().encodeToString(bytes24);

            // When & Then
            assertThatThrownBy(() -> new TokenEncryptor(base64Key))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("AES-256 key must be exactly 32 bytes");
        }

        @Test
        @DisplayName("Base64 디코딩 후 31바이트이면 Bean 생성 실패")
        void testTokenEncryptor_31ByteKey() {
            // Given
            byte[] bytes31 = new byte[31];  // 1바이트 부족
            String base64Key = Base64.getEncoder().encodeToString(bytes31);

            // When & Then
            assertThatThrownBy(() -> new TokenEncryptor(base64Key))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("AES-256 key must be exactly 32 bytes");
        }

        @Test
        @DisplayName("Base64 디코딩 후 33바이트이면 Bean 생성 실패")
        void testTokenEncryptor_33ByteKey() {
            // Given
            byte[] bytes33 = new byte[33];  // 1바이트 초과
            String base64Key = Base64.getEncoder().encodeToString(bytes33);

            // When & Then
            assertThatThrownBy(() -> new TokenEncryptor(base64Key))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("AES-256 key must be exactly 32 bytes");
        }

        @Test
        @DisplayName("Base64 디코딩 후 정확히 32바이트이면 Bean 생성 성공")
        void testTokenEncryptor_Exactly32ByteKey() {
            // Given
            byte[] bytes32 = new byte[32];  // AES-256 크기
            for (int i = 0; i < 32; i++) {
                bytes32[i] = (byte) i;
            }
            String base64Key = Base64.getEncoder().encodeToString(bytes32);

            // When
            TokenEncryptor encryptor = new TokenEncryptor(base64Key);

            // Then
            assertThat(encryptor).isNotNull();
            String encrypted = encryptor.encrypt("test-token");
            assertThat(encrypted).isNotNull();
            assertThat(encryptor.decrypt(encrypted)).isEqualTo("test-token");
        }

        @Test
        @DisplayName("공백을 포함한 Base64 문자열이면 Bean 생성 실패")
        void testTokenEncryptor_Base64WithWhitespace() {
            // Given
            byte[] bytes32 = new byte[32];
            String validBase64 = Base64.getEncoder().encodeToString(bytes32);
            String base64WithSpaces = validBase64.substring(0, 10) + " " + validBase64.substring(10);

            // When & Then
            assertThatThrownBy(() -> new TokenEncryptor(base64WithSpaces))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("RedisSecurityConfig 통합 테스트")
    class RedisSecurityConfigIntegrationTest {

        @Test
        @DisplayName("유효한 설정값으로 모든 Bean 생성 성공")
        void testRedisSecurityConfig_ValidConfiguration() {
            // Given
            RedisSecurityConfig config = new RedisSecurityConfig();
            String validHmacSecret = "this-is-a-valid-hmac-secret-key-with-32-chars-or-more";
            byte[] bytes32 = new byte[32];
            for (int i = 0; i < 32; i++) {
                bytes32[i] = (byte) i;
            }
            String validEncryptionKey = Base64.getEncoder().encodeToString(bytes32);

            // When
            RedisKeyHasher hasher = config.redisKeyHasher(validHmacSecret);
            TokenEncryptor encryptor = config.tokenEncryptor(validEncryptionKey);

            // Then
            assertThat(hasher).isNotNull();
            assertThat(encryptor).isNotNull();

            // Bean이 정상 동작하는지 검증
            String hashedKey = hasher.hash("12345678-01");
            assertThat(hashedKey).isNotNull().hasSize(16);

            String encrypted = encryptor.encrypt("test-access-token");
            assertThat(encrypted).isNotNull();
            assertThat(encryptor.decrypt(encrypted)).isEqualTo("test-access-token");
        }
    }
}
