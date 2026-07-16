---
name: debugging-and-error-recovery
description: Systematic debugging for Java Spring Boot. Use when a test fails, build breaks, or runtime error occurs. Delegates build errors to java-build-resolver agent.
---

# Debugging and Error Recovery

## Overview

Debug methodically: reproduce, isolate, hypothesize, test, fix. Never change code blindly to "make the error go away." Every fix must have a proven cause.

## When to Use

- A test fails unexpectedly
- Build fails (`./mvnw compile` or `./mvnw test`)
- Runtime exception in service
- CI pipeline red

---

## The Debug Loop

1. **REPRODUCE** — Get the failure to happen reliably. Note exact steps/command.
2. **ISOLATE** — Narrow to the smallest reproducing case. Which test? Which class? Which line?
3. **HYPOTHESIZE** — Form a falsifiable hypothesis: "X is null because Y wasn't initialized."
4. **TEST** — Verify the hypothesis (add a log, run the test, inspect state).
5. **FIX** — Make the minimal change that addresses the root cause.
6. **REGRESSION** — Add a test that reproduces the original failure. Confirm it now passes.

---

## Delegation Rules

- **Build error** (compilation failure, Maven error) → delegate to `java-build-resolver` agent
- **Logic error** (wrong output, test failure) → handle here with the debug loop
- **Cross-service error** → use `architect` agent to trace the call chain

---

## Common Spring Boot Failure Modes

| Symptom | Likely Cause | First Check |
|---|---|---|
| `NullPointerException` | Missing `@Autowired` / uninitialized field | Stack trace line, field initialization |
| `BeanCreationException` | Missing bean or circular dependency | `@ComponentScan` base packages, `@Configuration` |
| `LazyInitializationException` | Session closed before collection access | `@Transactional` scope, `@EntityGraph` |
| `HttpMessageNotReadableException` | Invalid JSON request body | DTO fields, `@Valid`, Jackson config |
| `DataIntegrityViolationException` | Constraint violation | DB schema vs entity mapping |
| `BeanDefinitionStoreException` | Duplicate bean definition | Two `@Service` classes with same name |
| Redis `ConnectionRefusedException` | Redis not running | Docker container status, port 6379 |

---

## Output Format

```
## Debug Session: [issue summary]

### Reproduction
- Command: ./mvnw test -Dtest=XTest#should_Y
- Frequency: always / intermittent

### Isolation
- Smallest reproducer: [test name / endpoint call]

### Hypothesis
[Root cause hypothesis — falsifiable]

### Evidence
[Logs, stack trace, state inspection confirming hypothesis]

### Fix
- File: path/to/File.java
- Change: [minimal change]
- Root cause: [why this fixes it]

### Regression Test
[Test added that reproduces original failure]
- Result: PASS
```

---

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "Let me just try changing this" | Random changes mask bugs, they don't fix them. Hypothesize first. |
| "The test is flaky" | Flaky tests hide real races. Investigate until deterministic. |
| "It works on my machine" | Environment differences are bugs. Reproduce in Docker. |
| "I'll add a try-catch to suppress it" | Swallowed exceptions hide root causes. Never swallow. |

## Verification (Exit Gate)

- [ ] Root cause identified and documented
- [ ] Minimal fix applied (no unrelated changes)
- [ ] Regression test added and passes
- [ ] Original failing test passes
- [ ] `./mvnw test` fully green
