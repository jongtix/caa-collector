package com.custom.trader.kis.exception;

import com.custom.trader.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KisApiException 단위 테스트.
 *
 * <p>한국투자증권 API 예외 처리 로직을 검증합니다.</p>
 */
@DisplayName("KisApiException 단위 테스트")
class KisApiExceptionTest {

    @Test
    @DisplayName("생성자(String): 메시지만 전달 시 KIS_API_ERROR 코드 사용")
    void constructor_MessageOnly() {
        // Given
        String message = "API 호출 실패";

        // When
        KisApiException exception = new KisApiException(message);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.KIS_API_ERROR);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("생성자(ErrorCode): ErrorCode만 전달 시 기본 메시지 사용")
    void constructor_ErrorCodeOnly() {
        // Given
        ErrorCode errorCode = ErrorCode.KIS_AUTH_ERROR;

        // When
        KisApiException exception = new KisApiException(errorCode);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("생성자(ErrorCode, String): 커스텀 ErrorCode와 메시지 사용")
    void constructor_ErrorCodeAndMessage() {
        // Given
        ErrorCode errorCode = ErrorCode.KIS_NO_ACCOUNT;
        String customMessage = "계좌 번호가 설정되지 않았습니다.";

        // When
        KisApiException exception = new KisApiException(errorCode, customMessage);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }

    @Test
    @DisplayName("예외 던지기: 메시지 전파 확인")
    void throwException_WithMessage() {
        // Given
        String message = "토큰 갱신 실패";

        try {
            // When
            throw new KisApiException(message);
        } catch (KisApiException e) {
            // Then
            assertThat(e)
                .isInstanceOf(KisApiException.class)
                .hasMessage(message);
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.KIS_API_ERROR);
        }
    }

    @Test
    @DisplayName("예외 던지기: ErrorCode 전파 확인")
    void throwException_WithErrorCode() {
        // Given
        ErrorCode errorCode = ErrorCode.KIS_AUTH_ERROR;

        try {
            // When
            throw new KisApiException(errorCode);
        } catch (KisApiException e) {
            // Then
            assertThat(e)
                .isInstanceOf(KisApiException.class)
                .hasMessage(errorCode.getMessage());
            assertThat(e.getErrorCode()).isEqualTo(errorCode);
        }
    }

    @Test
    @DisplayName("예외 던지기: ErrorCode와 커스텀 메시지 전파")
    void throwException_WithErrorCodeAndMessage() {
        // Given
        ErrorCode errorCode = ErrorCode.KIS_NO_ACCOUNT;
        String customMessage = "계좌를 찾을 수 없습니다.";

        try {
            // When
            throw new KisApiException(errorCode, customMessage);
        } catch (KisApiException e) {
            // Then
            assertThat(e)
                .isInstanceOf(KisApiException.class)
                .hasMessage(customMessage);
            assertThat(e.getErrorCode()).isEqualTo(errorCode);
        }
    }

    @Test
    @DisplayName("BusinessException 상속 확인")
    void extendsBusinessException() {
        // Given
        KisApiException exception = new KisApiException("테스트");

        // Then
        assertThat(exception).isInstanceOf(com.custom.trader.common.exception.BusinessException.class);
    }

    @Test
    @DisplayName("RuntimeException 상속 확인")
    void extendsRuntimeException() {
        // Given
        KisApiException exception = new KisApiException("테스트");

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
