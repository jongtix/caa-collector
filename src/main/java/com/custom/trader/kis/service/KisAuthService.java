package com.custom.trader.kis.service;

import com.custom.trader.common.exception.ErrorCode;
import com.custom.trader.kis.config.KisAccountProperties;
import com.custom.trader.kis.config.KisProperties;
import com.custom.trader.kis.dto.KisTokenRequest;
import com.custom.trader.kis.dto.KisTokenResponse;
import com.custom.trader.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisAuthService {

    private static final String TOKEN_PATH = "/oauth2/tokenP";
    private static final String REDIS_KEY_PREFIX = "kis:token:";
    private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConcurrentHashMap<String, ReentrantLock> accountLocks = new ConcurrentHashMap<>();

    private final RestClient kisRestClient;
    private final KisProperties kisProperties;
    private final StringRedisTemplate redisTemplate;

    public String getAccessToken(String accountNameOrNumber) {
        KisAccountProperties account = findAccount(accountNameOrNumber);
        String cacheKey = REDIS_KEY_PREFIX + account.accountNumber();

        String cachedToken = redisTemplate.opsForValue().get(cacheKey);
        if (cachedToken != null) {
            return cachedToken;
        }

        return refreshToken(account);
    }

    private KisAccountProperties findAccount(String accountNameOrNumber) {
        return kisProperties.accounts().stream()
                .filter(acc -> acc.name().equals(accountNameOrNumber)
                        || acc.accountNumber().equals(accountNameOrNumber))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Account lookup failed for identifier hash: {}", accountNameOrNumber.hashCode());
                    return new KisApiException(ErrorCode.KIS_NO_ACCOUNT);
                });
    }

    private String refreshToken(KisAccountProperties account) {
        String cacheKey = REDIS_KEY_PREFIX + account.accountNumber();

        ReentrantLock lock = accountLocks.computeIfAbsent(
                account.accountNumber(),
                k -> new ReentrantLock()
        );

        lock.lock();
        try {
            String cachedToken = redisTemplate.opsForValue().get(cacheKey);
            if (cachedToken != null) {
                return cachedToken;
            }

            log.info("Requesting new access token for account hash: {}", account.accountNumber().hashCode());

            KisTokenRequest request = KisTokenRequest.of(account.appKey(), account.appSecret());

            KisTokenResponse response = kisRestClient.post()
                    .uri(TOKEN_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(KisTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                log.error("Failed to get access token for account hash: {}", account.accountNumber().hashCode());
                throw new KisApiException(ErrorCode.KIS_AUTH_ERROR);
            }

            LocalDateTime expiryTime = LocalDateTime.parse(response.accessTokenTokenExpired(), EXPIRY_FORMATTER);
            Duration ttl = Duration.between(LocalDateTime.now(), expiryTime.minusMinutes(5));

            if (ttl.isPositive()) {
                redisTemplate.opsForValue().set(cacheKey, response.accessToken(), ttl);
            }

            log.info("Access token obtained successfully, expires at: {}", expiryTime);

            return response.accessToken();
        } finally {
            lock.unlock();
        }
    }
}
