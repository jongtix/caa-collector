package com.custom.trader.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.custom.trader.common.constant.DateFormatConstants.KST_ZONE_ID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiResponse 단위 테스트.
 *
 * <p>REST API 응답 DTO의 생성 및 속성을 검증합니다.</p>
 */
@DisplayName("ApiResponse 단위 테스트")
class ApiResponseTest {

    @Test
    @DisplayName("success(data): 성공 응답 생성 (메시지 없음)")
    void success_WithDataOnly() {
        // Given
        String data = "테스트 데이터";

        // When
        ApiResponse<String> response = ApiResponse.success(data);

        // Then
        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(data);
        assertThat(response.message()).isNull();
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.timestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("success(data, message): 성공 응답 생성 (메시지 포함)")
    void success_WithDataAndMessage() {
        // Given
        Integer data = 42;
        String message = "처리 완료";

        // When
        ApiResponse<Integer> response = ApiResponse.success(data, message);

        // Then
        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(data);
        assertThat(response.message()).isEqualTo(message);
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("success(null): null 데이터도 성공 응답 가능")
    void success_WithNullData() {
        // When
        ApiResponse<Void> response = ApiResponse.success(null);

        // Then
        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNull();
        assertThat(response.message()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("fail(message): 실패 응답 생성")
    void fail_WithMessage() {
        // Given
        String errorMessage = "처리 실패";

        // When
        ApiResponse<Void> response = ApiResponse.fail(errorMessage);

        // Then
        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.message()).isEqualTo(errorMessage);
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("타임스탬프: Asia/Seoul 타임존 사용")
    void timestamp_UsesSeoulTimezone() {
        // When
        ApiResponse<String> response = ApiResponse.success("test");

        // Then
        LocalDateTime now = LocalDateTime.now(KST_ZONE_ID);
        assertThat(response.timestamp())
            .isAfter(now.minusSeconds(1))
            .isBefore(now.plusSeconds(1));
    }

    @Test
    @DisplayName("Record 동등성: 동일한 값은 동일한 객체")
    void record_Equality() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        ApiResponse<String> response1 = new ApiResponse<>(true, "data", "msg", timestamp);
        ApiResponse<String> response2 = new ApiResponse<>(true, "data", "msg", timestamp);

        // Then
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    @DisplayName("Record toString(): 모든 필드 포함")
    void record_ToString() {
        // Given
        ApiResponse<String> response = ApiResponse.success("test", "성공");

        // Then
        String toString = response.toString();
        assertThat(toString)
            .contains("success=true")
            .contains("data=test")
            .contains("message=성공")
            .contains("timestamp=");
    }
}
