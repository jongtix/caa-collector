# ADR-0016: 글로벌 주식 시장 스케줄 아키텍처

**날짜**: 2026-02-04
**상태**: ✅ Accepted
**작성자**: pm, stock-market-expert

---

## 컨텍스트

### 현재 상황

Collector 서비스는 현재 **한국 시장에만 특화된 스케줄 구조**를 사용하고 있습니다:

```java
// StockPriceScheduler.java
@Scheduled(cron = "0 0 3 * * *")  // 백필: 03:00 KST
public void executeBackfill() { ... }

@Scheduled(cron = "0 30 18 * * *")  // 일간 수집: 18:30 KST
public void executeDailyCollection() { ... }
```

### 문제점

1. **하드코딩된 스케줄**: 한국 시장 거래 시간에만 최적화
2. **글로벌 확장 제약**: 미국, 유럽, 아시아 등 다른 시장 추가 시 관리 복잡도 증가
3. **휴장일 처리 미흡**: 정적 휴장일 캘린더 없음
4. **타임존 관리 부재**: 서머타임(DST) 대응 로직 없음

### 글로벌 시장 요구사항

| 시장 | 폐장 시간 | KST 환산 | 권장 수집 시간 | 비고 |
|------|----------|---------|--------------|------|
| **한국 (KRX)** | 15:30 | 15:30 | **18:30** | 3시간 버퍼 |
| **일본 (TSE)** | 15:00 | 15:00 | **18:30** | 한국과 통합 가능 |
| **홍콩 (HKEX)** | 16:00 | 17:00 | **18:30** | 한국과 통합 가능 |
| **미국 (NYSE)** | 16:00 ET | 06:00 (표준시)<br>05:00 (서머타임) | **07:00** | DST 안전 버퍼 |
| **유럽 (LSE)** | 16:30 GMT | 01:30 | **03:00** | 백필과 통합 가능 |

**서머타임 이슈**:
- **미국**: 3월 두 번째 일요일 ~ 11월 첫 번째 일요일 (UTC-4)
- **표준시**: UTC-5
- **리스크**: 06:00 KST 수집 시 서머타임 적용 시 폐장 후 1시간만 확보 (데이터 미확정 가능성)

### 일정 압박

- **Phase 2 진행률**: 24% (Week 2 시작)
- **배포 인프라 작업**: Docker, CI/CD, Watchtower 구축 필요 (44시간 예상)
- **리팩터링 비용**: 스케줄 아키텍처 개선 시 3시간 소요
- **트레이드오프**: Phase 2 일정 준수 vs 기술 부채 해소

---

## 결정

**점진적 접근 전략 (Phased Approach)**을 채택합니다:

1. **Phase 2 (즉시)**: 현재 구조 유지, 배포 인프라 작업 최우선
2. **Phase 3 시작 전 (2026-02-22까지)**: 시장별 설정 분리 및 미국 스케줄 추가
3. **Phase 4 이후 (필요 시)**: 동적 스케줄링 도입 (Quartz Scheduler)

### Phase 2: 최소 변경 (즉시 배포)

**변경 없음**:
```java
// 기존 스케줄 유지
@Scheduled(cron = "0 0 3 * * *")    // 백필: 03:00 KST
@Scheduled(cron = "0 30 18 * * *")  // 한국 일간: 18:30 KST
```

**이유**:
- Phase 2 배포 일정 준수 (진행률 24%, 44시간 작업 대기)
- 스케줄 변경은 Phase 3 WebSocket 실시간 수집과 함께 개선

### Phase 3 시작 전: 리팩터링 (2026-02-22까지, 3시간)

#### 1️⃣ 시장별 설정 분리

```java
// config/MarketScheduleConfig.java (신규)
@Configuration
@ConfigurationProperties(prefix = "scheduler.market")
public class MarketScheduleConfig {

    private Map<String, MarketSchedule> markets;

    public record MarketSchedule(
        String cron,
        String timezone,
        List<String> holidays  // 정적 휴장일 목록
    ) {}
}
```

```yaml
# application.yml
scheduler:
  market:
    korea:
      cron: "0 30 18 * * TUE-SAT"  # 월요일 제외 (주말 데이터 수집)
      timezone: "Asia/Seoul"
      holidays:
        - "2026-01-01"  # 신정
        - "2026-02-16"  # 설날
        - "2026-03-01"  # 삼일절
    usa:
      cron: "0 0 7 * * TUE-SAT"  # DST 안전 시간
      timezone: "America/New_York"
      holidays:
        - "2026-01-01"  # New Year's Day
        - "2026-07-04"  # Independence Day
```

