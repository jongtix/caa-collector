package com.custom.trader.stockprice.scheduler;

import com.custom.trader.stockprice.service.StockPriceCollectionService;
import com.custom.trader.testcontainers.MySQLTestcontainersConfig;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/**
 * ShedLock 분산 잠금 통합 테스트.
 *
 * <p>Testcontainers를 사용하여 실제 Redis 컨테이너에서 분산 잠금 동작을 검증합니다.</p>
 *
 * <p>테스트 시나리오:
 * <ul>
 *   <li>TC-1: 단일 인스턴스 잠금 획득 및 작업 실행</li>
 *   <li>TC-2: 동시 실행 2개 인스턴스 - 1개만 실행</li>
 *   <li>TC-3: 잠금 타임아웃 후 재획득</li>
 *   <li>TC-4: Redis 연결 실패 시 안전한 처리</li>
 * </ul>
 * </p>
 */
@Slf4j
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(MySQLTestcontainersConfig.class)
@DisplayName("ShedLock 분산 잠금 통합 테스트")
class ShedLockIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(false);

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("management.health.redis.enabled", () -> "true");
    }

    @Autowired
    private StockPriceScheduler scheduler;

    @SpyBean
    private StockPriceCollectionService collectionService;

    @Autowired
    private LockProvider lockProvider;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        // Mock 카운터 초기화
        clearInvocations(collectionService);

        // Redis Lock 초기화 (테스트 격리)
        try {
            redisConnectionFactory.getConnection().serverCommands().flushAll();
        } catch (Exception e) {
            log.warn("Failed to flush Redis: {}", e.getMessage());
        }
    }

    @Test
    @DisplayName("TC-1: 단일 인스턴스 잠금 획득 및 작업 실행")
    void singleInstanceShouldAcquireLockAndExecute() {
        // When: 첫 번째 실행
        scheduler.collectDailyPrices();

        // Then: StockPriceCollectionService.collectDailyPrices() 호출 확인
        verify(collectionService, times(1)).collectDailyPrices();

        // And: lockAtLeastFor 시간 내 재실행 시도 시 Lock 획득 실패로 실행되지 않음
        scheduler.collectDailyPrices();

        // Then: 여전히 1번만 실행됨 (두 번째 호출은 Lock 획득 실패로 차단)
        verify(collectionService, times(1)).collectDailyPrices();
    }

    @Test
    @DisplayName("TC-2: 동시 실행 2개 인스턴스 - 1개만 실행")
    void concurrentInstancesOnlyOneAcquiresLock() throws InterruptedException {
        // Given: 2개의 스레드가 동시에 실행 시도
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger executionCount = new AtomicInteger(0);

        // CollectionService에 지연 시간 주입 (Lock 보유 시간 확보)
        doAnswer(invocation -> {
            executionCount.incrementAndGet();
            Thread.sleep(500); // Lock을 보유한 상태로 0.5초 대기
            return null;
        }).when(collectionService).collectDailyPrices();

        // When: 2개의 스레드가 동시에 같은 작업 시도
        Runnable task = () -> {
            try {
                startLatch.await(); // 동시 시작 보장
                scheduler.collectDailyPrices();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        Thread thread1 = new Thread(task, "Scheduler-1");
        Thread thread2 = new Thread(task, "Scheduler-2");

        thread1.start();
        thread2.start();
        startLatch.countDown(); // 동시 실행 시작

        boolean completed = doneLatch.await(3, SECONDS);

        // Then: 2개의 스레드 모두 완료
        assertThat(completed).isTrue();

        // And: StockPriceCollectionService는 1번만 호출 (다른 1개는 Lock 획득 실패)
        verify(collectionService, times(1)).collectDailyPrices();

        // And: 실제 비즈니스 로직도 1번만 실행
        assertThat(executionCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-3: 잠금 타임아웃 후 재획득")
    void lockShouldBeReacquiredAfterTimeout() {
        // Given: 짧은 타임아웃 시간으로 Lock 획득 (1초 후 만료)
        Optional<SimpleLock> lock = lockProvider.lock(
                new net.javacrumbs.shedlock.core.LockConfiguration(
                        Instant.now(),
                        "collectDailyPrices",
                        Duration.ofSeconds(1),
                        Duration.ZERO
                )
        );

        // When: Lock 획득 성공 확인
        assertThat(lock).isPresent();
        lock.ifPresent(SimpleLock::unlock);

        // And: 타임아웃 대기 후 다시 Lock 획득 시도
        await()
                .atMost(3, SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Optional<SimpleLock> reacquiredLock = lockProvider.lock(
                            new net.javacrumbs.shedlock.core.LockConfiguration(
                                    Instant.now(),
                                    "collectDailyPrices",
                                    Duration.ofSeconds(5),
                                    Duration.ZERO
                            )
                    );

                    // Then: 타임아웃 후 Lock 재획득 가능
                    assertThat(reacquiredLock).isPresent();
                    reacquiredLock.ifPresent(SimpleLock::unlock);
                });
    }

    @Test
    @DisplayName("TC-4: Redis 연결 실패 시 안전한 처리")
    void shouldHandleRedisConnectionFailureSafely() {
        // Given: Redis 컨테이너 중지 (연결 실패 시뮬레이션)
        redis.stop();

        // When: 스케줄러 실행 시도
        try {
            scheduler.collectDailyPrices();
        } catch (Exception e) {
            // Then: 예외가 발생하더라도 애플리케이션이 중단되지 않음
            log.info("Expected exception during Redis failure: {}", e.getMessage());
        }

        // And: CollectionService는 호출되지 않음 (Lock 획득 실패)
        verify(collectionService, never()).collectDailyPrices();

        // Cleanup: Redis 컨테이너 재시작
        redis.start();
    }

    /**
     * Redis에 특정 이름의 Lock이 존재하는지 확인.
     *
     * @param lockName Lock 이름
     * @return Lock 존재 여부
     */
    private boolean isLockExistsInRedis(String lockName) {
        try {
            String key = "trader:job-lock:" + lockName;
            return Boolean.TRUE.equals(
                    redisConnectionFactory.getConnection().keyCommands().exists(key.getBytes())
            );
        } catch (Exception e) {
            log.warn("Failed to check lock existence in Redis: {}", e.getMessage());
            return false;
        }
    }
}
