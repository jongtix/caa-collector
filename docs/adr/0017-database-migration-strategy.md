# ADR-0017: Database Migration Strategy (Flyway 도입 지연 결정)

**날짜**: 2026-02-05
**상태**: ✅ Accepted (Deferred Implementation)
**작성자**: pm, backend-developer

---

## 컨텍스트

### 현재 상황

**CR-08 이슈 발생**: WatchlistStock 테이블에 인덱스 추가 필요 (`backfill_completed`, `id` 복합 인덱스)

**현재 방식**:
- **개발 환경**: JPA `@Index` 어노테이션으로 자동 생성 (H2, Hibernate DDL-Auto: create-drop)
- **Production 환경**: 수동 SQL 실행 + Git 커밋 메시지로 이력 추적
- **변경 이력**: ADR + 커밋 로그 의존 (체계적이지 않음)

```java
// WatchlistStock.java
@Table(name = "watchlist_stock", indexes = {
    @Index(name = "idx_watchlist_stock_backfill_id",
           columnList = "backfill_completed, id")
})
public class WatchlistStock extends BaseEntity {
    // ...
}
```

### 문제점

1. **Production 스키마 변경 시 수동 작업 필요**
   - DBA가 직접 SQL 실행
   - 변경 이력이 파일 시스템에 산재 (Git 커밋, 메모)

2. **버전 관리 도구 부재**
   - Flyway, Liquibase 등 자동화 도구 미사용
   - 스키마 버전과 애플리케이션 버전 간 불일치 리스크

3. **멀티 환경 동기화 어려움**
   - dev, staging, production 간 스키마 차이 발생 가능
   - 롤백 시 수동 역방향 SQL 작성 필요

### 의사결정 시점

- **현재 Phase**: Phase 2 Week 2 (배포 인프라 구축 중, 진행률 24%)
- **Phase 2 우선순위**: Docker, CI/CD, Watchtower 구축 (44시간 작업 대기)
- **프로젝트 규모**: 단일 마이크로서비스, 단일 DB, 1인 개발자
- **스키마 변경 빈도**: 낮음 (CR-08 같은 경우는 드뭄)

---

## 결정

### 결론: Flyway 도입을 Phase 3 이후로 **연기**

**현재 Approach (Phase 2-3):**
1. **개발 환경**: JPA `@Index` 자동 생성 유지
2. **Production 환경**: 수동 SQL 실행 + `docs/sql/` 디렉토리에 스크립트 보관
3. **변경 이력**: ADR + Production SQL 로그 섹션으로 추적
4. **검증**: 로컬 MySQL 컨테이너에서 사전 테스트

**Flyway 도입 트리거 (Phase 3 이후):**
- **Phase 3 종료 시 (2026-03-01)**: WebSocket 도입 후 스키마 변경 빈도 재평가
- **Phase 4 시작 시 (2026-03-02)**: AI Advisor 멀티 DB 환경 복잡도 평가
- **즉시 트리거**: Production 스키마 불일치 이슈 발생 시

---

## 근거

### 1. Phase 2 일정 준수 (최우선)

**Option 1 (채택): Flyway 도입 연기**
- Phase 2: 0시간 (변경 없음, Production SQL 관리 프로세스만 추가)
- Phase 3 이후 재평가
- **총 비용**: 0시간

**Option 2: 즉시 Flyway 도입**
- Phase 2: 6-8시간 (학습 2h, 설정 2h, 마이그레이션 스크립트 2h, 테스트 2h)
- Phase 2 일정 영향: 44시간 작업 대기 중 (14-18% 추가 부담)
- **총 비용**: 6-8시간 + 일정 지연 리스크

**결론**: Option 1이 Phase 2 목표 달성에 유리

### 2. 현재 프로젝트 규모

| 항목 | 현재 상태 | Flyway 필요성 |
|------|----------|--------------|
| **서비스 수** | 1개 (Collector) | 낮음 (멀티 서비스는 Phase 4+) |
| **DB 수** | 1개 (MySQL) | 낮음 (단일 스키마) |
| **개발자 수** | 1명 | 낮음 (협업 충돌 없음) |
| **스키마 변경 빈도** | 낮음 (CR-08 같은 경우 드뭄) | 낮음 |
| **환경 수** | 2개 (local, prod) | 중간 (3개 이상이면 높음) |

