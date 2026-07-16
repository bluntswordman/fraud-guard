---
name: springboot-tdd
description: Test-driven development for Java Spring Boot services. Red-Green-Refactor enforced. Use when implementing any feature in detection-svc or ingestion-svc.
---

# Spring Boot TDD

## Overview

Write the test first, watch it fail, write the minimum code to pass, then refactor. For Spring Boot this means: write the unit/integration test, run `./mvnw test`, see red, implement, see green, refactor.

## When to Use

- Implementing any task from `tasks/todo.md`
- Adding a new endpoint, service method, or repository query
- Fixing a bug (write a test that reproduces the bug first)

---

## The Red-Green-Refactor Loop

1. **RED** — Write a failing test. Run `./mvnw test -Dtest=ClassName`. Confirm it **FAILS** for the right reason.
2. **GREEN** — Write the minimum code to make the test pass. Run again. Confirm GREEN.
3. **REFACTOR** — Improve code quality without changing behavior. Run again. Confirm still GREEN.

**Never skip the RED step.** If the test passes immediately, the test is wrong or the code already exists.

---

## Test Pyramid for Spring Boot

```
        /\
       /e2e\           ← Few. Full app tests
      /------\
     /integration\     ← Some. @SpringBootTest, @DataJpaTest, MockMvc
    /--------------\
   /      unit      \  ← Many. Plain JUnit 5, Mockito mocks
  /------------------\
```

## Test Layers & When to Use Each

| Layer | Annotation | What it tests | Speed |
|---|---|---|---|
| Unit | `@Test` (plain) | Service logic, no Spring context | Fast (<1s) |
| Repository | `@DataJpaTest` | JPA queries, entity mapping | Medium |
| Controller | `@WebMvcTest` + `MockMvc` | HTTP layer, serialization, validation | Medium |
| Integration | `@SpringBootTest` | Full context, wiring | Slow |

## Naming Convention

```
TransactionServiceTest.java          // unit
TransactionRepositoryTest.java       // @DataJpaTest
TransactionControllerTest.java       // @WebMvcTest
IngestionSVCApplicationTests.java    // @SpringBootTest
```

Test methods: `should_<expected>_when_<condition>()`.

---

## Example: Service Unit Test

```java
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @InjectMocks
    private TransactionService service;

    @Test
    void should_returnTransactions_when_findAllCalled() {
        // given
        when(repository.findAll(any(Pageable.class)))
            .thenReturn(Page.empty());

        // when
        var result = service.findAll(Pageable.unpaged());

        // then
        assertThat(result).isNotNull();
        verify(repository).findAll(any(Pageable.class));
    }
}
```

## Example: Controller MockMvc Test

```java
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private TransactionService service;

    @Test
    void should_return200_when_uploadCsvCalled() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", "step,type,...".getBytes());

        mockMvc.perform(multipart("/api/transactions/upload").file(file))
            .andExpect(status().isOk());
    }
}
```

---

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "I'll add tests after it works" | Untested code is unverifiable code. The test is the spec. |
| "It's just a getter, no test needed" | Getters still need serialization tests at the controller layer. |
| "Integration tests are enough" | Integration tests are slow and hide unit-level bugs. Pyramid, not inverted. |
| "Mocking is overkill" | Mocks isolate the unit under test. Without them, failures cascade. |

## Verification (Exit Gate)

- [ ] New code has at least one failing-then-passing test
- [ ] `./mvnw test` passes with no failures
- [ ] Service-layer coverage ≥ 80% (`./mvnw test jacoco:report`)
- [ ] No test is `@Disabled` without a linked TODO issue
- [ ] Test names follow `should_X_when_Y` convention