#### 2️⃣ 미국 스케줄 추가

```java
// StockPriceScheduler.java
@Scheduled(cron = "0 0 7 * * TUE-SAT")  // 미국 일간: 07:00 KST
@SchedulerLock(name = "usa_daily_collection")
public void executeUsaDailyCollection() {
    List<WatchlistStock> usStocks = watchlistStockRepository
        .findByMarketCode(MarketCode.NASDAQ);

    stockPriceCollectionService.collectDailyPrices(usStocks);
}
```

**07:00 KST 선택 이유**:
- **표준시 (UTC-5)**: 미국 폐장 16:00 ET → KST 06:00 → 수집 07:00 (1시간 버퍼)
- **서머타임 (UTC-4)**: 미국 폐장 16:00 EDT → KST 05:00 → 수집 07:00 (2시간 버퍼)
- **안정성**: 두 경우 모두 충분한 버퍼 확보 (데이터 확정 보장)

#### 3️⃣ 정적 휴장일 캘린더 구현

```java
// service/MarketHolidayService.java (신규)
@Service
public class MarketHolidayService {

    private final MarketScheduleConfig config;

    public boolean isTradingDay(MarketCode market, LocalDate date) {
        // 1. 주말 체크
        if (date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY) {
            return false;
        }

        // 2. 정적 휴장일 체크
        MarketSchedule schedule = config.getMarkets().get(market.name().toLowerCase());
        return !schedule.holidays().contains(date.toString());
    }
}
```

#### 4️⃣ 최종 스케줄 구조

```
03:00 KST (매일): 백필 작업 (한국 + 미국 과거 데이터)
                  향후 유럽 데이터 추가 가능
07:00 KST (화~토): 미국 일간 수집 (NYSE, NASDAQ)
18:30 KST (화~토): 한국/아시아 일간 수집 (KRX, TSE, HKEX)
```

---

## 근거

### 1. 일정 영향 최소화

**Option 1 (채택): 점진적 접근**
- Phase 2: 0시간 (변경 없음)
- Phase 3 시작 전: 3시간 (리팩터링)
- **총 비용**: 3시간

**Option 2: 즉시 리팩터링**
- Phase 2: 3시간 (스케줄 리팩터링)
- Phase 2 일정 영향: 44시간 작업 대기 중 (7% 추가 부담)
- **총 비용**: 3시간 + 일정 지연 리스크

**결론**: Option 1이 일정 준수에 유리

### 2. 기술적 우수성

**장점**:
- **관심사 분리**: 시장별 설정 중앙화 (MarketScheduleConfig)
- **확장성**: 새 시장 추가 시 YAML 설정만 추가
- **유지보수성**: cron 표현식 변경 시 코드 수정 불필요

**단점**:
- **정적 휴장일**: 외부 API 연동 없이 YAML로 관리 (수동 업데이트 필요)
- **완화 조치**: Phase 4에서 외부 휴장일 API 연동 검토

### 3. DST 대응 전략

**동적 조정 vs 고정 시간 + 버퍼**:

| 방식 | 장점 | 단점 |
|------|------|------|
| **동적 조정** | 정확한 시간 | 복잡한 타임존 로직, 버그 리스크 |
| **고정 시간 + 버퍼** (채택) | 단순함, 안정성 | 약간의 시간 비효율 |

**채택 이유**: 07:00 KST는 표준시/서머타임 모두 안전하므로 복잡한 동적 조정 불필요

### 4. Phase 3 WebSocket 전환 고려

**Phase 3 계획** (2026-02-23 ~ 03-01):
- WebSocket 실시간 데이터 수집 도입
- 일간 스케줄은 **보조 수단** 또는 **백업**으로 전환 가능
- 장기적으로 일간 수집 빈도 감소 가능성

**결론**: Phase 2에서 과도한 스케줄 최적화는 불필요 (WebSocket이 주 수집 수단)

---

## 대안

### 대안 1: 지금 시장별 설정 분리 (Option 2)

**장점**:
- ✅ 즉시 글로벌 확장 준비
- ✅ 기술 부채 조기 해소

**단점**:
- ❌ 3시간 소요 (Phase 2 일정 압박)
- ❌ Phase 3 WebSocket 도입 시 일간 수집 역할 축소 가능성
- ❌ 현재 미국 데이터 수집 계획 없음 (Phase 3에서 검토)

**채택하지 않은 이유**: Phase 2 배포 인프라 작업 우선순위가 더 높음

