package com.custom.trader.watchlist.mapper;

import com.custom.trader.common.enums.AssetType;
import com.custom.trader.common.enums.MarketCode;
import com.custom.trader.kis.dto.watchlist.WatchlistGroupResponse;
import com.custom.trader.kis.dto.watchlist.WatchlistStockResponse;
import com.custom.trader.watchlist.entity.WatchlistGroup;
import com.custom.trader.watchlist.entity.WatchlistStock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WatchlistMapper 테스트")
class WatchlistMapperTest {

    private static final String TEST_USER_ID = "testUser";

    @Nested
    @DisplayName("toWatchlistStock 메소드")
    class ToWatchlistStock {

        @Test
        @DisplayName("API 종목 DTO → Entity 변환 성공")
        void API_종목_변환_성공() {
            // given
            String stockCode = "005930";
            var dto = new WatchlistStockResponse.StockItem(
                    "J",
                    stockCode,
                    "삼성전자",
                    "KRX"
            );

            // when
            WatchlistStock stock = WatchlistMapper.toWatchlistStock(stockCode, dto);

            // then
            assertThat(stock.getStockCode()).isEqualTo("005930");
            assertThat(stock.getStockName()).isEqualTo("삼성전자");
            assertThat(stock.getMarketCode()).isEqualTo(MarketCode.KRX);
            assertThat(stock.getAssetType()).isEqualTo(AssetType.DOMESTIC_STOCK);
            assertThat(stock.isBackfillCompleted()).isFalse(); // 기본값
        }

        @Test
        @DisplayName("해외 주식 변환 - NYSE")
        void 해외_주식_변환_NYSE() {
            // given
            String stockCode = "AAPL";
            var dto = new WatchlistStockResponse.StockItem(
                    "FS",
                    stockCode,
                    "Apple Inc",
                    "NYS"
            );

            // when
            WatchlistStock stock = WatchlistMapper.toWatchlistStock(stockCode, dto);

            // then
            assertThat(stock.getStockCode()).isEqualTo("AAPL");
            assertThat(stock.getStockName()).isEqualTo("Apple Inc");
            assertThat(stock.getMarketCode()).isEqualTo(MarketCode.NYS);
            assertThat(stock.getAssetType()).isEqualTo(AssetType.OVERSEAS_STOCK);
        }

        @Test
        @DisplayName("해외 주식 변환 - NASDAQ")
        void 해외_주식_변환_NASDAQ() {
            // given
            String stockCode = "TSLA";
            var dto = new WatchlistStockResponse.StockItem(
                    "FS",
                    stockCode,
                    "Tesla Inc",
                    "NAS"
            );

            // when
            WatchlistStock stock = WatchlistMapper.toWatchlistStock(stockCode, dto);

            // then
            assertThat(stock.getStockCode()).isEqualTo("TSLA");
            assertThat(stock.getStockName()).isEqualTo("Tesla Inc");
            assertThat(stock.getMarketCode()).isEqualTo(MarketCode.NAS);
            assertThat(stock.getAssetType()).isEqualTo(AssetType.OVERSEAS_STOCK);
        }

        @Test
        @DisplayName("국내 지수 변환")
        void 국내_지수_변환() {
            // given
            String stockCode = "0001";
            var dto = new WatchlistStockResponse.StockItem(
                    "U",
                    stockCode,
                    "코스피지수",
                    "KRX"
            );

            // when
            WatchlistStock stock = WatchlistMapper.toWatchlistStock(stockCode, dto);

            // then
            assertThat(stock.getStockCode()).isEqualTo("0001");
            assertThat(stock.getStockName()).isEqualTo("코스피지수");
            assertThat(stock.getMarketCode()).isEqualTo(MarketCode.KRX);
            assertThat(stock.getAssetType()).isEqualTo(AssetType.DOMESTIC_INDEX);
        }

