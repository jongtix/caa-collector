# 0005. WatchlistGroup 인덱스 추가

## 상태
Accepted (2026-01-23)

## 컨텍스트

`WatchlistGroup` Entity는 사용자별 관심종목 그룹을 관리하며, 다음과 같은 조회 패턴이 있습니다:

### 주요 조회 쿼리
```java
// 1. 사용자별 그룹 목록 조회
List<WatchlistGroup> findByUserId(String userId);

// 2. 사용자 + 그룹코드로 단건 조회
Optional<WatchlistGroup> findByUserIdAndGroupCode(String userId, String groupCode);
```

### 문제 상황
- 인덱스 없이 테이블 Full Scan 발생
- 스케줄러가 하루 2회(08:00, 18:00) 실행되며 매번 조회 발생
- 데이터가 증가하면 조회 성능 저하 예상

## 결정

**단일 인덱스 `idx_watchlist_group_user_id` 추가 + Unique Constraint 활용**

### 구현 방법
```java
@Entity
@Table(name = "watchlist_group",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "group_code"})
    },
    indexes = {
        @Index(name = "idx_watchlist_group_user_id", columnList = "user_id")
    }
)
public class WatchlistGroup extends BaseEntity {
    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(name = "group_code", nullable = false, length = 10)
    private String groupCode;
    // ...
}
```

### 설계 근거
1. **단일 인덱스 (user_id)**:
   - `findByUserId()` 조회 최적화
   - 가장 빈번한 조회 패턴

2. **Unique Constraint (user_id, group_code)**:
   - 비즈니스 규칙: 동일 사용자가 동일 그룹코드를 중복으로 가질 수 없음
   - Unique Constraint는 내부적으로 인덱스를 생성함
   - `findByUserIdAndGroupCode()` 조회 시 이 인덱스 활용 가능

### 데이터 모델 특징
- **user_id**: 사용자 식별자 (KIS API 계정명)
- **group_code**: 그룹 코드 (KIS API에서 제공하는 그룹 식별자)
- **group_name**: 그룹명 (사용자가 지정한 이름)

## 결과

### 긍정적 영향
- **조회 성능 향상**: Full Scan → Index Range Scan
- **확장성**: 데이터 증가 시에도 일정한 조회 성능 유지
- **복합 쿼리 지원**: 두 가지 조회 패턴 모두 인덱스 활용 가능
  - `findByUserId()`: idx_watchlist_group_user_id 사용
  - `findByUserIdAndGroupCode()`: Unique Constraint 인덱스 사용
- **데이터 무결성**: Unique Constraint로 중복 방지

### 부정적 영향
- **쓰기 성능 약간 저하**: INSERT/UPDATE 시 인덱스 갱신 오버헤드
  - 완화: 관심종목 그룹은 쓰기보다 읽기가 훨씬 많음 (Read-heavy)
- **저장 공간 증가**: 2개 인덱스 저장 공간 필요 (단일 + Unique Constraint)
  - 완화: 데이터 건수가 적어 용량 부담 미미

## 대안

### 대안 1: 복합 인덱스만 사용
```java
@Index(name = "idx_user_group", columnList = "user_id,group_code")
```
- 장점: 인덱스 1개만 생성, 두 쿼리 패턴 모두 커버
- 단점: Unique Constraint를 별도로 관리해야 하므로 결국 2개 인덱스 필요

### 대안 2: 복합 인덱스 2개 생성
```java
@Index(name = "idx_user_id", columnList = "user_id")
@Index(name = "idx_user_group", columnList = "user_id,group_code")
```
- 장점: 각 쿼리 최적화
- 단점:
  - 복합 인덱스가 user_id를 포함하므로 단일 인덱스 중복
  - 저장 공간 낭비
  - Unique Constraint 미적용 시 데이터 무결성 보장 불가

### 대안 3: Covering Index
```java
@Index(name = "idx_cover", columnList = "user_id,group_code,group_name")
```
- 장점: 인덱스만으로 쿼리 완료 (Index Only Scan)
- 단점:
  - 현재 조회 시 전체 컬럼을 가져오므로 효과 제한적
  - 인덱스 크기 증가

## 쿼리 실행 계획 분석

### 인덱스 적용 전
```sql
EXPLAIN SELECT * FROM watchlist_group WHERE user_id = 'USER1';
-- type: ALL (Full Table Scan)
```

### 인덱스 적용 후
```sql
EXPLAIN SELECT * FROM watchlist_group WHERE user_id = 'USER1';
-- type: ref (Index Range Scan)
-- key: idx_watchlist_group_user_id

EXPLAIN SELECT * FROM watchlist_group WHERE user_id = 'USER1' AND group_code = 'G001';
-- type: const (Unique Key Lookup)
-- key: UK_user_id_group_code (Unique Constraint 인덱스)
```

## 참고
- 커밋: `fa55c29 ⚡ WatchlistGroup 인덱스 추가`
- 관련 파일: `WatchlistGroup.java:20-26`
- 관련 Repository: `WatchlistGroupRepository.java`
- 조회 패턴: Read-heavy (쓰기 < 읽기)