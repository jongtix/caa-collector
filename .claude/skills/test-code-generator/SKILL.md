---
name: test-code-generator
description: "Java 21, JUnit 5, Mockito 기반 테스트 자동 생성"
---

# Test Generation Guide

## 1. Core Principles
- **Structure:** `// given`, `// when`, `// then` 주석 필수 사용.
- **Naming:** 한국어로 되어 있는 `해야_할_일` 형식 권장.
- **Isolate:** `@Transactional` 사용으로 DB 상태 독립성 보장.

## 2. Layer Strategy
- **Unit (Service):** `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks` 사용.
- **Slicing (Controller):** `@WebMvcTest`, `MockMvc` 활용 (Status, JSON 검증).
- **Persistence (Repository):** `@DataJpaTest` (H2 In-memory) 사용.

## 3. Java 21 & SB 4 Rules
- **Record:** DTO 테스트 시 생성자로 직접 인스턴스화.
- **AssertJ:** `assertThat().isEqualTo()` Fluent API 사용.
- **Async:** Virtual Thread 테스트 시 `Thread.sleep` 대신 `Awaitility` 사용.

## 4. Sample
```java
@Test
@DisplayName("회원 가입 성공")
void register_success() {
    // given
    var req = new UserRequest("test@test.com", "pw");
    given(userRepo.save(any())).willReturn(new User(1L, "test@test.com"));
    // when
    var res = userService.register(req);
    // then
    assertThat(res.email()).isEqualTo("test@test.com");
    verify(userRepo, times(1)).save(any());
}