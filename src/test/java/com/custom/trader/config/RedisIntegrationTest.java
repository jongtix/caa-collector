package com.custom.trader.config;

import com.custom.trader.common.util.RedisKeyHasher;
import com.custom.trader.common.util.TokenEncryptor;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Redis 캐싱 통합 테스트 (Testcontainers 기반).
 *
 * <p>실제 Redis 서버와의 통합을 검증:
 * - Token TTL 자동 삭제
 * - Token 암호화/복호화
 * - Redis 키 해싱
 * </p>
 */
@Testcontainers
@DisplayName("Redis 캐싱 통합 테스트")
class RedisIntegrationTest {

    @Container
    private static final RedisContainer REDIS_CONTAINER = new RedisContainer(
            DockerImageName.parse("redis:7.4-alpine")
    ).withExposedPorts(6379);

    private RedisTemplate<String, String> redisTemplate;
    private TokenEncryptor tokenEncryptor;
    private RedisKeyHasher redisKeyHasher;

    @BeforeEach
    void setUp() {
        // Redis 연결 설정
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(REDIS_CONTAINER.getHost());
        config.setPort(REDIS_CONTAINER.getFirstMappedPort());

        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();

        // 테스트용 Bean 생성
        byte[] bytes32 = new byte[32];
        for (int i = 0; i < 32; i++) {
            bytes32[i] = (byte) i;
        }
        String validEncryptionKey = Base64.getEncoder().encodeToString(bytes32);
        tokenEncryptor = new TokenEncryptor(validEncryptionKey);

        String validHmacSecret = "this-is-a-valid-hmac-secret-key-with-32-chars-or-more";
        redisKeyHasher = new RedisKeyHasher(validHmacSecret);

        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Nested
    @DisplayName("Token TTL 자동 삭제 검증")
    class TokenTtlTest {

        @Test
        @DisplayName("Token 저장 후 TTL 설정되고 시간 경과 후 자동 삭제됨")
        void Token_저장_TTL_설정_자동_삭제() {
            // Given
            String accountNo = "12345678-01";
            String hashedKey = redisKeyHasher.hash(accountNo);
            String originalToken = "test-access-token-12345";
            String encryptedToken = tokenEncryptor.encrypt(originalToken);

            // When: Token 저장 (TTL 2초)
            redisTemplate.opsForValue().set(hashedKey, encryptedToken, Duration.ofSeconds(2));

            // Then: 즉시 조회하면 존재함
            String storedToken = redisTemplate.opsForValue().get(hashedKey);
            assertThat(storedToken).isNotNull();
            assertThat(tokenEncryptor.decrypt(storedToken)).isEqualTo(originalToken);

            // TTL 확인
            Long ttl = redisTemplate.getExpire(hashedKey, TimeUnit.SECONDS);
            assertThat(ttl).isGreaterThan(0).isLessThanOrEqualTo(2);

            // 3초 대기 후 조회하면 삭제되어야 함
            await().atMost(Duration.ofSeconds(4))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() ->
                            assertThat(redisTemplate.opsForValue().get(hashedKey)).isNull()
                    );
        }

        @Test
        @DisplayName("TTL 없이 저장하면 영구 보존됨")
        void TTL_없이_저장하면_영구_보존() throws InterruptedException {
            // Given
            String accountNo = "99999999-99";
            String hashedKey = redisKeyHasher.hash(accountNo);
            String originalToken = "permanent-token";
            String encryptedToken = tokenEncryptor.encrypt(originalToken);

            // When: TTL 없이 저장
            redisTemplate.opsForValue().set(hashedKey, encryptedToken);

            // Then: TTL이 -1 (무제한)
            Long ttl = redisTemplate.getExpire(hashedKey, TimeUnit.SECONDS);
            assertThat(ttl).isEqualTo(-1);

            // 2초 대기 후에도 여전히 존재
            Thread.sleep(2000);
            String storedToken = redisTemplate.opsForValue().get(hashedKey);
            assertThat(storedToken).isNotNull();
            assertThat(tokenEncryptor.decrypt(storedToken)).isEqualTo(originalToken);

            // 테스트 정리
            redisTemplate.delete(hashedKey);
        }
    }

    @Nested
    @DisplayName("Token 암호화/복호화 검증")
    class TokenEncryptionTest {

        @Test
        @DisplayName("Token 암호화 후 Redis 저장 후 복호화하면 원본과 일치")
        void Token_암호화_저장_복호화_원본일치() {
            // Given
            String accountNo = "11111111-01";
            String hashedKey = redisKeyHasher.hash(accountNo);
            String originalToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload.signature";

            // When: 암호화 후 저장
            String encryptedToken = tokenEncryptor.encrypt(originalToken);
            redisTemplate.opsForValue().set(hashedKey, encryptedToken);

            // Then: 조회 후 복호화하면 원본과 일치
            String storedEncryptedToken = redisTemplate.opsForValue().get(hashedKey);
            assertThat(storedEncryptedToken).isNotNull();

            String decryptedToken = tokenEncryptor.decrypt(storedEncryptedToken);
            assertThat(decryptedToken).isEqualTo(originalToken);
        }

