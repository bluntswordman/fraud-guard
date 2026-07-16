---
name: code-review-and-quality
description: Five-axis code review before merge. Use before any PR or when reviewing changes. Runs java-reviewer and security-reviewer in parallel, then merges.
---

# Code Review and Quality

## Overview

Every change set is reviewed across five axes before it ships. This is not a vibe check — each axis has explicit criteria and evidence requirements.

## The Five Axes

1. **Correctness** — Does it do what the spec says? Are edge cases handled?
2. **Testability & Tests** — Is it tested? Does coverage meet the bar? Are tests meaningful or ceremonial?
3. **Security** — Input validation? SQL injection? Secrets? (Delegates to `security-and-hardening`)
4. **Performance** — N+1 queries? Unbounded collections? Blocking calls?
5. **Maintainability** — Is it readable? Does it follow `rules/java-springboot.md`? Is complexity under control?

---

## Parallel Fan-Out Pattern

Run reviewers concurrently, then synthesize:

```
              ┌─→ java-reviewer (correctness, tests, perf, maintainability)
changes ──────┤
              └─→ security-reviewer (security axis)
                          ↓
                     merge report
```

---

## Review Checklist (per axis)

### Correctness
- [ ] Implementation matches `tasks/plan.md` and spec acceptance criteria
- [ ] Null handling: `Optional`, `@Nullable`, null checks where needed
- [ ] Exception handling: custom exceptions, no swallowed exceptions
- [ ] Boundary conditions: empty collections, max-size, negative amounts

### Testability & Tests
- [ ] New code has unit tests (service layer)
- [ ] New endpoints have MockMvc tests
- [ ] Coverage ≥ 80% on changed lines (`./mvnw test jacoco:report`)
- [ ] Tests assert behavior, not implementation
- [ ] No `@Disabled` tests without justification

### Security
- [ ] All inputs validated (`@Valid`, Bean Validation)
- [ ] No string-concatenated SQL/JPQL — use JPA/`@Query` with parameters
- [ ] No secrets in code or `application.properties` (use env vars)
- [ ] `server.error.include-stacktrace=never`

### Performance
- [ ] No N+1 queries (use `@EntityGraph` or `fetch join`)
- [ ] No unbounded `findAll()` — paginate (`Pageable`)
- [ ] No blocking I/O in async paths
- [ ] Caching considered for hot reads (`@Cacheable`)
- [ ] Batch insert for CSV ingestion (not row-by-row)

### Maintainability
- [ ] Follows `.opencode/rules/java-springboot.md` (naming, package layout)
- [ ] Cyclomatic complexity ≤ 10 per method
- [ ] No god classes (> 500 lines)
- [ ] Public API has Javadoc
- [ ] No TODO/FIXME without linked issue

---

## Output Format

```
## Code Review: [PR/branch name]
**Verdict:** APPROVE | REQUEST_CHANGES | BLOCK

### Correctness: PASS / FAIL
[evidence]

### Tests: PASS / FAIL
- Coverage on changed lines: [%]
- Missing tests: [list]

### Security: PASS / FAIL
[findings]

### Performance: PASS / WARN
[concerns]

### Maintainability: PASS / WARN
[complexity, style issues]

### Required Changes (blocking)
- [file:line] — [issue] — [fix]

### Suggestions (non-blocking)
- [suggestion]
```

---

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "Tests pass, ship it" | Passing tests ≠ correct. Review the five axes. |
| "It's a small change, skip review" | Small changes cause production incidents too. |
| "Performance is fine for now" | N+1 queries don't get faster with scale. Fix now. |
| "Security will review later" | Shift left. Security is a review axis, not a phase. |

## Verification (Exit Gate)

- [ ] All five axes reviewed with evidence
- [ ] Verdict recorded (APPROVE / REQUEST_CHANGES / BLOCK)
- [ ] Blocking issues have file:line + fix
- [ ] `./mvnw test` and `./mvnw verify` pass
