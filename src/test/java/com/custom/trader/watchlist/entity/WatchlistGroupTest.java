package com.custom.trader.watchlist.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WatchlistGroup Entity")
class WatchlistGroupTest {

    @Nested
    @DisplayName("equals/hashCode 검증")
    class EqualsAndHashCode {

        @Test
        @DisplayName("자기_자신과_비교_시_true를_반환한다")
        void 자기_자신과_비교_시_true를_반환한다() {
            // Given
            WatchlistGroup group = WatchlistGroup.builder()
                .userId("12345678")
                .groupCode("001")
                .groupName("테스트그룹")
                .type("01")
                .build();

            // When & Then
            assertThat(group).isEqualTo(group);
        }

        @Test
        @DisplayName("null과_비교_시_false를_반환한다")
        void null과_비교_시_false를_반환한다() {
            // Given
            WatchlistGroup group = WatchlistGroup.builder()
                .userId("12345678")
                .groupCode("001")
                .groupName("테스트그룹")
                .type("01")
                .build();

            // When & Then
            assertThat(group).isNotEqualTo(null);
        }

        @Test
        @DisplayName("id가_null인_두_인스턴스는_서로_다르다")
        void id가_null인_두_인스턴스는_서로_다르다() {
            // Given
            WatchlistGroup group1 = WatchlistGroup.builder()
                .userId("12345678")
                .groupCode("001")
                .groupName("테스트그룹1")
                .type("01")
                .build();

            WatchlistGroup group2 = WatchlistGroup.builder()
                .userId("12345678")
                .groupCode("001")
                .groupName("테스트그룹2")
                .type("01")
                .build();

            // When & Then
            assertThat(group1.getId()).isNull();
            assertThat(group2.getId()).isNull();
            assertThat(group1).isNotEqualTo(group2);
        }

        @Test
        @DisplayName("동일한_id를_가진_인스턴스는_동등하다")
        void 동일한_id를_가진_인스턴스는_동등하다() {
            // Given
            WatchlistGroup group1 = WatchlistGroup.builder()
                .userId("12345678")
                .groupCode("001")
                .groupName("테스트그룹1")
                .type("01")
                .build();
            ReflectionTestUtils.setField(group1, "id", 1L);

            WatchlistGroup group2 = WatchlistGroup.builder()
                .userId("87654321")
                .groupCode("002")
                .groupName("테스트그룹2")
                .type("02")
                .build();
            ReflectionTestUtils.setField(group2, "id", 1L);

            // When & Then
            assertThat(group1).isEqualTo(group2);
        }

        @Test
        @DisplayName("다른_id를_가진_인스턴스는_동등하지_않다")
        void 다른_id를_가진_인스턴스는_동등하지_않다() {
            // Given
            WatchlistGroup group1 = WatchlistGroup.builder()
                .userId("12345678")
                .groupCode("001")
                .groupName("테스트그룹")
                .type("01")
                .build();
            ReflectionTestUtils.setField(group1, "id", 1L);

            WatchlistGroup group2 = WatchlistGroup.builder()
                .userId("12345678")
                .groupCode("001")
                .groupName("테스트그룹")
                .type("01")
                .build();
            ReflectionTestUtils.setField(group2, "id", 2L);

            // When & Then
            assertThat(group1).isNotEqualTo(group2);
        }

        @Test
        @DisplayName("hashCode는_클래스_타입에_의존하여_일관성을_유지한다")
        void hashCode는_클래스_타입에_의존하여_일관성을_유지한다() {
            // Given
            WatchlistGroup group1 = WatchlistGroup.builder()
                .userId("12345678")
                .groupCode("001")
                .groupName("테스트그룹1")
                .type("01")
                .build();
            ReflectionTestUtils.setField(group1, "id", 1L);

            WatchlistGroup group2 = WatchlistGroup.builder()
                .userId("87654321")
                .groupCode("002")
                .groupName("테스트그룹2")
                .type("02")
                .build();
            ReflectionTestUtils.setField(group2, "id", 2L);

            // When & Then
            assertThat(group1.hashCode()).isEqualTo(group2.hashCode());
        }

        @Test
        @DisplayName("id가_null인_인스턴스도_hashCode를_반환한다")
        void id가_null인_인스턴스도_hashCode를_반환한다() {
            // Given
            WatchlistGroup group = WatchlistGroup.builder()
                .userId("12345678")
                .groupCode("001")
                .groupName("테스트그룹")
                .type("01")
                .build();

            // When & Then
            assertThat(group.getId()).isNull();
            assertThat(group.hashCode()).isNotNull();
        }
    }
}
