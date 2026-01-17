---
name: persistence-strategy
description: "Java 21 Record & JPA/Querydsl 최적화 가이드"
---

# Persistence & Query Guide

## 1. Entity Rules
- **Access:** `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 필수.
- **Immutability:** `@Setter` 금지. 의미 있는 비즈니스 메서드 사용.
- **Fetch:** 모든 연관 관계 `FetchType.LAZY` 강제 (N+1 방지).
- **BaseEntity:** 생성/수정 시간 자동 관리용 상속 클래스 사용 권장.
- **Description:** 각 Entity 필드에 JavaDoc 주석 필수.

## 2. DTO Mapping
- **Standard:** 반환 시 Java 21 `record` 필수 사용.
- **Method:** Entity 내 `toResponse()` 또는 별도 `Mapper`로 계층 분리.

## 3. Query Strategy
- **JPA:** 단순 조회는 Method Name Query 사용.
- **Querydsl:** 동적 쿼리/복잡한 Join 시 필수 사용 (Type-safe).
- **N+1:** 필요 시 `fetchJoin()` 명시적 활용.

## 4. Performance
- **ReadOnly:** 조회 전용 메서드 `@Transactional(readOnly = true)`.
- **Processing:** 대량 데이터는 Java 21 Stream API 활용.

## 5. Sample
```java
@Entity @Getter @NoArgsConstructor(access = PROTECTED)
public class Member extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;
    private String name;
    public MemberResponse toResponse() { return new MemberResponse(id, name); }
}