package com.custom.trader.stockprice.service;

import com.custom.trader.kis.dto.stockprice.DomesticIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.DomesticStockDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasIndexDailyPriceResponse;
import com.custom.trader.kis.dto.stockprice.OverseasStockDailyPriceResponse;
import com.custom.trader.stockprice.constant.StockPriceConstants;
import com.custom.trader.stockprice.domestic.entity.DomesticIndexDailyPrice;
import com.custom.trader.stockprice.domestic.entity.DomesticStockDailyPrice;
import com.custom.trader.stockprice.domestic.repository.DomesticIndexDailyPriceRepository;
import com.custom.trader.stockprice.domestic.repository.DomesticStockDailyPriceRepository;
import com.custom.trader.stockprice.mapper.StockPriceMapper;
import com.custom.trader.stockprice.overseas.entity.OverseasIndexDailyPrice;
import com.custom.trader.stockprice.overseas.entity.OverseasStockDailyPrice;
import com.custom.trader.stockprice.overseas.repository.OverseasIndexDailyPriceRepository;
import com.custom.trader.stockprice.overseas.repository.OverseasStockDailyPriceRepository;
import com.custom.trader.stockprice.util.TriFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 주식 가격 데이터 저장을 담당하는 Persistence Layer 서비스.
 *
 * <p>책임:
 * <ul>
 *   <li>중복 체크: 기존 데이터와 비교하여 신규 데이터만 저장</li>
 *   <li>Entity 변환: DTO → Entity (StockPriceMapper 활용)</li>
 *   <li>DB 저장: Repository를 통한 데이터 저장</li>
 *   <li>트랜잭션 관리: 종목별 독립 트랜잭션 ({@link Propagation#REQUIRES_NEW})</li>
 * </ul>
 * </p>
 *
 * <p>트랜잭션 전파:
 * <ul>
 *   <li>{@link Propagation#REQUIRES_NEW}: 각 종목별로 독립적인 트랜잭션 생성</li>
 *   <li>부분 실패 허용: 한 종목 저장 실패 시 다른 종목은 영향 없음</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockPricePersistenceService {

    private final DomesticStockDailyPriceRepository domesticStockRepository;
    private final DomesticIndexDailyPriceRepository domesticIndexRepository;
    private final OverseasStockDailyPriceRepository overseasStockRepository;
    private final OverseasIndexDailyPriceRepository overseasIndexRepository;
    private final StockPriceMapper mapper;

    /**
     * Generic 저장 메서드.
     *
     * @param code 종목/지수 코드
     * @param exchange 거래소 코드 (국내는 null)
     * @param priceItems DTO 리스트
     * @param existingDatesFetcher 기존 날짜 조회 함수
     * @param dateFieldExtractor DTO에서 날짜 필드 추출 함수
     * @param mapper DTO -> Entity 변환 함수
     * @param saver Entity 리스트 저장 Consumer
     * @param <D> DTO 타입
     * @param <E> Entity 타입
     * @return 저장된 데이터 개수
     */
    private <D, E> int saveGeneric(
            String code,
            String exchange,
            List<D> priceItems,
            BiFunction<String, String, Set<LocalDate>> existingDatesFetcher,
            Function<D, String> dateFieldExtractor,
            TriFunction<String, String, D, E> mapper,
            Consumer<List<E>> saver
    ) {
        Set<LocalDate> existingDates = existingDatesFetcher.apply(code, exchange);

        List<E> toSave = priceItems.stream()
                .filter(p -> !existingDates.contains(StockPriceConstants.parseDate(dateFieldExtractor.apply(p))))
                .map(p -> mapper.apply(code, exchange, p))
                .toList();

        if (!toSave.isEmpty()) {
            saver.accept(toSave);
        }

        return toSave.size();
    }

    /**
     * 국내 주식 일간 가격 저장 (중복 체크 포함).
     *
     * @param stockCode 종목 코드 (예: "005930")
     * @param priceItems KIS API 응답 DTO 리스트
     * @return 저장된 데이터 개수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveDomesticStockPrices(
            String stockCode,
            List<DomesticStockDailyPriceResponse.PriceItem> priceItems
    ) {
        return saveGeneric(
                stockCode, null, priceItems,
                (code, exchange) -> domesticStockRepository.findAllTradeDatesByStockCode(code),
                p -> p.stckBsopDate(),
                (code, exchange, p) -> mapper.toDomesticStock(code, p),
                domesticStockRepository::saveAll
        );
    }

    /**
     * 국내 지수 일간 가격 저장 (중복 체크 포함).
     *
     * @param indexCode 지수 코드 (예: "0001")
     * @param priceItems KIS API 응답 DTO 리스트
     * @return 저장된 데이터 개수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveDomesticIndexPrices(
            String indexCode,
            List<DomesticIndexDailyPriceResponse.PriceItem> priceItems
    ) {
        return saveGeneric(
                indexCode, null, priceItems,
                (code, exchange) -> domesticIndexRepository.findAllTradeDatesByIndexCode(code),
                p -> p.stckBsopDate(),
                (code, exchange, p) -> mapper.toDomesticIndex(code, p),
                domesticIndexRepository::saveAll
        );
    }

    /**
     * 해외 주식 일간 가격 저장 (중복 체크 포함).
     *
     * @param stockCode 종목 코드 (예: "AAPL")
     * @param exchangeCode 거래소 코드 (예: "NAS")
     * @param priceItems KIS API 응답 DTO 리스트
     * @return 저장된 데이터 개수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveOverseasStockPrices(
            String stockCode,
            String exchangeCode,
            List<OverseasStockDailyPriceResponse.PriceItem> priceItems
    ) {
        return saveGeneric(
                stockCode, exchangeCode, priceItems,
                (code, exchange) -> overseasStockRepository
                        .findAllTradeDatesByStockCodeAndExchangeCode(code, exchange),
                p -> p.xymd(),
                (code, exchange, p) -> mapper.toOverseasStock(code, exchange, p),
                overseasStockRepository::saveAll
        );
    }

    /**
     * 해외 지수 일간 가격 저장 (중복 체크 포함).
     *
     * @param indexCode 지수 코드 (예: "COMP")
     * @param exchangeCode 거래소 코드 (예: "NAS")
     * @param priceItems KIS API 응답 DTO 리스트
     * @return 저장된 데이터 개수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveOverseasIndexPrices(
            String indexCode,
            String exchangeCode,
            List<OverseasIndexDailyPriceResponse.PriceItem> priceItems
    ) {
        return saveGeneric(
                indexCode, exchangeCode, priceItems,
                (code, exchange) -> overseasIndexRepository
                        .findAllTradeDatesByIndexCodeAndExchangeCode(code, exchange),
                p -> p.stckBsopDate(),
                (code, exchange, p) -> mapper.toOverseasIndex(code, exchange, p),
                overseasIndexRepository::saveAll
        );
    }

}