        @Test
        @DisplayName("알 수 없는 exchCode는 KRX 기본값")
        void 알_수_없는_exchCode_기본값() {
            // given
            String stockCode = "005930";
            var dto = new WatchlistStockResponse.StockItem(
                    "J",
                    stockCode,
                    "삼성전자",
                    "UNKNOWN_EXCH"
            );

            // when
            WatchlistStock stock = WatchlistMapper.toWatchlistStock(stockCode, dto);

            // then
            assertThat(stock.getMarketCode()).isEqualTo(MarketCode.KRX); // 기본값
        }

        @Test
        @DisplayName("알 수 없는 fidMrktClsCode는 DOMESTIC_STOCK 기본값")
        void 알_수_없는_fidMrktClsCode_기본값() {
            // given
            String stockCode = "005930";
            var dto = new WatchlistStockResponse.StockItem(
                    "UNKNOWN_TYPE",
                    stockCode,
                    "삼성전자",
                    "KRX"
            );

            // when
            WatchlistStock stock = WatchlistMapper.toWatchlistStock(stockCode, dto);

            // then
            assertThat(stock.getAssetType()).isEqualTo(AssetType.DOMESTIC_STOCK); // 기본값
        }

        @Test
        @DisplayName("null fidMrktClsCode는 DOMESTIC_STOCK 기본값")
        void null_fidMrktClsCode_기본값() {
            // given
            String stockCode = "005930";
            var dto = new WatchlistStockResponse.StockItem(
                    null,
                    stockCode,
                    "삼성전자",
                    "KRX"
            );

            // when
            WatchlistStock stock = WatchlistMapper.toWatchlistStock(stockCode, dto);

            // then
            assertThat(stock.getAssetType()).isEqualTo(AssetType.DOMESTIC_STOCK); // 기본값
        }
    }

    @Nested
    @DisplayName("toWatchlistGroup 메소드")
    class ToWatchlistGroup {

        @Test
        @DisplayName("API 그룹 DTO → Entity 변환 성공")
        void API_그룹_변환_성공() {
            // given
            var dto = new WatchlistGroupResponse.GroupItem("001", "관심그룹1");

            // when
            WatchlistGroup group = WatchlistMapper.toWatchlistGroup(TEST_USER_ID, dto);

            // then
            assertThat(group.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(group.getGroupCode()).isEqualTo("001");
            assertThat(group.getGroupName()).isEqualTo("관심그룹1");
            assertThat(group.getType()).isEqualTo("1");
            assertThat(group.getStocks()).isEmpty(); // 초기 상태
        }

        @Test
        @DisplayName("null groupCode 처리")
        void null_groupCode_처리() {
            // given
            var dto = new WatchlistGroupResponse.GroupItem(null, "Null코드그룹");

            // when
            WatchlistGroup group = WatchlistMapper.toWatchlistGroup(TEST_USER_ID, dto);

            // then
            assertThat(group.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(group.getGroupCode()).isNull();
            assertThat(group.getGroupName()).isEqualTo("Null코드그룹");
        }

        @Test
        @DisplayName("빈 문자열 groupName 처리")
        void 빈_문자열_groupName_처리() {
            // given
            var dto = new WatchlistGroupResponse.GroupItem("001", "");

            // when
            WatchlistGroup group = WatchlistMapper.toWatchlistGroup(TEST_USER_ID, dto);

            // then
            assertThat(group.getGroupCode()).isEqualTo("001");
            assertThat(group.getGroupName()).isEmpty();
        }
    }

    @Nested
    @DisplayName("buildApiStockMap 메소드")
    class BuildApiStockMap {

        @Test
        @DisplayName("정상 종목 리스트 → Map 변환")
        void 정상_종목_리스트_변환() {
            // given
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX"),
                    new WatchlistStockResponse.StockItem("J", "000660", "SK하이닉스", "KRX")
            );

            // when
            Map<String, WatchlistStockResponse.StockItem> stockMap =
                    WatchlistMapper.buildApiStockMap(apiStocks, "001");

            // then
            assertThat(stockMap).hasSize(2);
            assertThat(stockMap).containsKey("005930");
            assertThat(stockMap).containsKey("000660");
            assertThat(stockMap.get("005930").htsKorIsnm()).isEqualTo("삼성전자");
        }

        @Test
        @DisplayName("null stockCode 필터링")
        void null_stockCode_필터링() {
            // given
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", null, "Null종목", "KRX"),
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );

            // when
            Map<String, WatchlistStockResponse.StockItem> stockMap =
                    WatchlistMapper.buildApiStockMap(apiStocks, "001");

            // then
            assertThat(stockMap).hasSize(1); // null 필터링됨
            assertThat(stockMap).containsKey("005930");
            assertThat(stockMap).doesNotContainKey(null);
        }

