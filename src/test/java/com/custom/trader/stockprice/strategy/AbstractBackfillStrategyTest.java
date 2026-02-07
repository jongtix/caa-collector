package com.custom.trader.stockprice.strategy;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
import com.custom.trader.watchlist.entity.WatchlistStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.custom.trader.stockprice.constant.StockPriceConstants.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AbstractBackfillStrategy 추상 클래스 테스트")
class AbstractBackfillStrategyTest {

    private WatchlistStock testStock;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        testStock = WatchlistStock.builder()
                .stockCode("TEST001")
                .stockName("테스트 주식")
                .assetType(AssetType.DOMESTIC_STOCK)
                .marketCode(MarketCode.KRX)
                .build();

        startDate = LocalDate.of(2024, 1, 1);
        endDate = LocalDate.of(2024, 1, 31);
    }

    @Nested
    @DisplayName("백필 페이징 로직 검증")
    class BackfillPagingLogic {

        @Test
        @DisplayName("단일 페이지 데이터 처리")
        void 단일_페이지_데이터_처리() {
            // given
            var strategy = new TestBackfillStrategy();
            strategy.setTestData(createPriceItems(10, startDate)); // PAGE_SIZE 미만

            // when
            strategy.backfillHistoricalPrices(testStock, startDate, endDate);

            // then
            assertThat(strategy.getTotalFetchCount()).isEqualTo(1);
            assertThat(strategy.getTotalSavedCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("여러 페이지 데이터 처리")
        void 여러_페이지_데이터_처리() {
            // given
            var strategy = new TestBackfillStrategy();
            // 첫 페이지: PAGE_SIZE
            strategy.setTestData(createPriceItems(PAGE_SIZE, startDate));
            // 두 번째 페이지: PAGE_SIZE 미만
            strategy.addTestData(createPriceItems(10, startDate.minusDays(PAGE_SIZE)));

            // when
            strategy.backfillHistoricalPrices(testStock, startDate.minusDays(PAGE_SIZE + 10), endDate);

            // then
            assertThat(strategy.getTotalFetchCount()).isEqualTo(2);
            assertThat(strategy.getTotalSavedCount()).isEqualTo(PAGE_SIZE + 10);
        }

        @Test
        @DisplayName("빈 데이터 반환 시 백필 중단")
        void 빈_데이터_반환시_백필_중단() {
            // given
            var strategy = new TestBackfillStrategy();
            strategy.setTestData(List.of()); // 빈 리스트

            // when
            strategy.backfillHistoricalPrices(testStock, startDate, endDate);

            // then
            assertThat(strategy.getTotalFetchCount()).isEqualTo(1);
            assertThat(strategy.getTotalSavedCount()).isZero();
        }

        @Test
        @DisplayName("날짜 추출 실패 시 예외 발생")
        void 날짜_추출_실패시_예외_발생() {
            // given
            var strategy = new TestBackfillStrategy();
            strategy.setTestData(createPriceItems(PAGE_SIZE, startDate));
            strategy.setThrowExceptionOnExtractDate(true);

            // when & then
            assertThatThrownBy(() -> strategy.backfillHistoricalPrices(testStock, startDate, endDate))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Test exception");
        }
    }

    @Nested
    @DisplayName("엣지 케이스 검증")
    class EdgeCaseTest {

        @Test
        @DisplayName("PAGE_SIZE 정확히 100개이고 다음 페이지 빈 리스트면 2회 호출 후 종료")
        void PAGE_SIZE_정확히_100개_다음페이지_빈리스트() {
            // given
            var strategy = new TestBackfillStrategy();
            // 첫 페이지: 정확히 PAGE_SIZE (100개)
            strategy.setTestData(createPriceItems(PAGE_SIZE, startDate));
            // 두 번째 페이지: 빈 리스트
            strategy.addTestData(List.of());

            // when
            strategy.backfillHistoricalPrices(testStock, startDate.minusDays(PAGE_SIZE + 10), endDate);

            // then
            assertThat(strategy.getTotalFetchCount()).isEqualTo(2);
            assertThat(strategy.getTotalSavedCount()).isEqualTo(PAGE_SIZE);
        }

        @Test
        @DisplayName("날짜가 역순이 아닌 경우 min() 올바르게 추출")
        void 날짜_비정렬_데이터_min_올바르게_추출() {
            // given
            var strategy = new TestBackfillStrategy();
            // 날짜 순서: 2024-01-05, 2024-01-03, 2024-01-10, 2024-01-01 (비정렬)
            List<TestPriceItem> unorderedPrices = List.of(
                    new TestPriceItem(LocalDate.of(2024, 1, 5)),
                    new TestPriceItem(LocalDate.of(2024, 1, 3)),
                    new TestPriceItem(LocalDate.of(2024, 1, 10)),
                    new TestPriceItem(LocalDate.of(2024, 1, 1))  // 최소값
            );
            strategy.setTestData(unorderedPrices);

            // when
            strategy.backfillHistoricalPrices(testStock, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

            // then
            assertThat(strategy.getTotalFetchCount()).isEqualTo(1);
            assertThat(strategy.getTotalSavedCount()).isEqualTo(4);
            // min() 추출이 정상 동작하여 예외가 발생하지 않아야 함
        }

        @Test
        @DisplayName("같은 날짜 데이터 여러 개 있을 때 중복 제거 없이 모두 저장")
        void 같은_날짜_여러개_모두_저장() {
            // given
            var strategy = new TestBackfillStrategy();
            LocalDate sameDate = LocalDate.of(2024, 1, 5);
            List<TestPriceItem> duplicateDatePrices = List.of(
                    new TestPriceItem(sameDate),
                    new TestPriceItem(sameDate),
                    new TestPriceItem(sameDate)
            );
            strategy.setTestData(duplicateDatePrices);

            // when
            strategy.backfillHistoricalPrices(testStock, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

            // then
            assertThat(strategy.getTotalSavedCount()).isEqualTo(3);
            // 중복 날짜도 모두 저장됨 (비즈니스 로직에서 중복 처리는 별도)
        }

        @Test
        @DisplayName("무한 루프 방지: currentEndDate가 startDate 이전이 되면 종료")
        void 무한_루프_방지_currentEndDate가_startDate_이전() {
            // given
            var strategy = new TestBackfillStrategy();
            LocalDate requestStartDate = LocalDate.of(2024, 1, 1);
            LocalDate requestEndDate = LocalDate.of(2024, 1, 31);

            // 첫 페이지: PAGE_SIZE (2024-01-31 ~ 2023-12-23, 100일치)
            strategy.setTestData(createPriceItems(PAGE_SIZE, requestEndDate));
            // 두 번째 페이지 조회 시 currentEndDate = 2023-12-22
            // 이는 startDate(2024-01-01)보다 이전이므로 while 조건에서 종료

            // when
            strategy.backfillHistoricalPrices(testStock, requestStartDate, requestEndDate);

            // then
            // while 조건 (!currentEndDate.isBefore(startDate))에 의해 첫 페이지만 조회하고 종료
            assertThat(strategy.getTotalFetchCount()).isEqualTo(1);
            assertThat(strategy.getTotalSavedCount()).isEqualTo(PAGE_SIZE);
        }
    }

    @Nested
    @DisplayName("Template Method Pattern 검증")
    class TemplateMethodPattern {

        @Test
        @DisplayName("fetchPrices 메서드 호출 검증")
        void fetchPrices_메서드_호출_검증() {
            // given
            var strategy = new TestBackfillStrategy();
            strategy.setTestData(createPriceItems(10, startDate));

            // when
            strategy.backfillHistoricalPrices(testStock, startDate, endDate);

            // then
            assertThat(strategy.isFetchPricesCalled()).isTrue();
        }

        @Test
        @DisplayName("savePrices 메서드 호출 검증")
        void savePrices_메서드_호출_검증() {
            // given
            var strategy = new TestBackfillStrategy();
            strategy.setTestData(createPriceItems(10, startDate));

            // when
            strategy.backfillHistoricalPrices(testStock, startDate, endDate);

            // then
            assertThat(strategy.isSavePricesCalled()).isTrue();
        }

        @Test
        @DisplayName("extractDate 메서드 호출 검증")
        void extractDate_메서드_호출_검증() {
            // given
            var strategy = new TestBackfillStrategy();
            strategy.setTestData(createPriceItems(PAGE_SIZE, startDate));

            // when
            strategy.backfillHistoricalPrices(testStock, startDate, endDate);

            // then
            assertThat(strategy.isExtractDateCalled()).isTrue();
        }

        @Test
        @DisplayName("getAssetTypeName 메서드 호출 검증")
        void getAssetTypeName_메서드_호출_검증() {
            // given
            var strategy = new TestBackfillStrategy();
            strategy.setTestData(createPriceItems(10, startDate));

            // when
            strategy.backfillHistoricalPrices(testStock, startDate, endDate);

            // then
            assertThat(strategy.isGetAssetTypeNameCalled()).isTrue();
        }
    }

    /**
     * 테스트용 구체 클래스.
     */
    private static class TestBackfillStrategy extends AbstractBackfillStrategy<TestPriceItem> {

        private final List<List<TestPriceItem>> testDataPages = new ArrayList<>();
        private int currentPageIndex = 0;
        private int totalFetchCount = 0;
        private int totalSavedCount = 0;
        private boolean throwExceptionOnExtractDate = false;

        private boolean fetchPricesCalled = false;
        private boolean savePricesCalled = false;
        private boolean extractDateCalled = false;
        private boolean getAssetTypeNameCalled = false;

        void setTestData(List<TestPriceItem> data) {
            testDataPages.clear();
            testDataPages.add(data);
            currentPageIndex = 0;
        }

        void addTestData(List<TestPriceItem> data) {
            testDataPages.add(data);
        }

        void setThrowExceptionOnExtractDate(boolean throwException) {
            this.throwExceptionOnExtractDate = throwException;
        }

        @Override
        public int collectDailyPrice(WatchlistStock stock, LocalDate startDate, LocalDate endDate) {
            return 0; // 백필 로직만 테스트
        }

        @Override
        protected List<TestPriceItem> fetchPrices(WatchlistStock stock, LocalDate startDate, LocalDate endDate) {
            fetchPricesCalled = true;
            totalFetchCount++;
            if (currentPageIndex < testDataPages.size()) {
                return testDataPages.get(currentPageIndex++);
            }
            return List.of();
        }

        @Override
        protected int savePrices(WatchlistStock stock, List<TestPriceItem> prices) {
            savePricesCalled = true;
            int saved = prices.size();
            totalSavedCount += saved;
            return saved;
        }

        @Override
        protected LocalDate extractDate(TestPriceItem price) {
            extractDateCalled = true;
            if (throwExceptionOnExtractDate) {
                throw new RuntimeException("Test exception");
            }
            return price.date;
        }

        @Override
        protected String getAssetTypeName() {
            getAssetTypeNameCalled = true;
            return "test";
        }

        int getTotalFetchCount() {
            return totalFetchCount;
        }

        int getTotalSavedCount() {
            return totalSavedCount;
        }

        boolean isFetchPricesCalled() {
            return fetchPricesCalled;
        }

        boolean isSavePricesCalled() {
            return savePricesCalled;
        }

        boolean isExtractDateCalled() {
            return extractDateCalled;
        }

        boolean isGetAssetTypeNameCalled() {
            return getAssetTypeNameCalled;
        }
    }

    /**
     * 테스트용 가격 데이터.
     */
    private record TestPriceItem(LocalDate date) {
    }

    /**
     * 테스트 데이터 생성 헬퍼.
     */
    private List<TestPriceItem> createPriceItems(int count, LocalDate startDate) {
        List<TestPriceItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(new TestPriceItem(startDate.minusDays(i)));
        }
        return items;
    }
}
