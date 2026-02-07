package com.custom.trader.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * ShedLock 분산 잠금 설정.
 *
 * <p><b>목적:</b> 다중 인스턴스(Pod/서버) 환경에서 스케줄된 작업의 중복 실행 방지</p>
 *
 * <p><b>기본 잠금 시간:</b></p>
 * <ul>
 *   <li>기본 최대 잠금: 30분 (defaultLockAtMostFor)</li>
 *   <li>기본 최소 잠금: 5분 (defaultLockAtLeastFor)</li>
 *   <li>각 스케줄 메서드의 @SchedulerLock에서 개별 설정 가능</li>
 * </ul>
 *
 * <p><b>잠금 메커니즘:</b></p>
 * <ul>
 *   <li>Redis를 기반으로 분산 잠금 구현</li>
 *   <li>환경 식별자: "trader" (ShedLock 테이블에서 구분)</li>
 *   <li>원자적 연산: Redis SET NX (Atomic Set If Not Exists)</li>
 * </ul>
 *
 * <p><b>사용 예시:</b></p>
 * <pre>
 * {@code
 * @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Seoul")
 * @SchedulerLock(name = "backfillHistoricalPrices",
 *                lockAtMostFor = "PT6H",
 *                lockAtLeastFor = "PT3H")
 * public void backfillHistoricalPrices() { ... }
 * }
 * </pre>
 *
 * @see net.javacrumbs.shedlock.spring.annotation.SchedulerLock 개별 스케줄 메서드의 잠금 설정
 * @see WatchlistScheduler 관심종목 동기화 스케줄러
 * @see StockPriceScheduler 주식 가격 수집 스케줄러
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "30m", defaultLockAtLeastFor = "5m")
public class ShedLockConfig {

    /**
     * Redis 기반 ShedLock 잠금 제공자 생성.
     *
     * <p><b>구현 방식:</b></p>
     * <ul>
     *   <li>Redis를 잠금 저장소로 사용</li>
     *   <li>환경 식별자 "trader"로 여러 애플리케이션 간 구분</li>
     *   <li>ex) prod, staging, dev 등 각 환경별로 다른 식별자 사용 가능</li>
     * </ul>
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return RedisLockProvider 인스턴스
     */
    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "trader");
    }
}
