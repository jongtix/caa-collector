package com.custom.trader.common.util;

/**
 * 로그 출력 시 민감 정보를 마스킹하는 유틸리티.
 *
 * <p>모든 마스킹 메서드는 고정 길이 별표를 사용하여 실제 값의 길이를 노출하지 않습니다.
 * Thread-safe하며 null-safe 합니다.</p>
 *
 * <h3>마스킹 규칙</h3>
 * <ul>
 *   <li>{@code maskUserId}: 앞 2자 + 8개 별표 (예: "P1********")</li>
 *   <li>{@code maskAccountNumber}: 7개 별표 + 뒤 4자 (예: "*******8-01")</li>
 *   <li>{@code maskAppKey}: 앞 4자 + 12개 별표 (예: "PSab************")</li>
 *   <li>{@code maskAppSecret}: 22개 별표 (예: "**********************")</li>
 *   <li>{@code maskAccessToken}: 앞 4자 + 16개 별표 (예: "eyJh****************")</li>
 * </ul>
 *
 * @since 2026-02-01
 */
public final class LogMaskingUtil {

    private static final String MASKED_USER_ID_SUFFIX = "********";          // 8자
    private static final String MASKED_ACCOUNT_NUMBER_PREFIX = "*******";   // 7자
    private static final String MASKED_APP_KEY_SUFFIX = "************";     // 12자
    private static final String MASKED_APP_SECRET = "**********************"; // 22자
    private static final String MASKED_ACCESS_TOKEN_SUFFIX = "****************"; // 16자

    private static final String FALLBACK_MASK = "***";

    private static final int USER_ID_PREFIX_LENGTH = 2;
    private static final int ACCOUNT_NUMBER_SUFFIX_LENGTH = 4;
    private static final int APP_KEY_PREFIX_LENGTH = 4;
    private static final int ACCESS_TOKEN_PREFIX_LENGTH = 4;

    private LogMaskingUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 사용자 ID를 마스킹합니다.
     *
     * <p>앞 2자만 노출하고 나머지는 고정 길이 별표로 대체합니다.</p>
     *
     * @param userId 사용자 ID
     * @return 마스킹된 문자열 (예: "P1********")
     */
    public static String maskUserId(String userId) {
        if (userId == null || userId.length() <= USER_ID_PREFIX_LENGTH) {
            return FALLBACK_MASK;
        }
        return userId.substring(0, USER_ID_PREFIX_LENGTH) + MASKED_USER_ID_SUFFIX;
    }

    /**
     * 계좌번호를 마스킹합니다.
     *
     * <p>뒤 4자만 노출하고 앞부분은 고정 길이 별표로 대체합니다.</p>
     *
     * @param accountNumber 계좌번호 (예: "12345678-01")
     * @return 마스킹된 문자열 (예: "*******8-01")
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= ACCOUNT_NUMBER_SUFFIX_LENGTH) {
            return FALLBACK_MASK;
        }
        return MASKED_ACCOUNT_NUMBER_PREFIX +
               accountNumber.substring(accountNumber.length() - ACCOUNT_NUMBER_SUFFIX_LENGTH);
    }

    /**
     * API 앱 키를 마스킹합니다.
     *
     * <p>앞 4자만 노출하고 나머지는 고정 길이 별표로 대체합니다.</p>
     *
     * @param appKey API 앱 키
     * @return 마스킹된 문자열 (예: "PSab************")
     */
    public static String maskAppKey(String appKey) {
        if (appKey == null || appKey.length() <= APP_KEY_PREFIX_LENGTH) {
            return FALLBACK_MASK;
        }
        return appKey.substring(0, APP_KEY_PREFIX_LENGTH) + MASKED_APP_KEY_SUFFIX;
    }

    /**
     * API 앱 시크릿을 마스킹합니다.
     *
     * <p>모든 문자를 고정 길이 별표로 대체합니다.</p>
     *
     * @param appSecret API 앱 시크릿
     * @return 마스킹된 문자열 (예: "**********************")
     */
    public static String maskAppSecret(String appSecret) {
        if (appSecret == null) {
            return FALLBACK_MASK;
        }
        return MASKED_APP_SECRET;
    }

    /**
     * OAuth Access Token을 마스킹합니다.
     *
     * <p>앞 4자만 노출하고 나머지는 고정 길이 별표로 대체합니다.
     * Bearer 토큰(약 500-1000자)의 전체 길이를 노출하지 않습니다.</p>
     *
     * @param accessToken OAuth 2.0 액세스 토큰
     * @return 마스킹된 문자열 (예: "eyJh****************")
     */
    public static String maskAccessToken(String accessToken) {
        if (accessToken == null || accessToken.length() <= ACCESS_TOKEN_PREFIX_LENGTH) {
            return FALLBACK_MASK;
        }
        return accessToken.substring(0, ACCESS_TOKEN_PREFIX_LENGTH) + MASKED_ACCESS_TOKEN_SUFFIX;
    }
}
