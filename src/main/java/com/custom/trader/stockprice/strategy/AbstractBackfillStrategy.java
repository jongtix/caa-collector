package com.custom.trader.stockprice.strategy;

import com.custom.trader.watchlist.entity.WatchlistStock;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static com.custom.trader.stockprice.constant.StockPriceConstants.PAGE_SIZE;

/**
 * 백필 페이징 로직을 공통화한 추상 클래스.
 *
 * <p>Template Method Pattern을 사용하여 백필 페이징 로직을 추상화하고,
 * 각 Strategy 구현체는 API 호출, 저장, 날짜 추출 등의 차이점만 구현합니다.</p>
 *
 * @param <P> 가격 데이터 타입 (예: DomesticStockDailyPriceResponse.PriceItem)
 */
@Slf4j
public abstract class AbstractBackfillStrategy<P> implements StockPriceStrategy {

    /**
     * 일간 가격 수집 공통 로직.
     *
     * <p>이 메서드는 Template Method Pattern의 템플릿 메서드로,
     * 일간 가격 수집 프로세스의 흐름을 제어합니다.</p>
     *
     * @param stock 대상 종목
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 저장된 데이터 개수
     */
    @Override
    public int collectDailyPrice(WatchlistStock stock, LocalDate startDate, LocalDate endDate) {
        List<P> prices = fetchPrices(stock, startDate, endDate);
        int saved = savePrices(stock, prices);
        log.debug("Saved {} {} prices for: {}", saved, getAssetTypeName(), stock.getStockCode());
        return saved;
    }

    /**
     * 백필 공통 로직: 페이징 처리 및 반복 수집.
     *
     * <p>이 메서드는 Template Method Pattern의 템플릿 메서드로,
     * 전체 백필 프로세스의 흐름을 제어합니다.</p>
     *
     * @param stock 대상 종목
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     */
    @Override
    public void backfillHistoricalPrices(WatchlistStock stock, LocalDate startDate, LocalDate endDate) {
        String code = stock.getStockCode();
        LocalDate currentEndDate = endDate;
        int totalSaved = 0;

        while (!currentEndDate.isBefore(startDate)) {
            List<P> prices = fetchPrices(stock, startDate, currentEndDate);

            if (prices.isEmpty()) {
                break;
            }

            int savedCount = savePrices(stock, prices);
            totalSaved += savedCount;

            if (prices.size() < PAGE_SIZE) {
                break;
            }

            LocalDate lastDate = prices.stream()
                    .map(this::extractDate)
                    .min(Comparator.naturalOrder())
                    .orElseThrow(() -> new IllegalStateException("Cannot extract date from empty price list"));
            currentEndDate = lastDate.minusDays(1);
        }

        log.debug("Total saved {} {} prices for: {}", totalSaved, getAssetTypeName(), code);
    }

    /**
     * API에서 가격 데이터를 조회합니다.
     *
     * @param stock 대상 종목
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 가격 데이터 리스트
     */
    protected abstract List<P> fetchPrices(WatchlistStock stock, LocalDate startDate, LocalDate endDate);

    /**
     * 가격 데이터를 저장합니다.
     *
     * @param stock 대상 종목
     * @param prices 가격 데이터 리스트
     * @return 저장된 데이터 개수
     */
    protected abstract int savePrices(WatchlistStock stock, List<P> prices);

    /**
     * 가격 데이터에서 날짜를 추출합니다.
     *
     * @param price 가격 데이터
     * @return 추출된 날짜
     */
    protected abstract LocalDate extractDate(P price);

    /**
     * 로깅에 사용할 자산 타입 이름을 반환합니다.
     *
     * @return 자산 타입 이름 (예: "domestic stock", "overseas index")
     */
    protected abstract String getAssetTypeName();
}
