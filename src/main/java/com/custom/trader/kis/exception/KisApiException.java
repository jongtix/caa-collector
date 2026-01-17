package com.custom.trader.kis.exception;

import com.custom.trader.common.exception.BusinessException;
import com.custom.trader.common.exception.ErrorCode;

public class KisApiException extends BusinessException {

    public KisApiException(String message) {
        super(ErrorCode.KIS_API_ERROR, message);
    }

    public KisApiException(ErrorCode errorCode) {
        super(errorCode);
    }

    public KisApiException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