### 대안 2: Quartz Scheduler 도입 (Option 3)

**장점**:
- ✅ 동적 스케줄링 지원 (런타임 변경 가능)
- ✅ 클러스터링 지원 (분산 환경 대비)
- ✅ 휴장일 자동 스킵 로직 구현 가능

**단점**:
- ❌ 12시간 소요 (의존성 추가, DB 테이블 생성, 마이그레이션)
- ❌ Over-Engineering (현재 요구사항: 고정 스케줄만 필요)
- ❌ 유지보수 복잡도 증가

**채택하지 않은 이유**: 1인 개발 환경에서 불필요한 복잡성 도입

### 대안 3: 외부 휴장일 API 연동

**장점**:
- ✅ 자동화된 휴장일 관리
- ✅ 정확성 보장

**단점**:
- ❌ 외부 의존성 증가 (API 장애 시 영향)
- ❌ API 비용 발생 가능
- ❌ 네트워크 지연 리스크

**채택하지 않은 이유**: Phase 1 정적 캘린더로 충분, Phase 4에서 재검토

---

## 결과

### 긍정적 영향

1. ✅ **일정 준수**: Phase 2 배포 작업 최우선 (변경 최소화)
2. ✅ **점진적 개선**: Phase 3 시작 전 리팩터링으로 리스크 분산
3. ✅ **글로벌 확장 기반**: 시장별 설정 분리로 향후 확장 용이
4. ✅ **DST 안전성**: 07:00 KST 고정 시간으로 표준시/서머타임 모두 대응
5. ✅ **유지보수성**: YAML 기반 설정으로 코드 변경 최소화

### 부정적 영향

1. ⚠️ **단기 제약**: Phase 3까지 미국 데이터 수집 불가
2. ⚠️ **정적 휴장일 관리**: 수동 YAML 업데이트 필요
3. ⚠️ **기술 부채**: Phase 2 동안 하드코딩된 스케줄 유지

### 완화 조치

- **미국 데이터 부재**: Phase 3 WebSocket 도입 시 실시간 수집으로 대체 가능
- **정적 휴장일**: 연 1회 YAML 업데이트 (새해 초), Phase 4에서 API 연동 검토
- **기술 부채**: Phase 3 시작 전 3시간 리팩터링으로 해소 예정

---

## 구현 계획

### Phase 2: 배포 우선 (즉시 ~ 2026-02-22)

**변경 없음**:
- ✅ 기존 스케줄 유지 (03:00, 18:30)
- ✅ 배포 자동화 작업 진행 (Docker, CI/CD, Watchtower)

**문서화**:
- ✅ ADR-0016 작성 (이 문서)
- ✅ TODO.md 업데이트 (스케줄 토론 내용 요약)
- ✅ TECHSPEC.md 업데이트 (향후 계획 요약)

### Phase 3 시작 전: 리팩터링 (2026-02-22까지, 3시간)

#### 1️⃣ MarketScheduleConfig 추가 (1시간)
```bash
# 파일 생성
touch src/main/java/com/custom/trader/config/MarketScheduleConfig.java
touch src/main/java/com/custom/trader/service/MarketHolidayService.java
```

#### 2️⃣ application.yml 설정 추가 (0.5시간)
```yaml
scheduler:
  market:
    korea:
      cron: "0 30 18 * * TUE-SAT"
      timezone: "Asia/Seoul"
      holidays: [...]
    usa:
      cron: "0 0 7 * * TUE-SAT"
      timezone: "America/New_York"
      holidays: [...]
```

#### 3️⃣ StockPriceScheduler 리팩터링 (1시간)
- 미국 일간 수집 메서드 추가
- MarketHolidayService 연동
- 휴장일 체크 로직 추가

#### 4️⃣ 테스트 작성 및 검증 (0.5시간)
- MarketHolidayService 단위 테스트
- 휴장일 스킵 통합 테스트
- cron 표현식 검증 테스트

---

## 참고 자료

- [docs/TODO.md](../TODO.md) - 스케줄 토론 내용 (2026-02-04)
- [docs/TECHSPEC.md](../TECHSPEC.md) - Scheduler 명세
- [docs/MILESTONE.md](../MILESTONE.md) - Phase 2/3 일정
- [Spring @Scheduled Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#scheduling)
- [NYSE Trading Hours](https://www.nyse.com/markets/hours-calendars)
- [KRX Trading Hours](http://www.krx.co.kr/main/main.jsp)

---

## 버전 히스토리

- **v1.0** (2026-02-04): 초안 작성 (pm, stock-market-expert)
