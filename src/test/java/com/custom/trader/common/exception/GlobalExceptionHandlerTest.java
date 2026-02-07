package com.custom.trader.common.exception;

import com.custom.trader.common.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {GlobalExceptionHandlerTest.TestController.class},
        excludeAutoConfiguration = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("BusinessException 처리")
    class BusinessExceptionHandling {

        @Test
        @DisplayName("BAD_REQUEST 예외 발생시 400 응답")
        void BAD_REQUEST_예외_발생시_400_응답() throws Exception {
            // when & then
            mockMvc.perform(get("/test/business/bad-request"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT_VALUE.getMessage()))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("UNAUTHORIZED 예외 발생시 401 응답")
        void UNAUTHORIZED_예외_발생시_401_응답() throws Exception {
            // when & then
            mockMvc.perform(get("/test/business/unauthorized"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.KIS_AUTH_ERROR.getMessage()))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("BAD_GATEWAY 예외 발생시 502 응답")
        void BAD_GATEWAY_예외_발생시_502_응답() throws Exception {
            // when & then
            mockMvc.perform(get("/test/business/bad-gateway"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.KIS_API_ERROR.getMessage()))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("INTERNAL_SERVER_ERROR 예외 발생시 500 응답")
        void INTERNAL_SERVER_ERROR_예외_발생시_500_응답() throws Exception {
            // when & then
            mockMvc.perform(get("/test/business/internal-error"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("Validation 예외 처리")
    class ValidationExceptionHandling {

        @Test
        @DisplayName("MethodArgumentNotValidException 핸들러 메소드 존재 확인")
        void MethodArgumentNotValidException_핸들러_메소드_존재() throws NoSuchMethodException {
            // when
            var method = GlobalExceptionHandler.class.getMethod(
                    "handleValidationException",
                    MethodArgumentNotValidException.class
            );

            // then
            var annotation = method.getAnnotation(org.springframework.web.bind.annotation.ExceptionHandler.class);
            assert annotation != null;
            assert annotation.value()[0] == MethodArgumentNotValidException.class;
        }
    }

    @Nested
    @DisplayName("Exception 처리 (기타 모든 예외)")
    class GenericExceptionHandling {

        @Test
        @DisplayName("RuntimeException 발생시 500 응답")
        void RuntimeException_발생시_500_응답() throws Exception {
            // when & then
            mockMvc.perform(get("/test/exception/runtime"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("NullPointerException 발생시 500 응답")
        void NullPointerException_발생시_500_응답() throws Exception {
            // when & then
            mockMvc.perform(get("/test/exception/npe"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("IllegalArgumentException 발생시 500 응답")
        void IllegalArgumentException_발생시_500_응답() throws Exception {
            // when & then
            mockMvc.perform(get("/test/exception/illegal-argument"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("GlobalExceptionHandler 어노테이션 검증")
    class AnnotationVerification {

        @Test
        @DisplayName("@RestControllerAdvice 어노테이션 존재 확인")
        void RestControllerAdvice_어노테이션_존재() {
            // when
            var annotation = GlobalExceptionHandler.class.getAnnotation(RestControllerAdvice.class);

            // then
            assert annotation != null;
        }
    }

    // 테스트용 컨트롤러
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/business/bad-request")
        public ApiResponse<Void> throwBadRequest() {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        @GetMapping("/business/unauthorized")
        public ApiResponse<Void> throwUnauthorized() {
            throw new BusinessException(ErrorCode.KIS_AUTH_ERROR);
        }

        @GetMapping("/business/bad-gateway")
        public ApiResponse<Void> throwBadGateway() {
            throw new BusinessException(ErrorCode.KIS_API_ERROR);
        }

        @GetMapping("/business/internal-error")
        public ApiResponse<Void> throwInternalError() {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }


        @GetMapping("/exception/runtime")
        public ApiResponse<Void> throwRuntimeException() {
            throw new RuntimeException("런타임 예외 발생");
        }

        @GetMapping("/exception/npe")
        public ApiResponse<Void> throwNullPointerException() {
            throw new NullPointerException("null 참조");
        }

        @GetMapping("/exception/illegal-argument")
        public ApiResponse<Void> throwIllegalArgumentException() {
            throw new IllegalArgumentException("잘못된 파라미터");
        }
    }
}
