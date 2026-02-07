package com.custom.trader.common.entity;

import com.custom.trader.watchlist.entity.WatchlistGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseEntity의 JPA 콜백 테스트.
 * @PrePersist, @PreUpdate 어노테이션이 정상적으로 동작하는지 검증한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BaseEntity JPA 콜백 테스트")
class BaseEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    private static final String TEST_USER_ID = "testUser";
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 1L;

    @BeforeEach
    void setUp() {
        // 각 테스트는 독립적인 트랜잭션에서 실행됨 (@DataJpaTest의 기본 동작)
        entityManager.clear();
    }

    @Nested
    @DisplayName("onCreate 검증")
    class OnCreateTest {

        @Test
        @DisplayName("Entity 저장 시 createdAt과 updatedAt이 자동 설정된다")
        void 저장_시_타임스탬프_자동_설정() {
            // given
            WatchlistGroup group = WatchlistGroup.builder()
                    .userId(TEST_USER_ID)
                    .groupCode("001")
                    .groupName("테스트그룹")
                    .type("1")
                    .build();

            LocalDateTime beforePersist = LocalDateTime.now();

            // when
            WatchlistGroup savedGroup = entityManager.persistAndFlush(group);

            LocalDateTime afterPersist = LocalDateTime.now();

            // then
            assertThat(savedGroup.getCreatedAt()).isNotNull();
            assertThat(savedGroup.getUpdatedAt()).isNotNull();

            // 타임스탬프가 persist 전후 범위 내에 있는지 검증
            assertThat(savedGroup.getCreatedAt())
                    .isAfterOrEqualTo(beforePersist.minusSeconds(TIMESTAMP_TOLERANCE_SECONDS))
                    .isBeforeOrEqualTo(afterPersist.plusSeconds(TIMESTAMP_TOLERANCE_SECONDS));

            assertThat(savedGroup.getUpdatedAt())
                    .isAfterOrEqualTo(beforePersist.minusSeconds(TIMESTAMP_TOLERANCE_SECONDS))
                    .isBeforeOrEqualTo(afterPersist.plusSeconds(TIMESTAMP_TOLERANCE_SECONDS));
        }

        @Test
        @DisplayName("저장 시 createdAt과 updatedAt이 거의 동시에 설정된다")
        void 저장_시_두_타임스탬프_동시_설정() {
            // given
            WatchlistGroup group = WatchlistGroup.builder()
                    .userId(TEST_USER_ID)
                    .groupCode("002")
                    .groupName("테스트그룹2")
                    .type("1")
                    .build();

            // when
            WatchlistGroup savedGroup = entityManager.persistAndFlush(group);

            // then
            assertThat(savedGroup.getCreatedAt()).isNotNull();
            assertThat(savedGroup.getUpdatedAt()).isNotNull();

            // createdAt과 updatedAt이 1초 이내 차이로 설정되었는지 검증
            long secondsDiff = Math.abs(
                    java.time.Duration.between(
                            savedGroup.getCreatedAt(),
                            savedGroup.getUpdatedAt()
                    ).getSeconds()
            );

            assertThat(secondsDiff).isLessThanOrEqualTo(TIMESTAMP_TOLERANCE_SECONDS);
        }
    }

    @Nested
    @DisplayName("onUpdate 검증")
    class OnUpdateTest {

        @Test
        @DisplayName("Entity 수정 시 updatedAt만 갱신되고 createdAt은 불변이다")
        void 수정_시_updatedAt만_갱신() throws InterruptedException {
            // given: Entity 저장
            WatchlistGroup group = WatchlistGroup.builder()
                    .userId(TEST_USER_ID)
                    .groupCode("001")
                    .groupName("초기이름")
                    .type("1")
                    .build();

            WatchlistGroup savedGroup = entityManager.persistAndFlush(group);
            LocalDateTime originalCreatedAt = savedGroup.getCreatedAt();
            LocalDateTime originalUpdatedAt = savedGroup.getUpdatedAt();

            // 타임스탬프 차이를 보장하기 위해 잠시 대기
            Thread.sleep(100);

            // when: Entity 수정
            savedGroup.updateGroupName("변경된이름");
            entityManager.flush(); // @PreUpdate 트리거
            entityManager.clear(); // 영속성 컨텍스트 초기화

            WatchlistGroup updatedGroup = entityManager.find(WatchlistGroup.class, savedGroup.getId());

            // then
            assertThat(updatedGroup.getCreatedAt())
                    .isEqualTo(originalCreatedAt) // createdAt은 불변
                    .describedAs("createdAt은 수정되지 않아야 한다");

            assertThat(updatedGroup.getUpdatedAt())
                    .isAfter(originalUpdatedAt) // updatedAt만 갱신
                    .describedAs("updatedAt은 갱신되어야 한다");

            assertThat(updatedGroup.getGroupName())
                    .isEqualTo("변경된이름")
                    .describedAs("실제 데이터도 수정되어야 한다");
        }

        @Test
        @DisplayName("여러 번 수정 시에도 createdAt은 항상 불변이다")
        void 여러_번_수정_시_createdAt_불변() throws InterruptedException {
            // given
            WatchlistGroup group = WatchlistGroup.builder()
                    .userId(TEST_USER_ID)
                    .groupCode("002")
                    .groupName("초기이름")
                    .type("1")
                    .build();

            WatchlistGroup savedGroup = entityManager.persistAndFlush(group);
            LocalDateTime originalCreatedAt = savedGroup.getCreatedAt();

            // when: 3번 수정
            Thread.sleep(100);
            savedGroup.updateGroupName("첫번째변경");
            entityManager.flush();

            Thread.sleep(100);
            savedGroup.updateGroupName("두번째변경");
            entityManager.flush();

            Thread.sleep(100);
            savedGroup.updateGroupName("세번째변경");
            entityManager.flush();

            entityManager.clear();
            WatchlistGroup finalGroup = entityManager.find(WatchlistGroup.class, savedGroup.getId());

            // then
            assertThat(finalGroup.getCreatedAt())
                    .isEqualTo(originalCreatedAt)
                    .describedAs("여러 번 수정해도 createdAt은 불변이어야 한다");

            assertThat(finalGroup.getGroupName())
                    .isEqualTo("세번째변경")
                    .describedAs("최종 수정 내용이 반영되어야 한다");
        }
    }

    @Nested
    @DisplayName("타임스탬프 순서 검증")
    class TimestampOrderTest {

        @Test
        @DisplayName("모든 Entity에서 createdAt <= updatedAt이 보장된다")
        void 타임스탬프_순서_보장() throws InterruptedException {
            // given: 여러 Entity 저장 및 수정
            WatchlistGroup group1 = createAndSaveGroup("001", "그룹1");
            WatchlistGroup group2 = createAndSaveGroup("002", "그룹2");
            WatchlistGroup group3 = createAndSaveGroup("003", "그룹3");
            WatchlistGroup group4 = createAndSaveGroup("004", "그룹4");

            // when: 일부 Entity 수정
            Thread.sleep(100);
            group1.updateGroupName("그룹1_수정");
            entityManager.flush();

            Thread.sleep(100);
            group3.updateGroupName("그룹3_수정");
            entityManager.flush();

            entityManager.clear();

            // then: 모든 Entity에서 createdAt <= updatedAt 검증
            WatchlistGroup reloadedGroup1 = entityManager.find(WatchlistGroup.class, group1.getId());
            WatchlistGroup reloadedGroup2 = entityManager.find(WatchlistGroup.class, group2.getId());
            WatchlistGroup reloadedGroup3 = entityManager.find(WatchlistGroup.class, group3.getId());
            WatchlistGroup reloadedGroup4 = entityManager.find(WatchlistGroup.class, group4.getId());

            assertThat(reloadedGroup1.getCreatedAt())
                    .isBeforeOrEqualTo(reloadedGroup1.getUpdatedAt())
                    .describedAs("수정된 그룹1: createdAt <= updatedAt");

            assertThat(reloadedGroup2.getCreatedAt())
                    .isBeforeOrEqualTo(reloadedGroup2.getUpdatedAt())
                    .describedAs("수정되지 않은 그룹2: createdAt <= updatedAt");

            assertThat(reloadedGroup3.getCreatedAt())
                    .isBeforeOrEqualTo(reloadedGroup3.getUpdatedAt())
                    .describedAs("수정된 그룹3: createdAt <= updatedAt");

            assertThat(reloadedGroup4.getCreatedAt())
                    .isBeforeOrEqualTo(reloadedGroup4.getUpdatedAt())
                    .describedAs("수정되지 않은 그룹4: createdAt <= updatedAt");
        }

        @Test
        @DisplayName("수정되지 않은 Entity는 createdAt과 updatedAt이 동일하다")
        void 수정되지_않은_Entity_타임스탬프_동일() {
            // given & when: 저장만 하고 수정하지 않음
            WatchlistGroup group = createAndSaveGroup("001", "테스트그룹");

            entityManager.clear();
            WatchlistGroup reloadedGroup = entityManager.find(WatchlistGroup.class, group.getId());

            // then: createdAt == updatedAt (초 단위 비교)
            long secondsDiff = Math.abs(
                    java.time.Duration.between(
                            reloadedGroup.getCreatedAt(),
                            reloadedGroup.getUpdatedAt()
                    ).getSeconds()
            );

            assertThat(secondsDiff)
                    .isLessThanOrEqualTo(TIMESTAMP_TOLERANCE_SECONDS)
                    .describedAs("수정하지 않은 Entity는 createdAt과 updatedAt이 거의 동일해야 한다");
        }

        @Test
        @DisplayName("수정된 Entity는 updatedAt > createdAt이다")
        void 수정된_Entity_updatedAt_큼() throws InterruptedException {
            // given: Entity 저장
            WatchlistGroup group = createAndSaveGroup("005", "그룹5");

            // when: 수정
            Thread.sleep(100);
            group.updateGroupName("그룹5_수정");
            entityManager.flush();

            entityManager.clear();
            WatchlistGroup reloadedGroup = entityManager.find(WatchlistGroup.class, group.getId());

            // then: updatedAt > createdAt
            assertThat(reloadedGroup.getUpdatedAt())
                    .isAfter(reloadedGroup.getCreatedAt())
                    .describedAs("수정된 Entity는 updatedAt이 createdAt보다 커야 한다");
        }
    }

    // 테스트 헬퍼 메서드
    private WatchlistGroup createAndSaveGroup(String groupCode, String groupName) {
        WatchlistGroup group = WatchlistGroup.builder()
                .userId(TEST_USER_ID)
                .groupCode(groupCode)
                .groupName(groupName)
                .type("1")
                .build();
        return entityManager.persistAndFlush(group);
    }
}
