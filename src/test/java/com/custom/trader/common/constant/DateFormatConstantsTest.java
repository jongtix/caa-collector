package com.custom.trader.common.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DateFormatConstants 테스트.
 *
 * <p>KIS API 날짜 포맷 파싱 로직의 엣지 케이스를 검증합니다.
 * parseDate() 메서드의 정상 동작, 경계값, 잘못된 입력에 대한 예외 처리를 확인합니다.</p>
 *
 * @see DateFormatConstants
 */
@DisplayName("DateFormatConstants")
class DateFormatConstantsTest {

    @Test
    @DisplayName("정상 케이스: yyyyMMdd 형식 파싱")
    void parseDate_정상() {
        // given
        var dateStr = "20240125";

        // when
        var result = DateFormatConstants.parseDate(dateStr);

        // then
        assertThat(result).isEqualTo(LocalDate.of(2024, 1, 25));
    }

    @Test
    @DisplayName("null 입력: NullPointerException")
    void parseDate_null() {
        // when & then
        assertThatThrownBy(() -> DateFormatConstants.parseDate(null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("빈 문자열 입력: DateTimeParseException")
    void parseDate_empty(String dateStr) {
        // when & then
        assertThatThrownBy(() -> DateFormatConstants.parseDate(dateStr))
                .isInstanceOf(DateTimeParseException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-01-25", "2024/01/25", "202４0125"})
    @DisplayName("잘못된 포맷: 하이픈, 슬래시, 전각 숫자")
    void parseDate_잘못된_포맷(String dateStr) {
        // when & then
        assertThatThrownBy(() -> DateFormatConstants.parseDate(dateStr))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    @DisplayName("잘못된 포맷: 2자리 일자 (20240125 대신 202401025)")
    void parseDate_잘못된_포맷_일자2자리() {
        // given
        var dateStr = "202401025"; // 9자리 (8자리가 아님)

        // when & then
        assertThatThrownBy(() -> DateFormatConstants.parseDate(dateStr))
                .isInstanceOf(DateTimeParseException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"20241301", "20240132"})
    @DisplayName("범위 초과: 13월, 32일")
    void parseDate_범위초과(String dateStr) {
        // when & then
        assertThatThrownBy(() -> DateFormatConstants.parseDate(dateStr))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    @DisplayName("범위 초과: 평년 2월 29일 (ResolverStyle.SMART 자동 조정)")
    void parseDate_범위초과_평년2월29일() {
        // given
        var dateStr = "20230229"; // 2023년은 평년

        // when
        // DateTimeFormatter.ofPattern()은 기본적으로 ResolverStyle.SMART를 사용하여
        // 잘못된 날짜를 이전 유효한 날짜로 조정함 (2023-02-29 → 2023-02-28)
        // 엄격한 검증이 필요하면 ResolverStyle.STRICT를 사용해야 함
        var result = DateFormatConstants.parseDate(dateStr);

        // then
        // 2023-02-29는 2023-02-28로 자동 조정됨 (SMART 모드의 동작)
        assertThat(result).isEqualTo(LocalDate.of(2023, 2, 28));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024#0125", "2024 01 25"})
    @DisplayName("특수문자 포함: #, 공백")
    void parseDate_특수문자_포함(String dateStr) {
        // when & then
        assertThatThrownBy(() -> DateFormatConstants.parseDate(dateStr))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    @DisplayName("경계값: 20000101 (최소)")
    void parseDate_경계값_최소() {
        // given
        var dateStr = "20000101";

        // when
        var result = DateFormatConstants.parseDate(dateStr);

        // then
        assertThat(result).isEqualTo(LocalDate.of(2000, 1, 1));
    }

    @Test
    @DisplayName("경계값: 99991231 (최대)")
    void parseDate_경계값_최대() {
        // given
        var dateStr = "99991231";

        // when
        var result = DateFormatConstants.parseDate(dateStr);

        // then
        assertThat(result).isEqualTo(LocalDate.of(9999, 12, 31));
    }

    @Test
    @DisplayName("윤년 2월 29일: 파싱 성공")
    void parseDate_윤년2월29일() {
        // given
        var dateStr = "20240229"; // 2024년은 윤년

        // when
        var result = DateFormatConstants.parseDate(dateStr);

        // then
        assertThat(result).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    @DisplayName("DEFAULT_START_DATE는 1900-01-01")
    void defaultStartDate() {
        // when & then
        assertThat(DateFormatConstants.DEFAULT_START_DATE)
                .isEqualTo(LocalDate.of(1900, 1, 1));
    }

    @Test
    @DisplayName("KST_ZONE은 Asia/Seoul")
    void kstZone() {
        // when & then
        assertThat(DateFormatConstants.KST_ZONE).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("KST_ZONE_ID는 Asia/Seoul ZoneId")
    void kstZoneId() {
        // when & then
        assertThat(DateFormatConstants.KST_ZONE_ID.getId()).isEqualTo("Asia/Seoul");
    }
}
