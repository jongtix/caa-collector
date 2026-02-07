package com.custom.trader.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RedisKeyHasher")
class RedisKeyHasherTest {

    private static final String VALID_SECRET = "test-hmac-secret-for-junit-tests-minimum-32-characters-long";

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Test
        @DisplayName("유효한 비밀키로 정상 생성")
        void 유효한_비밀키로_정상_생성() {
            // given & when & then
            new RedisKeyHasher(VALID_SECRET);
        }

        @Test
        @DisplayName("null 비밀키로 생성 시 예외 발생")
        void null_비밀키로_생성시_예외_발생() {
            assertThatThrownBy(() -> new RedisKeyHasher(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or blank");
        }

        @Test
        @DisplayName("빈 비밀키로 생성 시 예외 발생")
        void 빈_비밀키로_생성시_예외_발생() {
            assertThatThrownBy(() -> new RedisKeyHasher("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or blank");
        }

        @Test
        @DisplayName("HMAC 비밀키가 32자 미만이면 예외 발생")
        void hmacSecret_32자미만_예외발생() {
            assertThatThrownBy(() -> new RedisKeyHasher("short-key-less-than-32-chars"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be at least 32 characters");
        }

        @Test
        @DisplayName("HMAC 비밀키가 정확히 32자이면 정상 생성")
        void hmacSecret_32자_정상생성() {
            assertThatNoException().isThrownBy(() ->
                    new RedisKeyHasher("12345678901234567890123456789012") // 32자
            );
        }
    }

    @Nested
    @DisplayName("hash 메서드")
    class Hash {

        private final RedisKeyHasher hasher = new RedisKeyHasher(VALID_SECRET);

        @Test
        @DisplayName("동일 입력에 대해 동일 해시 반환 (결정적)")
        void 동일_입력에_대해_동일_해시_반환() {
            // given
            String accountNumber = "12345678-01";

            // when
            String hash1 = hasher.hash(accountNumber);
            String hash2 = hasher.hash(accountNumber);

            // then
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("해시 결과는 16자 hex 문자열")
        void 해시_결과는_16자_hex_문자열() {
            // given & when
            String hash = hasher.hash("12345678-01");

            // then
            assertThat(hash).hasSize(16);
            assertThat(hash).matches("[0-9a-f]{16}");
        }

        @Test
        @DisplayName("서로 다른 입력은 서로 다른 해시 반환")
        void 서로_다른_입력은_서로_다른_해시_반환() {
            // given & when
            String hash1 = hasher.hash("12345678-01");
            String hash2 = hasher.hash("87654321-01");

            // then
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("서로 다른 비밀키는 서로 다른 해시 반환")
        void 서로_다른_비밀키는_서로_다른_해시_반환() {
            // given
            RedisKeyHasher otherHasher = new RedisKeyHasher("another-secret-key-at-least-32-characters-long!");
            String accountNumber = "12345678-01";

            // when
            String hash1 = hasher.hash(accountNumber);
            String hash2 = otherHasher.hash(accountNumber);

            // then
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("null 입력 시 예외 발생")
        void null_입력시_예외_발생() {
            assertThatThrownBy(() -> hasher.hash(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account number must not be null or blank");
        }

        @Test
        @DisplayName("빈 문자열 입력 시 예외 발생")
        void 빈_문자열_입력시_예외_발생() {
            assertThatThrownBy(() -> hasher.hash("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account number must not be null or blank");
        }
    }
}
