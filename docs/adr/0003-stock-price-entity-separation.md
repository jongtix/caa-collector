# 0003. StockPrice Entity 4종 분리 설계

## 상태
Accepted (2026-01-24)

## 컨텍스트

주식 가격 데이터를 수집하고 저장해야 하는 요구사항이 있었습니다. 국내/해외 시장과 주식/지수의 조합으로 총 4가지 유형의 데이터가 존재합니다.

### 고려 사항
- **데이터 스키마**: 국내/해외, 주식/지수마다 필드 구성이 다름
  - 국내 주식: 시가/고가/저가/종가/거래량/거래대금 등
  - 해외 주식: 통화, 환율 정보 추가
  - 지수: 거래량/거래대금 없음
- **API 엔드포인트**: KIS API에서 각 유형별로 다른 엔드포인트 사용
- **조회 패턴**: 도메인별로 독립적인 조회 요구사항

## 결정

**4개의 독립적인 Entity로 분리하여 설계**

### Entity 구조
```
com.custom.trader.stockprice
├── domestic/
│   ├── DomesticStockPrice.java     # 국내 주식 일간 가격
│   └── DomesticIndexPrice.java     # 국내 지수 일간 가격
└── overseas/
    ├── OverseasStockPrice.java     # 해외 주식 일간 가격
    └── OverseasIndexPrice.java     # 해외 지수 일간 가격
```

### 테이블 구조
- `domestic_stock_price`: 국내 주식 일간 가격
- `domestic_index_price`: 국내 지수 일간 가격
- `overseas_stock_price`: 해외 주식 일간 가격
- `overseas_index_price`: 해외 지수 일간 가격

### 인덱스 전략
```java
@Table(indexes = {
    @Index(name = "idx_symbol_date", columnList = "symbol,priceDate")
})
```
- 심볼 + 날짜 조합으로 시계열 조회 최적화

## 결과

### 긍정적 영향
- **타입 안정성**: 각 도메인에 맞는 필드만 존재, 컴파일 시점에 오류 검출
- **명확한 책임 분리**: Entity마다 명확한 역할과 스키마
- **조회 성능**: 필요한 테이블만 스캔, 불필요한 필드 로드 방지
- **확장성**: 새로운 유형 추가 시 다른 Entity에 영향 없음
- **유지보수성**: 각 도메인 로직이 독립적으로 관리됨

### 부정적 영향
- **코드 중복**: 유사한 Entity/Repository 코드 4개
  - 완화: 공통 로직은 Service 계층에서 추상화 가능
- **테이블 수 증가**: 4개 테이블 관리 필요
  - 완화: 각 테이블은 독립적이므로 관리 복잡도는 낮음

## 대안

### 대안 1: Single Table Inheritance (STI)
```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "price_type")
public abstract class StockPrice { ... }
```
- 장점: 하나의 테이블로 관리
- 단점:
  - 스키마가 다른 데이터를 억지로 통합 (NULL 컬럼 다수 발생)
  - 인덱스 효율 저하 (discriminator 컬럼 추가 필터링)
  - 테이블 크기 증가

### 대안 2: Joined Table Inheritance
```java
@Inheritance(strategy = InheritanceType.JOINED)
```
- 장점: 공통/특화 필드 분리
- 단점:
  - JOIN 쿼리 필수 (성능 저하)
  - 복잡한 상속 구조

### 대안 3: JSONB 컬럼 사용
```java
@Column(columnDefinition = "jsonb")
private String priceData;
```
- 장점: 유연한 스키마
- 단점:
  - 타입 안정성 상실
  - 쿼리 복잡도 증가
  - 인덱스 생성 제한

## 참고
- 관련 패키지: `com.custom.trader.stockprice`
- 관련 서비스: `KisStockPriceService`, `StockPriceCollectorService`
- 스케줄러: `StockPriceScheduler` (03:00 백필, 18:30 일간 수집)