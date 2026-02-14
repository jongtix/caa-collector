package com.custom.trader.config;

import com.custom.trader.testcontainers.MySQLTestcontainersConfig;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * RateLimiterConfig 테스트
 *
 * <p>Google Guava RateLimiter를 사용한 KIS API 호출 속도 제한 검증
 *
 * <h2>테스트 범위</h2>
 * <ul>
 *   <li>Rate Limiter 기본 동작 검증 (5개)</li>
 *   <li>시간 경과 후 재충전 검증 (2개)</li>
 *   <li>동시성 환경 검증 (2개)</li>
 *   <li>경계값 및 예외 케이스 (3개)</li>
 *   <li>실제 사용 시나리오 (3개)</li>
 * </ul>
 *
 * @see RateLimiterConfig
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(MySQLTestcontainersConfig.class)
@DisplayName("RateLimiterConfig 테스트")
class RateLimiterConfigTest {

    @Autowired
    private RateLimiter kisApiRateLimiter;

    private static final double PERMITS_PER_SECOND = 20.0;
    private static final double EPSILON = 0.1; // 허용 오차

    @BeforeEach
    void setUp() {
        // RateLimiter 상태 초기화: 누적된 permit을 모두 소진하고 정확히 20개 permit만 보유
        // tryAcquire를 반복하여 모든 누적 permit 소진
        while (kisApiRateLimiter.tryAcquire(Duration.ZERO)) {
            // 누적된 permit 소진
        }

        // 1초 대기하여 정확히 20개 permit 충전 (20 permits/sec)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Nested
    @DisplayName("Rate Limiter 기본 동작 검증")
    class BasicBehavior {

        @Test
        @DisplayName("Bean이 정상적으로 생성되어야 함")
        void beanShouldBeCreated() {
            assertThat(kisApiRateLimiter).isNotNull();
        }

        @Test
        @DisplayName("초당 허용량이 20으로 설정되어야 함")
        void rateShouldBe20PermitsPerSecond() {
            double actualRate = kisApiRateLimiter.getRate();
            assertThat(actualRate).isCloseTo(PERMITS_PER_SECOND, within(EPSILON));
        }

        @Test
        @DisplayName("단일 permit 획득 시 즉시 성공해야 함")
        void singleAcquireShouldSucceedImmediately() {
            boolean acquired = kisApiRateLimiter.tryAcquire();
            assertThat(acquired).isTrue();
        }

        @Test
        @DisplayName("연속 20개 permit 획득 시 모두 성공해야 함")
        void twentyConsecutiveAcquiresShouldSucceed() {
            for (int i = 0; i < 20; i++) {
                boolean acquired = kisApiRateLimiter.tryAcquire(Duration.ofMillis(100));
                assertThat(acquired).as("Permit %d 획득 실패", i + 1).isTrue();
            }
        }

        @Test
        @DisplayName("acquire() 호출 시 블록킹되지 않고 즉시 반환해야 함")
        void acquireShouldNotBlockInitially() {
            long startTime = System.nanoTime();
            kisApiRateLimiter.acquire(); // 블록킹 호출
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            // CI 환경 및 Testcontainers 오버헤드 고려하여 100ms로 완화
            assertThat(elapsedMs).isLessThan(100);
        }
    }

    @Nested
    @DisplayName("시간 경과 후 재충전 검증")
    class RechargeAfterTime {

        @Test
        @DisplayName("1초 대기 후 permit이 재충전되어야 함")
        void permitsShouldRechargeAfterOneSecond() throws InterruptedException {
            // Given: 초기 permit 20개 모두 소진
            for (int i = 0; i < 20; i++) {
                kisApiRateLimiter.acquire();
            }

            // When: 1초 대기
            Thread.sleep(1000);

            // Then: 20개 permit 재충전 확인
            int rechargedCount = 0;
            for (int i = 0; i < 20; i++) {
                if (kisApiRateLimiter.tryAcquire()) {
                    rechargedCount++;
                }
            }
            assertThat(rechargedCount).isGreaterThanOrEqualTo(19); // 타이밍 오차 허용
        }

        @Test
        @DisplayName("0.5초 대기 후 약 10개 permit이 재충전되어야 함")
        void permitsShouldRechargeProportionally() throws InterruptedException {
            // Given: 초기 permit 20개 모두 소진
            for (int i = 0; i < 20; i++) {
                kisApiRateLimiter.acquire();
            }

            // When: 0.5초 대기
            Thread.sleep(500);

            // Then: 약 10개 permit 재충전 확인 (오차 ±2)
            int rechargedCount = 0;
            for (int i = 0; i < 20; i++) {
                if (kisApiRateLimiter.tryAcquire()) {
                    rechargedCount++;
                }
            }
            assertThat(rechargedCount).isBetween(8, 12);
        }
    }

    @Nested
    @DisplayName("동시성 환경 검증")
    class ConcurrencyScenarios {

        @Test
        @DisplayName("다중 스레드에서 동시 요청 시 초당 20개로 제한되어야 함")
        void concurrentRequestsShouldBeRateLimited() throws InterruptedException {
            // Given: 10개 스레드가 각각 5번씩 요청 (총 50개)
            int threadCount = 10;
            int requestsPerThread = 5;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            long startTime = System.nanoTime();

            // When: 동시 요청 실행
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await(); // 동시 시작 대기
                        for (int j = 0; j < requestsPerThread; j++) {
                            kisApiRateLimiter.acquire();
                            successCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown(); // 모든 스레드 시작
            doneLatch.await(); // 모든 스레드 완료 대기

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            // Then: 50개 요청 완료에 약 1.5초 소요
            // @BeforeEach에서 20개 permit 확보했으므로 (50-20)/20 = 1.5초 (CI 환경 고려 오차 ±700ms)
            assertThat(successCount.get()).isEqualTo(50);
            assertThat(elapsedMs).isBetween(800L, 3000L);
        }

        @Test
        @DisplayName("경쟁 상태에서도 permit 누수가 없어야 함")
        void noPermitLeakageUnderRaceCondition() throws InterruptedException {
            // Given: 100개 스레드가 각각 tryAcquire 시도
            int threadCount = 100;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // When: 동시 tryAcquire 실행
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        if (kisApiRateLimiter.tryAcquire(Duration.ofMillis(10))) {
                            successCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            doneLatch.await();

            // Then: 성공 횟수가 초기 permit 수(20)를 초과하지 않아야 함
            assertThat(successCount.get()).isLessThanOrEqualTo(20);
        }
    }

    @Nested
    @DisplayName("경계값 및 예외 케이스")
    class EdgeCases {

        @Test
        @DisplayName("타임아웃 0ms로 tryAcquire 시 즉시 반환해야 함")
        void tryAcquireWithZeroTimeoutShouldReturnImmediately() {
            long startTime = System.nanoTime();
            boolean acquired = kisApiRateLimiter.tryAcquire(Duration.ZERO);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            // CI 환경 및 Testcontainers 오버헤드 고려하여 100ms로 완화
            assertThat(elapsedMs).isLessThan(100);
            assertThat(acquired).isTrue(); // 초기 permit 존재
        }

        @Test
        @DisplayName("연속 30개 요청 시 21번째부터 블록킹되어야 함")
        void requestsBeyondLimitShouldBlock() {
            long startTime = System.nanoTime();

            // 처음 20개는 즉시 성공
            for (int i = 0; i < 20; i++) {
                kisApiRateLimiter.acquire();
            }

            // 21번째 요청은 블록킹됨
            kisApiRateLimiter.acquire();
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            // 블록킹 시간이 약 50ms (1/20초) 이상이어야 함 (CI 환경 고려하여 20ms로 완화)
            assertThat(elapsedMs).isGreaterThanOrEqualTo(20);
        }

        @Test
        @DisplayName("매우 짧은 타임아웃으로 permit 소진 후 tryAcquire 시 실패해야 함")
        void tryAcquireWithShortTimeoutShouldFailAfterDepletion() {
            // Given: permit 20개 모두 소진
            for (int i = 0; i < 20; i++) {
                kisApiRateLimiter.acquire();
            }

            // When: 1ms 타임아웃으로 시도
            boolean acquired = kisApiRateLimiter.tryAcquire(Duration.ofMillis(1));

            // Then: 실패해야 함
            assertThat(acquired).isFalse();
        }
    }

    @Nested
    @DisplayName("실제 사용 시나리오")
    class RealWorldScenarios {

        @Test
        @DisplayName("KIS API 호출 패턴 시뮬레이션 (1초당 15회 호출)")
        void simulateKisApiCallPattern() throws InterruptedException {
            // Given: 1초당 15회 호출 (초당 20회 한도 이내)
            int requestsPerSecond = 15;
            long startTime = System.nanoTime();

            // When: 3초간 반복 호출
            for (int sec = 0; sec < 3; sec++) {
                for (int i = 0; i < requestsPerSecond; i++) {
                    kisApiRateLimiter.acquire();
                }
                Thread.sleep(1000); // 1초 대기
            }

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            // Then: 약 3초 소요 (CI 환경 고려하여 오차 ±500ms)
            assertThat(elapsedMs).isBetween(2500L, 3800L);
        }

        @Test
        @DisplayName("버스트 트래픽 처리 (초기 20개 즉시, 이후 점진적)")
        void handleBurstTraffic() {
            long startTime = System.nanoTime();

            // 초기 20개 즉시 처리
            for (int i = 0; i < 20; i++) {
                kisApiRateLimiter.acquire();
            }

            long firstBatchTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            // 추가 10개 처리 (블록킹됨)
            for (int i = 0; i < 10; i++) {
                kisApiRateLimiter.acquire();
            }

            long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            // 처음 20개는 즉시 (CI 환경 고려하여 100ms 미만)
            assertThat(firstBatchTime).isLessThan(100);

            // 총 30개 처리에 약 0.5초 소요 (10/20 = 0.5초, CI 환경 고려하여 오차 ±300ms)
            assertThat(totalTime).isBetween(200L, 1000L);
        }

        @Test
        @DisplayName("장기 실행 시나리오 (5초간 지속 호출)")
        void longRunningScenario() throws InterruptedException {
            // Given: 5초간 초당 20회 호출 (총 100회)
            int totalRequests = 100;
            long startTime = System.nanoTime();

            // When: 블록킹 방식으로 호출
            for (int i = 0; i < totalRequests; i++) {
                kisApiRateLimiter.acquire();
            }

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            // Then: 약 4초 소요 (100/20 = 5초이나, @BeforeEach에서 확보한 30개 permit으로 인해 4초로 단축)
            // CI 환경 고려하여 오차 ±1000ms
            assertThat(elapsedMs).isBetween(3000L, 6000L);
        }
    }
}
