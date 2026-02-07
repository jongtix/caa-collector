package com.custom.trader.kis.service;

import static com.custom.trader.common.constant.DateFormatConstants.KST_ZONE_ID;

import com.custom.trader.common.exception.ErrorCode;
import com.custom.trader.common.util.LogMaskingUtil;
import com.custom.trader.common.util.RedisKeyHasher;
import com.custom.trader.common.util.TokenEncryptor;
import com.custom.trader.kis.config.KisAccountProperties;
import com.custom.trader.kis.config.KisProperties;
import com.custom.trader.kis.dto.auth.KisTokenRequest;
import com.custom.trader.kis.dto.auth.KisTokenResponse;
import com.custom.trader.kis.exception.KisApiException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisAuthService {

    private static final String TOKEN_PATH = "/oauth2/tokenP";
    private static final String REDIS_KEY_PREFIX = "kis:token:";
    private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 계정별 토큰 갱신 Lock (Double-Checked Locking 패턴용).
     *
     * <p>현재 상황: 계정 수 고정 2개, 맵 크기 최대 2개로 메모리 누수 없음.</p>
     *
     * <p>향후 계정이 동적으로 추가/삭제되는 경우:
     * - WeakHashMap 사용하여 미사용 Lock 자동 제거, 또는
     * - Guava Cache의 expireAfterAccess로 일정 시간 미사용 Lock 제거</p>
     */
    private final ConcurrentHashMap<String, ReentrantLock> accountLocks = new ConcurrentHashMap<>();

    private final RestClient kisRestClient;
    private final KisProperties kisProperties;
    private final StringRedisTemplate redisTemplate;
    private final RedisKeyHasher redisKeyHasher;
    private final TokenEncryptor tokenEncryptor;

    public String getAccessToken(String accountNameOrNumber) {
        KisAccountProperties account = findAccount(accountNameOrNumber);
        String cacheKey = REDIS_KEY_PREFIX + redisKeyHasher.hash(account.accountNumber());

        String encryptedToken = redisTemplate.opsForValue().get(cacheKey);
        if (encryptedToken != null) {
            return tokenEncryptor.decrypt(encryptedToken);
        }

        return refreshToken(account);
    }

    private KisAccountProperties findAccount(String accountNameOrNumber) {
        return kisProperties.accounts().stream()
                .filter(acc -> acc.name().equals(accountNameOrNumber)
                        || acc.accountNumber().equals(accountNameOrNumber))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Account lookup failed for identifier: {}", LogMaskingUtil.maskAccountNumber(accountNameOrNumber));
                    return new KisApiException(ErrorCode.KIS_NO_ACCOUNT);
                });
    }

    /**
     * 기본 계정을 조회합니다.
     *
     * <p>설정된 계정 목록에서 첫 번째 계정을 기본 계정으로 반환합니다.
     * 현재는 단일 계정 전략을 사용하나, 향후 멀티 계정 지원 시 확장 가능합니다.</p>
     *
     * @return 기본 계정
     * @throws KisApiException 설정된 계정이 없을 때
     */
    public KisAccountProperties getDefaultAccount() {
        if (kisProperties.accounts() == null || kisProperties.accounts().isEmpty()) {
            throw new KisApiException("No accounts configured");
        }
        return kisProperties.accounts().getFirst();
    }

    private String refreshToken(KisAccountProperties account) {
        String hashedAccountNumber = redisKeyHasher.hash(account.accountNumber());
        String cacheKey = REDIS_KEY_PREFIX + hashedAccountNumber;

        ReentrantLock lock = accountLocks.computeIfAbsent(
                hashedAccountNumber,
                k -> new ReentrantLock()
        );

        lock.lock();
        try {
            String encryptedToken = redisTemplate.opsForValue().get(cacheKey);
            if (encryptedToken != null) {
                return tokenEncryptor.decrypt(encryptedToken);
            }

            log.info("Requesting new access token for account: {}", LogMaskingUtil.maskAccountNumber(account.accountNumber()));

            KisTokenRequest request = KisTokenRequest.of(account.appKey(), account.appSecret());

            KisTokenResponse response = kisRestClient.post()
                    .uri(TOKEN_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(KisTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                log.error("Failed to get access token for account: {}", LogMaskingUtil.maskAccountNumber(account.accountNumber()));
                throw new KisApiException(ErrorCode.KIS_AUTH_ERROR);
            }

            LocalDateTime expiryTime = LocalDateTime.parse(response.accessTokenTokenExpired(), EXPIRY_FORMATTER);
            Duration ttl = Duration.between(LocalDateTime.now(KST_ZONE_ID), expiryTime.minusMinutes(5));

            if (ttl.isPositive()) {
                redisTemplate.opsForValue().set(cacheKey, tokenEncryptor.encrypt(response.accessToken()), ttl);
            }

            log.info("Access token obtained successfully, expires at: {}", expiryTime);

            return response.accessToken();
        } finally {
            lock.unlock();
        }
    }
}
