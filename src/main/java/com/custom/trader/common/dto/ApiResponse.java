package com.custom.trader.common.dto;

import com.custom.trader.common.constant.DateFormatConstants;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now(DateFormatConstants.KST_ZONE_ID));
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, LocalDateTime.now(DateFormatConstants.KST_ZONE_ID));
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message, LocalDateTime.now(DateFormatConstants.KST_ZONE_ID));
    }
}
