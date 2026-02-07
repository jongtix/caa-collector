package com.custom.trader.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 기반 토큰 암호화/복호화 유틸리티.
 *
 * <p>Redis에 저장되는 OAuth 토큰을 암호화하여 저장 시 보호한다.
 * GCM 모드를 사용하여 기밀성과 무결성을 동시에 보장한다.</p>
 *
 * <h3>암호화 사양</h3>
 * <ul>
 *   <li>알고리즘: AES-256-GCM</li>
 *   <li>IV 크기: 96비트 (12바이트, NIST 권장)</li>
 *   <li>인증 태그: 128비트 (16바이트)</li>
 *   <li>출력 형식: Base64(IV + 암호문 + 태그)</li>
 * </ul>
 *
 * <h3>보안 특성</h3>
 * <ul>
 *   <li>각 암호화마다 무작위 IV 생성 (nonce 재사용 방지)</li>
 *   <li>GCM 인증 태그로 암호문 위변조 탐지</li>
 *   <li>SecureRandom 사용으로 예측 불가능한 IV 보장</li>
 * </ul>
 *
 * @since 2026-02-02
 */
public class TokenEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKeySpec;
    private final SecureRandom secureRandom;

    /**
     * Base64 인코딩된 AES-256 키로 TokenEncryptor를 생성한다.
     *
     * @param base64EncodedKey Base64 인코딩된 32바이트 AES 키
     * @throws IllegalArgumentException 키가 null이거나 비어있는 경우, 또는 디코딩된 키 길이가 32바이트가 아닌 경우
     */
    public TokenEncryptor(String base64EncodedKey) {
        if (base64EncodedKey == null || base64EncodedKey.isBlank()) {
            throw new IllegalArgumentException("Encryption key must not be null or blank");
        }

        byte[] keyBytes = Base64.getDecoder().decode(base64EncodedKey);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES-256 key must be exactly 32 bytes");
        }

        this.secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    /**
     * 평문을 AES-256-GCM으로 암호화한다.
     *
     * <p>무작위 96비트 IV를 생성하고, IV + 암호문을 Base64로 인코딩하여 반환한다.</p>
     *
     * @param plainText 암호화할 평문
     * @return Base64 인코딩된 암호문 (IV 포함)
     * @throws IllegalArgumentException plainText가 null이거나 비어있는 경우
     * @throws IllegalStateException 암호화 실패 시
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            throw new IllegalArgumentException("Plain text must not be null or empty");
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // IV + 암호문(태그 포함) 결합
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Token encryption failed", e);
        }
    }

    /**
     * AES-256-GCM 암호문을 복호화한다.
     *
     * <p>Base64 디코딩 후 IV를 추출하고, 암호문을 복호화하여 평문을 반환한다.
     * GCM 인증 태그 검증에 실패하면 예외가 발생한다.</p>
     *
     * @param cipherText Base64 인코딩된 암호문 (IV 포함)
     * @return 복호화된 평문
     * @throws IllegalArgumentException cipherText가 null이거나 비어있는 경우, 또는 형식이 잘못된 경우
     * @throws IllegalStateException 복호화 실패 시 (인증 태그 불일치 포함)
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            throw new IllegalArgumentException("Cipher text must not be null or empty");
        }

        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            if (combined.length < GCM_IV_LENGTH_BYTES + 1) {
                throw new IllegalArgumentException("Cipher text is too short to contain IV and data");
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);

            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, GCM_IV_LENGTH_BYTES, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Token decryption failed", e);
        }
    }
}
