package com.custom.trader.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenEncryptor")
class TokenEncryptorTest {

    // Base64 인코딩된 32바이트 키: "testAes256KeyForJunitTests!12345"
    private static final String VALID_KEY = "dGVzdEFlczI1NktleUZvckp1bml0VGVzdHMhMTIzNDU=";

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Test
        @DisplayName("유효한 Base64 키로 정상 생성")
        void 유효한_Base64_키로_정상_생성() {
            new TokenEncryptor(VALID_KEY);
        }

        @Test
        @DisplayName("null 키로 생성 시 예외 발생")
        void null_키로_생성시_예외_발생() {
            assertThatThrownBy(() -> new TokenEncryptor(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or blank");
        }

        @Test
        @DisplayName("빈 키로 생성 시 예외 발생")
        void 빈_키로_생성시_예외_발생() {
            assertThatThrownBy(() -> new TokenEncryptor("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or blank");
        }

        @Test
        @DisplayName("32바이트가 아닌 키로 생성 시 예외 발생")
        void 잘못된_길이_키로_생성시_예외_발생() {
            // 16바이트 키 (Base64)
            String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

            assertThatThrownBy(() -> new TokenEncryptor(shortKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("AES-256 key must be exactly 32 bytes");
        }
    }

    @Nested
    @DisplayName("encrypt/decrypt 라운드트립")
    class EncryptDecrypt {

        private final TokenEncryptor encryptor = new TokenEncryptor(VALID_KEY);

        @Test
        @DisplayName("암호화 후 복호화하면 원본 반환")
        void 암호화_후_복호화하면_원본_반환() {
            // given
            String plainText = "eyJhbGciOiJIUzI1NiJ9.test-access-token";

            // when
            String encrypted = encryptor.encrypt(plainText);
            String decrypted = encryptor.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(plainText);
        }

        @Test
        @DisplayName("동일 평문의 암호화 결과는 매번 다름 (IV 무작위)")
        void 동일_평문의_암호화_결과는_매번_다름() {
            // given
            String plainText = "same-token-value";

            // when
            String encrypted1 = encryptor.encrypt(plainText);
            String encrypted2 = encryptor.encrypt(plainText);

            // then
            assertThat(encrypted1).isNotEqualTo(encrypted2);

            // 복호화 결과는 동일
            assertThat(encryptor.decrypt(encrypted1)).isEqualTo(plainText);
            assertThat(encryptor.decrypt(encrypted2)).isEqualTo(plainText);
        }

        @Test
        @DisplayName("암호화된 값은 Base64 형식")
        void 암호화된_값은_Base64_형식() {
            // given & when
            String encrypted = encryptor.encrypt("test-token");

            // then
            assertThat(encrypted).matches("[A-Za-z0-9+/=]+");
        }

        @Test
        @DisplayName("긴 토큰도 정상 처리")
        void 긴_토큰도_정상_처리() {
            // given
            String longToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
                    "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

            // when
            String encrypted = encryptor.encrypt(longToken);
            String decrypted = encryptor.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(longToken);
        }
    }

    @Nested
    @DisplayName("encrypt 예외 처리")
    class EncryptExceptions {

        private final TokenEncryptor encryptor = new TokenEncryptor(VALID_KEY);

        @Test
        @DisplayName("null 평문 암호화 시 예외 발생")
        void null_평문_암호화시_예외_발생() {
            assertThatThrownBy(() -> encryptor.encrypt(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plain text must not be null or empty");
        }

        @Test
        @DisplayName("빈 평문 암호화 시 예외 발생")
        void 빈_평문_암호화시_예외_발생() {
            assertThatThrownBy(() -> encryptor.encrypt(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plain text must not be null or empty");
        }
    }

    @Nested
    @DisplayName("decrypt 예외 처리")
    class DecryptExceptions {

        private final TokenEncryptor encryptor = new TokenEncryptor(VALID_KEY);

        @Test
        @DisplayName("null 암호문 복호화 시 예외 발생")
        void null_암호문_복호화시_예외_발생() {
            assertThatThrownBy(() -> encryptor.decrypt(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cipher text must not be null or empty");
        }

        @Test
        @DisplayName("빈 암호문 복호화 시 예외 발생")
        void 빈_암호문_복호화시_예외_발생() {
            assertThatThrownBy(() -> encryptor.decrypt(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cipher text must not be null or empty");
        }

        @Test
        @DisplayName("짧은 암호문 복호화 시 예외 발생")
        void 짧은_암호문_복호화시_예외_발생() {
            // 12바이트(IV) 미만의 데이터
            String shortCipher = Base64.getEncoder().encodeToString(new byte[5]);

            assertThatThrownBy(() -> encryptor.decrypt(shortCipher))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("too short");
        }

        @Test
        @DisplayName("변조된 암호문 복호화 시 예외 발생 (GCM 인증 실패)")
        void 변조된_암호문_복호화시_예외_발생() {
            // given
            String encrypted = encryptor.encrypt("original-token");
            byte[] bytes = Base64.getDecoder().decode(encrypted);

            // 마지막 바이트 변조 (GCM 태그 영역)
            bytes[bytes.length - 1] ^= 0xFF;
            String tampered = Base64.getEncoder().encodeToString(bytes);

            // when & then
            assertThatThrownBy(() -> encryptor.decrypt(tampered))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Token decryption failed");
        }

        @Test
        @DisplayName("다른 키로 암호화된 값은 복호화 실패")
        void 다른_키로_암호화된_값은_복호화_실패() {
            // given
            byte[] otherKeyBytes = new byte[32];
            otherKeyBytes[0] = 1; // 다른 키
            String otherKey = Base64.getEncoder().encodeToString(otherKeyBytes);
            TokenEncryptor otherEncryptor = new TokenEncryptor(otherKey);

            String encrypted = otherEncryptor.encrypt("secret-token");

            // when & then
            assertThatThrownBy(() -> encryptor.decrypt(encrypted))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Token decryption failed");
        }
    }
}