        @Test
        @DisplayName("빈 문자열 stockCode 필터링")
        void 빈_문자열_stockCode_필터링() {
            // given
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "", "빈문자열종목", "KRX"),
                    new WatchlistStockResponse.StockItem("J", "   ", "공백종목", "KRX"),
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX")
            );

            // when
            Map<String, WatchlistStockResponse.StockItem> stockMap =
                    WatchlistMapper.buildApiStockMap(apiStocks, "001");

            // then
            assertThat(stockMap).hasSize(1); // 빈 문자열/공백 필터링됨
            assertThat(stockMap).containsKey("005930");
        }

        @Test
        @DisplayName("중복 stockCode - 나중 값 우선")
        void 중복_stockCode_나중_값_우선() {
            // given
            var apiStocks = List.of(
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자", "KRX"),
                    new WatchlistStockResponse.StockItem("J", "005930", "삼성전자(중복)", "KRX")
            );

            // when
            Map<String, WatchlistStockResponse.StockItem> stockMap =
                    WatchlistMapper.buildApiStockMap(apiStocks, "001");

            // then
            assertThat(stockMap).hasSize(1); // 중복 제거
            assertThat(stockMap.get("005930").htsKorIsnm()).isEqualTo("삼성전자(중복)"); // 나중 값 우선
        }

        @Test
        @DisplayName("빈 리스트 처리")
        void 빈_리스트_처리() {
            // given
            var apiStocks = List.<WatchlistStockResponse.StockItem>of();

            // when
            Map<String, WatchlistStockResponse.StockItem> stockMap =
                    WatchlistMapper.buildApiStockMap(apiStocks, "001");

            // then
            assertThat(stockMap).isEmpty();
        }
    }

    @Nested
    @DisplayName("buildDbStockMap 메소드")
    class BuildDbStockMap {

        @Test
        @DisplayName("정상 엔티티 리스트 → Map 변환")
        void 정상_엔티티_리스트_변환() {
            // given
            var stock1 = WatchlistStock.builder()
                    .stockCode("005930")
                    .stockName("삼성전자")
                    .marketCode(MarketCode.KRX)
                    .assetType(AssetType.DOMESTIC_STOCK)
                    .build();
            var stock2 = WatchlistStock.builder()
                    .stockCode("000660")
                    .stockName("SK하이닉스")
                    .marketCode(MarketCode.KRX)
                    .assetType(AssetType.DOMESTIC_STOCK)
                    .build();

            // when
            Map<String, WatchlistStock> stockMap =
                    WatchlistMapper.buildDbStockMap(List.of(stock1, stock2));

            // then
            assertThat(stockMap).hasSize(2);
            assertThat(stockMap).containsKey("005930");
            assertThat(stockMap).containsKey("000660");
            assertThat(stockMap.get("005930").getStockName()).isEqualTo("삼성전자");
        }

        @Test
        @DisplayName("빈 리스트 처리")
        void 빈_리스트_처리() {
            // given
            var stocks = List.<WatchlistStock>of();

            // when
            Map<String, WatchlistStock> stockMap = WatchlistMapper.buildDbStockMap(stocks);

            // then
            assertThat(stockMap).isEmpty();
        }
    }

    @Nested
    @DisplayName("Utility 클래스 보안 테스트")
    class UtilityClassSecurity {

        @Test
        @DisplayName("생성자 호출 시 예외 발생")
        void 생성자_호출_불가() {
            // when & then
            assertThatThrownBy(() -> {
                var constructor = WatchlistMapper.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            })
            .getCause()
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("Utility class cannot be instantiated");
        }
    }
}
