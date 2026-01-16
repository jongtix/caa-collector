---
name: error-handling-master
description: "SB 4 전역 예외 및 공통 응답 규격"
---

# Error & Response Guide

## 1. API Response
- **Format:** `record ApiResponse<T>(boolean success, T data, String message, LocalDateTime timestamp)`

## 2. Exception Strategy
- **Global:** `@RestControllerAdvice` 중앙 관리.
- **Custom:** `BusinessException` (extends `RuntimeException`) 기반.
- **Mapping:** `Enum` 에러 코드 사용 (Status Code 매핑 필수).

## 3. Implementation
- **Validation:** `@Valid` 실패 시 `MethodArgumentNotValidException` 필드 상세 반환.
- **Java 21:** `switch` 패턴 매칭으로 핸들러 로직 최적화.

## 4. Sample
```java
public record ApiResponse<T>(boolean success, T data, String message) {}

@ExceptionHandler(BusinessException.class)
public ResponseEntity<ApiResponse<Void>> handle(BusinessException e) {
    return ResponseEntity.status(e.getErrorCode().getStatus())
        .body(new ApiResponse<>(false, null, e.getMessage()));
}