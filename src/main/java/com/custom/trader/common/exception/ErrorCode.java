package com.custom.trader.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),

    // KIS API
    KIS_API_ERROR(HttpStatus.BAD_GATEWAY, "한국투자증권 API 호출 중 오류가 발생했습니다."),
    KIS_AUTH_ERROR(HttpStatus.UNAUTHORIZED, "한국투자증권 인증에 실패했습니다."),
    KIS_NO_ACCOUNT(HttpStatus.BAD_REQUEST, "설정된 계좌가 없습니다.");

    private final HttpStatus status;
    private final String message;
}