**결론**: 1인 개발 환경에서 Flyway는 **Over-Engineering** 가능성

### 3. 기술 부채 관리 전략

**Flyway 도입은 기술 부채 예방이지 긴급 문제 해결이 아님**:
- **긴급**: Docker 컨테이너화, CI/CD 파이프라인 (배포 불가능 상태)
- **중요하지만 긴급하지 않음**: Flyway 도입 (수동 SQL로도 운영 가능)

**완화 조치**:
- `docs/sql/` 디렉토리로 변경 이력 추적
- ADR에 Production SQL 로그 섹션 추가
- Phase 3 종료 시 재평가 (WebSocket 도입 후 스키마 변경 빈도 확인)

### 4. Flyway vs @Index 비교

| 항목 | Flyway | JPA @Index (현재) |
|------|--------|-------------------|
| **학습 곡선** | 중간 (Versioning 전략 이해 필요) | 낮음 (JPA 표준 어노테이션) |
| **초기 설정** | 6-8시간 | 0시간 (이미 적용 중) |
| **Production 적용** | 자동 (애플리케이션 시작 시) | 수동 (SQL 실행) |
| **변경 이력** | DB 테이블에 자동 기록 | Git + ADR 수동 기록 |
| **협업 환경** | 필수 (버전 충돌 방지) | 선택 (1인 개발은 불필요) |
| **롤백** | 수동 (Down 스크립트 작성) | 수동 (역방향 SQL 작성) |
| **복잡도** | 높음 (멀티 환경 설정) | 낮음 (환경별 yml 분리) |
| **Phase 2 적합성** | ❌ 낮음 (일정 지연 위험) | ✅ 높음 (빠른 배포) |

---

## 대안

### 대안 1: 즉시 Flyway 도입

**장점**:
- ✅ Production 스키마 버전 관리 자동화
- ✅ 변경 이력 DB 테이블에 자동 저장 (`flyway_schema_history`)
- ✅ 멀티 환경 스키마 동기화 보장

**단점**:
- ❌ Phase 2 일정 6-8시간 지연
- ❌ 초기 학습 곡선 (Versioning 전략, 네이밍 규칙)
- ❌ 기존 스키마를 Flyway 마이그레이션으로 변환 필요 (Baseline)

**기각 이유**: Phase 2 우선순위 충돌, 1인 개발 환경에서 과도한 오버헤드

### 대안 2: Liquibase 도입

**장점**:
- ✅ XML/YAML 기반 스키마 정의 (가독성)
- ✅ Rollback 전략 우수 (Change Set 단위 롤백)
- ✅ 멀티 DB 지원 (MySQL, PostgreSQL, Oracle)

**단점**:
- ❌ Flyway보다 복잡한 학습 곡선
- ❌ Spring Boot 기본 지원은 Flyway (추가 설정 필요)
- ❌ 대안 1과 동일한 일정 지연 문제

**기각 이유**: 대안 1과 동일 (일정 우선순위 충돌)

### 대안 3: JPA ddl-auto: update (Production)

**장점**:
- ✅ 코드 변경만으로 스키마 자동 적용
- ✅ Flyway/Liquibase 학습 불필요

**단점**:
- ❌ Production에서 자동 스키마 변경은 **매우 위험** (데이터 손실, 예측 불가 변경)
- ❌ 롤백 불가능 (Hibernate가 역방향 SQL 생성 안 함)
- ❌ 업계 표준 위배 (Production은 `validate`만 사용)

**기각 이유**: **절대 금지** (업계 표준: Production은 `validate` 또는 `none`만 사용)

---

## 결과

### 긍정적 영향

1. ✅ **일정 준수**: Phase 2 배포 작업 최우선 (44시간 작업 보호)
2. ✅ **기술 부채 분산**: Phase 3 종료 시 재평가로 리스크 분산
3. ✅ **프로젝트 복잡도 최소화**: 1인 개발 환경 최적화
4. ✅ **점진적 개선**: 필요 시점에 도입 (Phase 4 멀티 DB 환경)

### 부정적 영향

1. ⚠️ **수동 작업 필요**: Production 스키마 변경 시 DBA가 직접 SQL 실행
2. ⚠️ **변경 이력 분산**: Git + ADR에 의존 (DB 테이블 기록 없음)
3. ⚠️ **멀티 환경 동기화**: 수동 관리 필요 (dev, staging, prod)
4. ⚠️ **기술 부채 누적**: Phase 3까지 수동 SQL 방식 유지

