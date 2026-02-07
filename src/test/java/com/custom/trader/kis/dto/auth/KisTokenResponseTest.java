package com.custom.trader.kis.dto.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KisTokenResponse 테스트")
class KisTokenResponseTest {

    @Nested
    @DisplayName("toString 마스킹 테스트")
    class ToStringMaskingTest {

        @Test
        @DisplayName("accessToken이 마스킹되어 출력")
        void toString_masksAccessToken() {
            // given
            String realToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature";
            KisTokenResponse response = new KisTokenResponse(
                    realToken, "2026-02-07 12:00:00", "Bearer", 86400L
            );

            // when
            String result = response.toString();

            // then
            assertThat(result).doesNotContain(realToken);
            assertThat(result).contains("eyJh****************");
            assertThat(result).contains("accessTokenTokenExpired=2026-02-07 12:00:00");
            assertThat(result).contains("tokenType=Bearer");
            assertThat(result).contains("expiresIn=86400");
        }

        @Test
        @DisplayName("accessToken이 null일 때 안전하게 마스킹")
        void toString_handlesNullAccessToken() {
            // given
            KisTokenResponse response = new KisTokenResponse(
                    null, "2026-02-07 12:00:00", "Bearer", 86400L
            );

            // when
            String result = response.toString();

            // then
            assertThat(result).contains("accessToken=***");
            assertThat(result).doesNotContain("null");
        }

        @Test
        @DisplayName("toString에 원본 토큰의 5번째 문자 이후가 포함되지 않음")
        void toString_doesNotLeakTokenBeyondPrefix() {
            // given
            String realToken = "eyJhSECRET_PAYLOAD_SHOULD_NOT_APPEAR_IN_LOG";
            KisTokenResponse response = new KisTokenResponse(
                    realToken, "2026-02-07 12:00:00", "Bearer", 86400L
            );

            // when
            String result = response.toString();

            // then
            assertThat(result).doesNotContain("SECRET_PAYLOAD_SHOULD_NOT_APPEAR_IN_LOG");
            assertThat(result).doesNotContain(realToken);
        }

        @Test
        @DisplayName("Record의 접근자는 마스킹 없이 원본 반환")
        void accessors_returnOriginalValues() {
            // given
            String realToken = "eyJhbGciOiJIUzI1NiJ9";
            KisTokenResponse response = new KisTokenResponse(
                    realToken, "2026-02-07 12:00:00", "Bearer", 86400L
            );

            // when & then
            assertThat(response.accessToken()).isEqualTo(realToken);
            assertThat(response.accessTokenTokenExpired()).isEqualTo("2026-02-07 12:00:00");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(86400L);
        }

        @Test
        @DisplayName("equals/hashCode는 마스킹 영향 없이 동작")
        void equalsAndHashCode_notAffectedByMasking() {
            // given
            KisTokenResponse response1 = new KisTokenResponse(
                    "token123", "2026-02-07 12:00:00", "Bearer", 86400L
            );
            KisTokenResponse response2 = new KisTokenResponse(
                    "token123", "2026-02-07 12:00:00", "Bearer", 86400L
            );

            // when & then
            assertThat(response1).isEqualTo(response2);
            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }
    }
}
