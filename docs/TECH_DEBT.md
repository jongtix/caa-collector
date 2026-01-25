# Technical Debt (기술 부채)

프로젝트에서 알고 있지만 아직 해결하지 않은 기술적 개선 사항들을 기록합니다.

## 1. Converter autoApply 개선

### 현재 상태
모든 Entity에서 Enum 필드마다 `@Convert` 어노테이션을 명시해야 함:

```java
@Entity
public class WatchlistStock {

    @Convert(converter = MarketCodeConverter.class)
    private MarketCode marketCode;

    @Convert(converter = AssetTypeConverter.class)
    private AssetType assetType;
}
```

### 문제점
- Enum 필드가 추가될 때마다 반복 작업
- 코드 중복
- 휴먼 에러 가능성 (Converter 누락)

### 개선 방안

#### 방안 1: Hibernate @TypeDef (권장)

**구현**:
```java
// BaseEntity 또는 패키지 레벨에 선언
@TypeDef(name = "marketCode", typeClass = MarketCodeType.class, defaultForType = MarketCode.class)
@TypeDef(name = "assetType", typeClass = AssetTypeType.class, defaultForType = AssetType.class)
package com.custom.trader.common.entity;

// UserType 구현 필요
public class MarketCodeType implements UserType<MarketCode> {
    @Override
    public int getSqlType() { return Types.INTEGER; }

    @Override
    public Class<MarketCode> returnedClass() { return MarketCode.class; }

    @Override
    public MarketCode nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        Integer code = rs.getInt(position);
        return rs.wasNull() ? null : MarketCode.fromCode(code);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, MarketCode value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.INTEGER);
        } else {
            st.setInt(index, value.getCode());
        }
    }

    // 기타 메서드 구현...
}

// Entity에서는 어노테이션 불필요!
private MarketCode marketCode;  // 자동 변환
```

**장점**:
- Entity 코드 깔끔
- 전역 설정 (모든 MarketCode 필드에 자동 적용)
- Hibernate 표준 방식

**단점**:
- UserType 구현 코드량 증가 (각 Enum마다 구현체 필요)
- Hibernate 종속
- AttributeConverter보다 복잡

**적용 시기**: Phase 2 또는 Enum이 5개 이상으로 증가할 때

---

#### 방안 2: Custom Meta Annotation

**구현**:
```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Convert(converter = MarketCodeConverter.class)
public @interface MarketCodeField {
}

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Convert(converter = AssetTypeConverter.class)
public @interface AssetTypeField {
}

// 사용
@Entity
public class WatchlistStock {

    @MarketCodeField
    private MarketCode marketCode;

    @AssetTypeField
    private AssetType assetType;
}
```

**장점**:
- 의미 명확 (MarketCodeField라는 이름으로 의도 표현)
- 구현 간단 (어노테이션만 추가)
- AttributeConverter 재사용

**단점**:
- 여전히 어노테이션 필요
- Enum마다 커스텀 어노테이션 생성 필요

**적용 시기**: 현재 Converter가 너무 장황하다고 느껴질 때

---

### 우선순위
- **우선순위**: P2 (낮음)
- **이유**: 현재 방식으로도 동작하며, Enum이 4-5개 수준이므로 크게 불편하지 않음
- **트리거**: Enum이 10개 이상으로 증가하거나, Entity가 20개 이상으로 증가할 때 재검토

### 관련 파일
- `MarketCodeConverter.java`
- `AssetTypeConverter.java`
- 모든 Entity 클래스
