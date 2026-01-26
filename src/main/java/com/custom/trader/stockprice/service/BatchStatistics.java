package com.custom.trader.stockprice.service;

import lombok.Getter;

/**
 * 배치 처리 통계를 추적하는 클래스.
 *
 * <p>예외 유형별로 성공/실패를 분류하여 배치 작업의 가시성을 제공합니다.</p>
 */
@Getter
public class BatchStatistics {
    private int total;
    private int success;
    private int recoverableFailure;  // KisApiException
    private int criticalFailure;     // DataAccessException
    private int unexpectedFailure;   // Other exceptions

    public void incrementTotal() {
        total++;
    }

    public void incrementSuccess() {
        success++;
    }

    public void incrementRecoverableFailure() {
        recoverableFailure++;
    }

    public void incrementCriticalFailure() {
        criticalFailure++;
    }

    public void incrementUnexpectedFailure() {
        unexpectedFailure++;
    }

    public double getSuccessRate() {
        return total == 0 ? 0.0 : (double) success / total * 100;
    }

    public String getSummary() {
        return String.format(
            "Total: %d, Success: %d (%.2f%%), Recoverable: %d, Critical: %d, Unexpected: %d",
            total, success, getSuccessRate(),
            recoverableFailure, criticalFailure, unexpectedFailure
        );
    }
}