        @Test
        @DisplayName("암호화된 값이 원본과 다름 (실제로 암호화됨)")
        void 암호화된_값이_원본과_다름() {
            // Given
            String originalToken = "plain-text-token-12345";

            // When
            String encryptedToken = tokenEncryptor.encrypt(originalToken);

            // Then: 암호화된 값은 원본과 다름
            assertThat(encryptedToken).isNotEqualTo(originalToken);
            assertThat(encryptedToken).isNotBlank();

            // 복호화하면 원본과 일치
            assertThat(tokenEncryptor.decrypt(encryptedToken)).isEqualTo(originalToken);
        }

        @Test
        @DisplayName("동일 원본을 여러 번 암호화하면 다른 값 생성 (IV 때문)")
        void 동일_원본_여러번_암호화_다른값() {
            // Given
            String originalToken = "same-token-12345";

            // When
            String encrypted1 = tokenEncryptor.encrypt(originalToken);
            String encrypted2 = tokenEncryptor.encrypt(originalToken);

            // Then: 암호화된 값이 다름 (IV가 매번 랜덤 생성됨)
            assertThat(encrypted1).isNotEqualTo(encrypted2);

            // 하지만 복호화하면 둘 다 원본과 일치
            assertThat(tokenEncryptor.decrypt(encrypted1)).isEqualTo(originalToken);
            assertThat(tokenEncryptor.decrypt(encrypted2)).isEqualTo(originalToken);
        }
    }

    @Nested
    @DisplayName("Redis 키 해싱 검증")
    class RedisKeyHashingTest {

        @Test
        @DisplayName("계좌번호 해싱 후 Redis 키로 사용 가능")
        void 계좌번호_해싱_Redis_키로_사용() {
            // Given
            String accountNo1 = "12345678-01";
            String accountNo2 = "87654321-02";

            // When
            String hashedKey1 = redisKeyHasher.hash(accountNo1);
            String hashedKey2 = redisKeyHasher.hash(accountNo2);

            // Then: 해시값이 다름
            assertThat(hashedKey1).isNotEqualTo(hashedKey2);

            // Redis 키로 사용 가능
            redisTemplate.opsForValue().set(hashedKey1, "token1");
            redisTemplate.opsForValue().set(hashedKey2, "token2");

            assertThat(redisTemplate.opsForValue().get(hashedKey1)).isEqualTo("token1");
            assertThat(redisTemplate.opsForValue().get(hashedKey2)).isEqualTo("token2");
        }

        @Test
        @DisplayName("동일한 계좌번호는 항상 동일한 해시값 생성")
        void 동일_계좌번호_동일_해시값() {
            // Given
            String accountNo = "99999999-99";

            // When
            String hash1 = redisKeyHasher.hash(accountNo);
            String hash2 = redisKeyHasher.hash(accountNo);

            // Then: 항상 동일한 해시값
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("해시값은 16자 고정 길이")
        void 해시값_16자_고정길이() {
            // Given
            String shortAccount = "123-01";
            String longAccount = "12345678901234567890-01";

            // When
            String hash1 = redisKeyHasher.hash(shortAccount);
            String hash2 = redisKeyHasher.hash(longAccount);

            // Then: 둘 다 16자 고정
            assertThat(hash1).hasSize(16);
            assertThat(hash2).hasSize(16);
        }
    }

    @Nested
    @DisplayName("전체 통합 시나리오")
    class EndToEndScenarioTest {

        @Test
        @DisplayName("실제 사용 시나리오: 계좌번호 해싱 → Token 암호화 → Redis 저장 → TTL 자동 삭제")
        void 실제_사용_시나리오_전체_흐름() {
            // Given
            String accountNo = "12345678-01";
            String accessToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload.signature";

            // When: 계좌번호 해싱
            String hashedKey = redisKeyHasher.hash(accountNo);
            assertThat(hashedKey).hasSize(16);

            // When: Token 암호화
            String encryptedToken = tokenEncryptor.encrypt(accessToken);
            assertThat(encryptedToken).isNotEqualTo(accessToken);

            // When: Redis 저장 (TTL 2초)
            redisTemplate.opsForValue().set(hashedKey, encryptedToken, Duration.ofSeconds(2));

            // Then: 즉시 조회 후 복호화하면 원본과 일치
            String storedToken = redisTemplate.opsForValue().get(hashedKey);
            assertThat(storedToken).isNotNull();
            String decryptedToken = tokenEncryptor.decrypt(storedToken);
            assertThat(decryptedToken).isEqualTo(accessToken);

            // Then: TTL 확인
            Long ttl = redisTemplate.getExpire(hashedKey, TimeUnit.SECONDS);
            assertThat(ttl).isGreaterThan(0).isLessThanOrEqualTo(2);

            // Then: 3초 대기 후 자동 삭제
            await().atMost(Duration.ofSeconds(4))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() ->
                            assertThat(redisTemplate.opsForValue().get(hashedKey)).isNull()
                    );
        }
    }
}
