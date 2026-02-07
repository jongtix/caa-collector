package com.custom.trader.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Redis 키에 사용되는 계좌번호를 HMAC-SHA256으로 해싱하는 유틸리티.
 *
 * <p>민감한 계좌번호가 Redis 키에 평문으로 노출되지 않도록 보호한다.
 * 해시 결과의 앞 16자만 사용하여 키 길이를 절약하면서도
 * 충분한 엔트로피(64비트)를 확보한다.</p>
 *
 * <h3>보안 특성</h3>
 * <ul>
 *   <li>HMAC-SHA256 사용으로 비밀키 없이는 원본 값 역추적 불가</li>
 *   <li>동일 입력에 대해 항상 동일 출력 (결정적 해시)</li>
 *   <li>16자 접두어 사용 시 충돌 확률: 약 1/2^64</li>
 * </ul>
 *
 * <h3>성능 최적화</h3>
 * <p>ThreadLocal을 사용하여 스레드별로 Mac 인스턴스를 캐싱한다.
 * Mac.getInstance() 호출 비용을 절약하면서도 thread-safe를 보장한다.</p>
 *
 * @since 2026-02-02
 */
public class RedisKeyHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int HASH_PREFIX_LENGTH = 16;

    private final SecretKeySpec secretKeySpec;

    /**
     * ThreadLocal로 스레드별 Mac 인스턴스 캐싱.
     * Mac은 thread-safe하지 않으므로 스레드별로 독립적인 인스턴스를 유지한다.
     */
    private final ThreadLocal<Mac> macThreadLocal;

    private static final int MINIMUM_SECRET_LENGTH = 32;

    /**
     * HMAC 비밀키로 RedisKeyHasher를 생성한다.
     *
     * <p>비밀키는 최소 32자 이상이어야 한다. 짧은 비밀키는 계좌번호(8자리-2자리)의
     * 브루트포스 역추적을 가능하게 하므로 보안상 허용하지 않는다.</p>
     *
     * @param hmacSecret HMAC 비밀키 (최소 32자 필수)
     * @throws IllegalArgumentException hmacSecret이 null이거나 비어있거나 32자 미만인 경우
     */
    public RedisKeyHasher(String hmacSecret) {
        if (hmacSecret == null || hmacSecret.isBlank()) {
            throw new IllegalArgumentException("HMAC secret must not be null or blank");
        }
        if (hmacSecret.length() < MINIMUM_SECRET_LENGTH) {
            throw new IllegalArgumentException(
                    "HMAC secret must be at least " + MINIMUM_SECRET_LENGTH
                            + " characters for security. Current length: " + hmacSecret.length());
        }
        this.secretKeySpec = new SecretKeySpec(
                hmacSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
        );
        this.macThreadLocal = ThreadLocal.withInitial(() -> {
            try {
                Mac mac = Mac.getInstance(HMAC_ALGORITHM);
                mac.init(secretKeySpec);
                return mac;
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("HMAC-SHA256 algorithm not available", e);
            } catch (InvalidKeyException e) {
                throw new IllegalStateException("Invalid HMAC secret key", e);
            }
        });
    }

    /**
     * 계좌번호를 HMAC-SHA256으로 해싱한다.
     *
     * <p>결과는 해시의 앞 16자(hex)만 반환한다.</p>
     *
     * <p>Mac 인스턴스는 ThreadLocal에서 관리되며, 생성 시점에 이미 초기화가 완료된다.
     * 따라서 doFinal() 호출 시 체크 예외가 발생하지 않는다.</p>
     *
     * @param accountNumber 해싱할 계좌번호
     * @return 해싱된 문자열 (16자 hex)
     * @throws IllegalArgumentException accountNumber가 null이거나 비어있는 경우
     */
    public String hash(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number must not be null or blank");
        }

        Mac mac = macThreadLocal.get();  // ThreadLocal에서 인스턴스 가져오기 (이미 초기화됨)
        byte[] hmacBytes = mac.doFinal(accountNumber.getBytes(StandardCharsets.UTF_8));
        String fullHex = HexFormat.of().formatHex(hmacBytes);
        return fullHex.substring(0, HASH_PREFIX_LENGTH);
    }
}
