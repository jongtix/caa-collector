# 0006. MapStruct를 활용한 Entity 변환 로직 개선

## 상태
Accepted (2026-01-25)

## 컨텍스트

`StockBackfillService`와 `StockPriceCollectorService`에서 DTO → Entity 변환 로직이 중복되어 있었습니다.

### 문제 상황
- 8개 메서드에서 동일한 변환 로직 반복
- DTO 필드가 추가/변경될 때 8곳 모두 수정 필요
- `parseBigDecimal()`, `parseDate()` 헬퍼 메서드도 중복

```java
// StockBackfillService.java (179-201라인)
var entity = DomesticStockDailyPrice.builder()
    .stockCode(stockCode)
    .tradeDate(parseDate(priceItem.stckBsopDate()))
    .openPrice(parseBigDecimal(priceItem.stckOprc()))
    // ... 반복

// StockPriceCollectorService.java (88-104라인)
var entity = DomesticStockDailyPrice.builder()
    .stockCode(stock.getStockCode())
    .tradeDate(parseDate(priceItem.stckBsopDate()))
    .openPrice(parseBigDecimal(priceItem.stckOprc()))
    // ... 동일한 로직 반복
```

## 결정

**MapStruct 라이브러리를 도입하여 변환 로직을 중앙화**

### 구현 방법

1. **Gradle 의존성 추가**:
   ```gradle
   implementation 'org.mapstruct:mapstruct:1.5.5.Final'
   annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
   ```

2. **Mapper 인터페이스 생성**:
   ```java
   @Mapper(componentModel = "spring")
   public interface StockPriceMapper {

       @Mapping(target = "tradeDate", expression = "java(parseDate(dto.stckBsopDate()))")
       @Mapping(target = "openPrice", expression = "java(parseBigDecimal(dto.stckOprc()))")
       DomesticStockDailyPrice toDomesticStock(String stockCode, PriceItem dto);

       default LocalDate parseDate(String dateStr) {
           return LocalDate.parse(dateStr, DATE_FORMATTER);
       }

       default BigDecimal parseBigDecimal(String value) {
           return value != null ? new BigDecimal(value) : BigDecimal.ZERO;
       }
   }
   ```

3. **Service에서 사용**:
   ```java
   var entity = stockPriceMapper.toDomesticStock(stockCode, priceItem);
   ```

## 결과

### 긍정적 영향
- **중복 제거**: 8개 메서드의 변환 로직이 Mapper 인터페이스 4개 메서드로 통합
- **타입 안전성**: 컴파일 타임에 매핑 코드 생성 및 검증
- **유지보수성**: 필드 변경 시 Mapper만 수정
- **성능**: 런타임 리플렉션 없이 컴파일 타임 코드 생성 (성능 우수)
- **테스트 용이성**: Mapper만 독립적으로 테스트 가능

### 부정적 영향
- **빌드 의존성 추가**: MapStruct 라이브러리 및 annotation processor 추가
  - 완화: 널리 사용되는 안정적인 라이브러리 (Spring 공식 권장)
- **학습 곡선**: MapStruct 어노테이션 학습 필요
  - 완화: 기본 사용법은 간단하며, 복잡한 매핑은 default 메서드로 해결

## 대안

### 대안 1: 수동 Mapper 클래스
```java
public class StockPriceMappers {
    public static DomesticStockDailyPrice toDomesticStock(...) {
        return DomesticStockDailyPrice.builder()...build();
    }
}
```
- 장점: 의존성 없음
- 단점: 수동 작성, 컴파일 타임 검증 없음

### 대안 2: Entity 정적 팩토리 메서드
```java
public static DomesticStockDailyPrice from(String stockCode, PriceItem dto) {...}
```
- 장점: 코드 간결
- 단점: Entity가 DTO에 의존 (의존성 방향 역전)

### 대안 3: DTO에 toEntity() 메서드
```java
public DomesticStockDailyPrice toEntity(String stockCode) {...}
```
- 장점: 사용 편리
- 단점: DTO가 Domain 계층에 의존 (계층 분리 약화)

## 참고
- MapStruct 공식 문서: https://mapstruct.org/
- 관련 파일: `StockPriceMapper.java`, `StockBackfillService.java`, `StockPriceCollectorService.java`
- 변환 대상: 4종 Entity (DomesticStock, DomesticIndex, OverseasStock, OverseasIndex)
