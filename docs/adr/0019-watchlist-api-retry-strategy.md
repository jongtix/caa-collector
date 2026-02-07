# ADR-0019: Watchlist API 재시도 전략

## 상태

**Accepted** (2026-02-06)

## 컨텍스트

### 이슈: MA-01 - WatchlistService의 N+1 API 호출 패턴

**현재 구현**:
- WatchlistService의 그룹 동기화 시 각 그룹마다 1회의 KIS API 호출 발생
- 그룹 수에 비례하여 API 호출 횟수 증가 (N+1 패턴)
- 순차 처리 방식으로 총 소요 시간이 선형적으로 증가

**성능 메트릭**:

| 그룹 수 | API 호출 횟수 | 예상 소요 시간 (250ms/call) | KIS Rate Limit (20/s) 사용률 |
|---------|--------------|---------------------------|---------------------------|
| 5개 | 6회 | 1.5초 | 30% |
| 10개 | 11회 | 2.75초 | 55% |
| 20개 | 21회 | 5.25초 | **105%** ⚠️ |
| 50개 | 51회 | 12.75초 | **255%** ⚠️ |

**현재 비즈니스 상황**:
- 현재 사용자의 평균 그룹 수: 5-10개 수준
- 스케줄: 하루 2회 (08:00, 18:00 실행)
- **실제 비즈니스 영향**: 없음 (Rate Limit 초과 없음, 2-3초 소요 허용 가능)

**잠재적 위험**:
- 그룹 20개 이상 달성 시 Rate Limit 초과 위험 (20/s = 1초당 20회)
- 장기적으로 사용자 확장 시 성능 저하 및 API 호출 실패 가능성

## 대안 검토

### Option A: 병렬화 (CompletableFuture + Virtual Threads)

**구현 방안**:
```java
// Virtual Threads 기반 병렬 API 호출
List<CompletableFuture<GroupResponse>> futures = groups.stream()
    .map(group -> CompletableFuture.supplyAsync(
        () -> kisApiClient.getGroupStocks(group.getGroupId()),
        virtualThreadExecutor
    ))
    .toList();

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .join();
```

**효과**:
- 성능 개선: **80% 향상** (20개 그룹 기준 5.25초 → 1초)
- Rate Limit 효율 최대화 (20/s → 초당 20개 병렬 호출)

**비용**:
- 개발 시간: **22시간** (3-4일)
  - CompletableFuture 구조 설계 및 구현 (8시간)
  - Virtual Thread Executor 설정 (2시간)
  - 에러 핸들링 (병렬 예외 처리, Circuit Breaker 연동) (6시간)
  - 테스트 작성 (병렬 시나리오, 부분 실패 처리) (4시간)
  - 문서화 (2시간)

**리스크**:
- **높음**: 복잡도 증가 (병렬 구현, 동시성 이슈)
- 부분 실패 처리 복잡 (일부 그룹만 실패 시 롤백 전략)
- 병렬 재시도 시 Rate Limit 초과 가능성

**결론**: ❌ **Phase 2에서는 보류**

---

### Option B: 재시도 로직만 추가 (현재 채택)

**구현 방안**:
```java
@Retryable(
    value = {KisApiException.class, RestClientException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2.0)
)
public GroupResponse getGroupStocksWithRetry(String groupId) {
    return kisApiClient.getGroupStocks(groupId);
}
```

**효과**:
- 안정성 개선: 일시적 네트워크 장애 대응 (타임아웃, 502/503 에러)
- 성능 개선: **없음** (여전히 순차 처리)
- Rate Limit: **변화 없음** (여전히 20개 그룹 시 초과 위험)

**비용**:
- 개발 시간: **9시간** (2일)
  - WatchlistService 리팩터링 (2시간)
  - KisWatchlistService 재시도 로직 추가 (1.5시간)
  - 커스텀 예외 추가 (1시간)
  - 로깅 개선 (1시간)
  - 설정 및 테스트 (1.5시간)
  - 문서화 (ADR 작성, 2시간)

**리스크**:
- **낮음**: 프로덕션 검증된 패턴 (Spring Retry 표준)
- 재시도 지연으로 인한 동기화 시간 증가 (최대 7초: 1s + 2s + 4s)

**결론**: ✅ **Phase 2에서 채택**

---

### Option C: Redis 캐싱만 추가

**구현 방안**:
```java
@Cacheable(value = "groupStocks", key = "#groupId", unless = "#result == null")
public GroupResponse getGroupStocks(String groupId) {
    return kisApiClient.getGroupStocks(groupId);
}
```

**효과**:
- 성능 개선: 반복 조회 시 **95% 향상** (캐시 히트 시 API 호출 제거)
- 첫 조회: **개선 없음** (여전히 N+1 패턴)

**비용**:
- 개발 시간: **2시간**
- 캐시 무효화 전략 필요 (복잡도 증가)

**결론**: 추후 Option B와 조합 가능 (Phase 3 이후)

---

### Option D: 현상 유지

**효과**: 없음

**비용**: 0시간

**리스크**:
- **중간**: 그룹 20개 이상 달성 시 Rate Limit 초과 → API 호출 실패 → 동기화 실패

**결론**: ❌ **미선택** (장기 위험 방치)

---

## 결정

**선택**: **Option B (재시도 로직만 추가)**

### 선택 이유