### 완화 조치

1. **Production SQL 관리 프로세스**
   - `docs/sql/` 디렉토리에 모든 변경 스크립트 보관
   - 파일명 규칙: `<ISSUE>-<description>.sql` (예: `CR-08-add-watchlist-stock-indexes.sql`)
   - 주석 템플릿: 목적, 적용 날짜, 실행자, 검증 방법

2. **변경 이력 추적**
   - ADR에 **Production SQL 변경 로그** 섹션 추가
   - Git 커밋 메시지에 스키마 변경 명시 (gitmoji `🗃️`)

3. **사전 검증**
   - 로컬 MySQL 컨테이너에서 테스트
   - 배포 전 코드 리뷰 체크리스트에 "스키마 변경 검토" 추가

4. **재검토 일정**
   - **Phase 3 종료 시** (2026-03-01): WebSocket 도입 후 스키마 변경 빈도 평가
   - **Phase 4 시작 시** (2026-03-02): AI Advisor 멀티 DB 환경 복잡도 평가
   - **즉시**: Production 스키마 불일치 이슈 발생 시

---

## Production SQL 변경 로그

### CR-08: WatchlistStock 인덱스 추가 (2026-02-05)

**변경 내용**:
```sql
-- 목적: 백필 스케줄러 성능 최적화
-- backfill_completed = false 조회 시 풀 테이블 스캔 방지

CREATE INDEX idx_watchlist_stock_backfill_id
    ON watchlist_stock (backfill_completed, id);
```

**적용 환경**: Production MySQL
**실행 날짜**: 미정 (Phase 2 배포 시)
**검증 방법**:
```sql
EXPLAIN SELECT id, stock_code, stock_name, backfill_completed
FROM watchlist_stock
WHERE backfill_completed = false
ORDER BY id
LIMIT 100;
```

**예상 결과**:
- Before: `type: ALL` (Full Table Scan)
- After: `type: ref`, `key: idx_watchlist_stock_backfill_id`

**참조**:
- SQL 스크립트: [docs/sql/CR-08-add-watchlist-stock-indexes.sql](../sql/CR-08-add-watchlist-stock-indexes.sql)
- 코드 변경: WatchlistStock.java (L15-17)
- Git Commit: (배포 시 업데이트)

---

## 재검토 일정

| 시점 | 조건 | 평가 항목 |
|------|------|----------|
| **Phase 3 종료** | 2026-03-01 | WebSocket 도입 후 스키마 변경 빈도 재평가 |
| **Phase 4 시작** | 2026-03-02 | AI Advisor 멀티 DB 환경 복잡도 평가 |
| **즉시** | Production 이슈 발생 시 | 스키마 불일치, 롤백 실패 등 |

**재평가 기준**:
- 스키마 변경 빈도 > 월 2회: Flyway 도입 고려
- 멀티 DB 환경 (3개 이상): Flyway 필수
- Production 스키마 불일치 이슈 발생: 즉시 도입

---

## 구현 계획

### Phase 2: Production SQL 관리 프로세스 추가 (즉시 ~ 2026-02-22)

#### 1️⃣ docs/sql/ 디렉토리 생성 (0.5시간)

**디렉토리 구조**:
```
docs/sql/
├── README.md                                    # 사용 가이드
├── CR-08-add-watchlist-stock-indexes.sql       # CR-08 인덱스 추가
└── TEMPLATE.sql                                 # 템플릿 (복사용)
```

**README.md 내용**:
```markdown
# Production SQL Scripts

이 디렉토리는 Production 환경에 적용할 SQL 스크립트를 보관합니다.

## 파일명 규칙
- 형식: `<ISSUE>-<description>.sql`
- 예시: `CR-08-add-watchlist-stock-indexes.sql`

## SQL 스크립트 템플릿
TEMPLATE.sql 참조

## 적용 프로세스
1. 로컬 MySQL 컨테이너에서 테스트
2. 코드 리뷰 (스키마 변경 체크리스트)
3. Production 적용 (DBA 또는 DevOps)
4. ADR에 변경 로그 업데이트
```

#### 2️⃣ TEMPLATE.sql 작성 (0.5시간)

