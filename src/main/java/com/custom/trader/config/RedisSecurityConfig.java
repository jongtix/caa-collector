package com.custom.trader.config;

import com.custom.trader.common.util.RedisKeyHasher;
import com.custom.trader.common.util.TokenEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 보안 관련 Bean 설정.
 *
 * <p>Redis 키 해싱 및 토큰 암호화에 필요한 유틸리티 Bean을 등록한다.</p>
 *
 * <h3>필수 환경변수</h3>
 * <ul>
 *   <li>{@code REDIS_KEY_HMAC_SECRET}: HMAC-SHA256 비밀키 (최소 32자)</li>
 *   <li>{@code TOKEN_ENCRYPTION_KEY}: AES-256 키 (Base64 인코딩된 32바이트)</li>
 * </ul>
 *
 * @since 2026-02-02
 */
@Configuration
public class RedisSecurityConfig {

    /**
     * Redis 키 해싱 Bean.
     *
     * <p>계좌번호를 HMAC-SHA256으로 해싱하여 Redis 키에 평문 노출을 방지한다.</p>
     *
     * @param hmacSecret HMAC 비밀키
     * @return RedisKeyHasher 인스턴스
     */
    @Bean
    public RedisKeyHasher redisKeyHasher(
            @Value("${security.redis.hmac-secret}") String hmacSecret
    ) {
        return new RedisKeyHasher(hmacSecret);
    }

    /**
     * 토큰 암호화 Bean.
     *
     * <p>Redis에 저장되는 OAuth 토큰을 AES-256-GCM으로 암호화한다.</p>
     *
     * @param encryptionKey Base64 인코딩된 AES-256 키
     * @return TokenEncryptor 인스턴스
     */
    @Bean
    public TokenEncryptor tokenEncryptor(
            @Value("${security.token.encryption-key}") String encryptionKey
    ) {
        return new TokenEncryptor(encryptionKey);
    }
}