1. **현재 비즈니스 영향 없음**
   - 5-10개 그룹 수준에서는 Rate Limit 초과 없음 (30-55% 사용률)
   - 하루 2회 스케줄 (08:00, 18:00)로 2-3초 소요는 허용 가능

2. **일시적 장애 대응 우선**
   - 네트워크 타임아웃, 일시적 502/503 에러에 강건하게 대응
   - 안정성 개선으로 운영 리스크 감소

3. **개발 시간 최소화**
   - 9시간 (2일) vs 22시간 (3-4일)
   - Phase 2 Week 2 일정 준수 (배포 자동화 우선)

4. **프로덕션 검증된 패턴**
   - Spring Retry 표준 라이브러리 활용
   - 예측 가능한 동작, 낮은 리스크

5. **점진적 개선 전략**
   - Phase 2: 재시도 로직으로 안정성 확보
   - Phase 3 이후: 그룹 20개 달성 시 병렬화 재검토

### Trade-off 수용

- ✅ **수용**: 성능 개선 없음 (여전히 N+1 패턴)
- ✅ **수용**: 그룹 20개 이상 시 Rate Limit 초과 위험 (현재 비즈니스 영향 없음)
- ✅ **수용**: 재시도 지연으로 최대 7초 추가 (일시적 장애 시에만 발생)

---

## 구현 계획

### 일정

- **시작**: 2026-02-09 (월)
- **종료**: 2026-02-15 (금)
- **소요 시간**: 9시간

### 단계별 작업

#### Step 1: WatchlistService 리팩터링 (2시간)

- [ ] `upsertGroups()` 메서드에 재시도 로직 분리
- [ ] 트랜잭션 경계 명확화 (재시도 시 트랜잭션 재시작)
- [ ] 로깅 개선 (재시도 시도 횟수, 성공/실패 기록)

#### Step 2: KisWatchlistService 재시도 로직 추가 (1.5시간)

- [ ] Spring Retry 의존성 추가 (`build.gradle`)
- [ ] `@EnableRetry` 설정 (`WatchlistConfig`)
- [ ] `@Retryable` 애노테이션 적용
  - 대상 예외: `KisApiException`, `RestClientException`
  - 최대 시도: 3회
  - Backoff: 1초, 2초, 4초 (지수 백오프)

#### Step 3: 커스텀 예외 추가 (1시간)

- [ ] `WatchlistSyncException` 생성
  - 재시도 불가능한 예외 (4xx 에러 등)
  - 재시도 가능한 예외 (5xx 에러, 타임아웃)

#### Step 4: 로깅 개선 (1시간)

- [ ] 재시도 이벤트 로깅 (`@Recover` 메서드)
- [ ] 최종 실패 시 에러 로그 및 알림

#### Step 5: 설정 및 테스트 (1.5시간)

- [ ] `application.yml`에 재시도 설정 추가
- [ ] 통합 테스트 작성
  - 재시도 성공 시나리오 (2회 실패 → 3회 성공)
  - 최종 실패 시나리오 (3회 모두 실패)
  - Backoff 간격 검증

#### Step 6: 문서화 (2시간)

- [ ] ADR-0019 작성 (이 문서)
- [ ] TODO.md 업데이트 (MA-01 항목 수정)
- [ ] MILESTONE.md 업데이트 (Phase 2 Week 2 일정 반영)

---

## 후속 조치 (Deferred to Phase 3+)

### Trigger: 그룹 20개 달성

**조건**:
- 사용자당 관심종목 그룹 수가 20개 이상일 때
- Rate Limit 초과 발생 시

**Action**:
- Option A (병렬화) 필요성 재검토
- 목표: 성능 80% 개선 (5.25초 → 1초)

### Phase 3 재평가 항목

1. **실제 사용자 데이터 분석**
   - 평균 그룹 수 추적 (매주 통계)
   - Rate Limit 초과 발생 빈도

2. **병렬화 ROI 검토**
   - 22시간 투자 대비 효과
   - WebSocket 실시간 동기화와의 통합 가능성

3. **대안 검토**
   - 배치 API 제공 여부 (KIS API 확인)
   - 캐싱 전략 추가 (Option C)

---

## 위험 및 완화 방안

| 위험 | 영향도 | 발생 확률 | 완화 방안 |
|------|--------|----------|----------|
| Rate Limit 초과 (20개 그룹 이상) | 높음 | 중 (장기적) | Phase 3에서 병렬화 재검토 |
| 재시도 지연으로 동기화 시간 증가 | 낮음 | 낮음 (일시적 장애 시에만) | 지수 백오프 설정 조정 (최대 3회) |
| 장기 트랜잭션으로 DB 연결 점유 | 중간 | 낮음 | MySQL 타임아웃 설정 확인 (30초) |
| 재시도 로직 버그 (무한 루프) | 높음 | 매우 낮음 | maxAttempts=3 설정, 통합 테스트로 검증 |

---

## 의사 결정권자

- **승인자**: Backend Developer (기술 결정), PM (일정 조율)
- **구현자**: Backend Developer
- **검토자**: Code Reviewer

---

## 참고 문서

- **MA-01 검증**: Performance Engineer 보고서 (docs/review/feature-caa-collector-deployment-infra.md)
- **Option 비교**: Backend Developer 기술 검토
- **Spring Retry 공식 문서**: https://docs.spring.io/spring-retry/docs/api/current/

---

## 태그

`#performance` `#api-retry` `#n-plus-one` `#phase2` `#deferred-optimization`