```sql
-- ============================================================
-- Production SQL Script
-- ============================================================
--
-- Issue: <ISSUE-NUMBER>
-- Description: <간단한 설명>
-- Author: <작성자>
-- Date: <작성 날짜>
--
-- ============================================================
-- 목적:
-- <이 변경이 왜 필요한가?>
--
-- ============================================================
-- 영향 범위:
-- - 테이블: <테이블명>
-- - 예상 실행 시간: <예상 시간>
-- - 다운타임 필요 여부: <Yes/No>
--
-- ============================================================
-- 사전 검증 (로컬 MySQL):
-- docker exec -it caa-collector-mysql mysql -u root -p caa_collector
--
-- ============================================================

-- [DDL 또는 DML 쿼리]


-- ============================================================
-- 검증 쿼리:
-- <변경 사항 확인 쿼리>
--
-- 예상 결과:
-- <기대하는 결과>
-- ============================================================

-- ============================================================
-- 롤백 쿼리 (필요 시):
-- <역방향 SQL>
-- ============================================================
```

#### 3️⃣ CR-08 SQL 스크립트 작성 (0.5시간)

파일명: `docs/sql/CR-08-add-watchlist-stock-indexes.sql`

```sql
-- ============================================================
-- Production SQL Script
-- ============================================================
--
-- Issue: CR-08
-- Description: WatchlistStock 테이블 인덱스 추가 (성능 최적화)
-- Author: backend-developer
-- Date: 2026-02-05
--
-- ============================================================
-- 목적:
-- 백필 스케줄러 실행 시 `backfill_completed = false` 조회 성능 개선
-- 풀 테이블 스캔 방지 (기존: ~500ms → 개선: ~50ms)
--
-- ============================================================
-- 영향 범위:
-- - 테이블: watchlist_stock
-- - 예상 실행 시간: ~3초 (레코드 수 < 1000개 가정)
-- - 다운타임 필요 여부: No (인덱스 생성은 Non-Blocking)
--
-- ============================================================
-- 사전 검증 (로컬 MySQL):
-- docker exec -it caa-collector-mysql mysql -u root -p caa_collector
--
-- ============================================================

-- 인덱스 추가
CREATE INDEX idx_watchlist_stock_backfill_id
    ON watchlist_stock (backfill_completed, id);


-- ============================================================
-- 검증 쿼리:
--
EXPLAIN SELECT id, stock_code, stock_name, backfill_completed
FROM watchlist_stock
WHERE backfill_completed = false
ORDER BY id
LIMIT 100;
--
-- 예상 결과:
-- - type: ref (Full Table Scan 아님)
-- - key: idx_watchlist_stock_backfill_id
-- - rows: < 100 (스캔 레코드 수)
-- ============================================================

-- ============================================================
-- 롤백 쿼리 (필요 시):
--
DROP INDEX idx_watchlist_stock_backfill_id ON watchlist_stock;
-- ============================================================
```

#### 4️⃣ TODO.md 업데이트 (0.5시간)

**추가 항목** (Priority 1):
```markdown
### Phase 3 종료 시 (2026-03-01)
- [ ] Flyway 도입 재평가 (ADR-0017 참조)
  - WebSocket 도입 후 스키마 변경 빈도 확인
  - 월 2회 이상 변경 시 Flyway 도입 고려
```

#### 5️⃣ ADR README 업데이트 (0.5시간)

**추가 행**:
```markdown
| [0017](0017-database-migration-strategy.md) | Database Migration Strategy (Flyway 도입 지연) | Accepted | 2026-02-05 |
```

---

## 참고 자료

### 내부 문서
- [docs/TODO.md](../TODO.md) - Phase 2 작업 목록
- [docs/TECHSPEC.md](../TECHSPEC.md) - Database Schema
- [docs/MILESTONE.md](../MILESTONE.md) - Phase 2/3 일정

### MSA 문서
- [../../docs/MILESTONE.md](../../MILESTONE.md) - MSA 전체 일정
- [../../docs/BLUEPRINT.md](../../BLUEPRINT.md) - Database Strategy

### 기술 스택
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Spring Boot Database Initialization](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization)
- [Liquibase vs Flyway Comparison](https://www.baeldung.com/liquibase-vs-flyway)
- [MySQL Index Performance](https://dev.mysql.com/doc/refman/8.0/en/optimization-indexes.html)

---

## 버전 히스토리

- **v1.0** (2026-02-05): 초안 작성 (pm, backend-developer)
