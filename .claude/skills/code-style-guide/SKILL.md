---
name: code-style-guide
description: "Java 21, Spring Boot 3, Thymeleaf 스타일 가이드"
---

# Java 21 & SB 3 Style Guide

## 1. Tech Stack
- Java 21, Spring Boot 3.5.9, Gradle.

## 2. Coding Rules
- **Java 21:** DTO/API 객체는 `record` 필수, 지역 변수는 `var` 사용, `switch/instanceof` 패턴 매칭 우선, I/O 작업 시 Virtual Threads 고려.
- **Spring Boot:** 생성자 주입 필수(`@RequiredArgsConstructor`), RESTful `ResponseEntity<T>` 반환, `@RestControllerAdvice` 전역 예외 처리, `application.yml` 사용.

## 3. Naming
- **Package:** lowercase
- **Naming:** `*Controller`, `*Service`, `*Repository`, `*Entity` (JPA)

## 4. Self-Check
- [ ] Java 21 `record` 사용?
- [ ] 생성자 주입(No `@Autowired`)?
- [ ] SB 4 규격 준수?
- [ ] Setter 지양 및 불변성 유지?