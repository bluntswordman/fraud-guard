---
name: secure-design-review
description: Mandatory pre-implementation gate. Checks design-pattern appropriateness (rules/java-design-patterns.md), runs OWASP Top 10 scan (rules/owasp-top10.md), and verifies ASVS level (rules/owasp-asvs.md). Use for every non-trivial feature before BUILD phase.
---

# Secure Design Review

## Overview

This skill is the SECURE-DESIGN gate between PLAN and BUILD. It produces a binding `design-report.md` that must reach `verdict: PASS` before `springboot-tdd` may proceed.

## When to Use (MANDATORY triggers)

- Any new endpoint, service, or repository
- Any change to auth, crypto, file handling, or external calls
- Any schema change
- Any new dependency
- Any feature spanning detection-svc ↔ ingestion-svc

**When NOT to use:** Pure refactor with no behavior change, typo/log-level fixes (still log the skip in the PR).

---

## The Three-Axis Review

```
┌──────────────────────────────────────────────────┐
│  AXIS 1: Design Pattern Appropriateness          │
│  rules/java-design-patterns.md                   │
│  → Which pattern? Justify or pick from catalog   │
├──────────────────────────────────────────────────┤
│  AXIS 2: OWASP Top 10 Scan                       │
│  rules/owasp-top10.md                            │
│  → A01–A10 checklist, each PASS/FAIL/N-A         │
├──────────────────────────────────────────────────┤
│  AXIS 3: ASVS Level Verification                 │
│  rules/owasp-asvs.md                             │
│  → Target L2; list affected ASVS-V* IDs          │
└──────────────────────────────────────────────────┘
```

---

## Process

### Step 1: Pattern Appropriateness

1. Read `tasks/plan.md` for the feature.
2. For each new class/module, consult `.opencode/rules/java-design-patterns.md`:
   - Identify the problem family (creation / interaction / composition / concurrency / integration).
   - Pick the canonical pattern from the catalog.
   - If no pattern fits, write a one-paragraph justification.
3. Record: `class → pattern → reason`.

### Step 2: OWASP Top 10 Scan

For each category A01–A10 in `.opencode/rules/owasp-top10.md`:
- Determine if the change is in scope (touches that risk).
- If in scope: apply the "Safe" snippet pattern, mark PASS only if the implementation plan conforms.
- If out of scope: mark N/A with reason.
- Any FAIL → verdict BLOCK.

### Step 3: ASVS Level Verification

1. Determine feature risk tier:
   - **L1 minimum:** any code change.
   - **L2 required:** auth, persistence, file handling, external calls, API surface.
   - **L3 required:** crypto, multi-tenant isolation, audit trails.
2. For the target level, list every `ASVS-V*` requirement the feature affects.
3. For each, state how the plan satisfies it (test, config, code pattern).
4. Any unmet L2/L3 requirement → verdict BLOCK.

### Step 4: Cross-Cutting Threat Model (for L3 features only)

Write a short STRIDE analysis to `docs/threat-models/<feature>.md`:
- Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege.
- One mitigation per identified threat.

---

## Output: `design-report.md`

```markdown
# Design Report: [feature name]
## Date: [ISO date]
## Author: [agent + human reviewer]

## Patterns Applied
| Class / Module | Pattern | Reason |
|---|---|---|
| TransactionService | Strategy | Pluggable detection strategies via Spring IoC |
| IngestionController | Facade | Simplifies CSV pipeline for REST layer |

## OWASP Top 10 Scan
| ID | Category | Scope? | Status | Evidence |
|---|---|---|---|---|
| A01 | Broken Access Control | Yes | PASS | `@PreAuthorize` on new endpoints |
| A02 | Cryptographic Failures | N/A | — | No crypto change |
| A03 | Injection | Yes | PASS | `@Query` with `:param` only |
| A04 | Insecure Design | Yes | PASS | This report exists |
| A05 | Security Misconfiguration | No | N/A | No config change |
| A06 | Vulnerable Components | No | N/A | No new deps |
| A07 | Auth Failures | N/A | — | No auth change |
| A08 | Integrity Failures | N/A | — | No CI/image change |
| A09 | Logging Failures | Yes | PASS | Structured `event=` logs |
| A10 | SSRF | No | N/A | No outbound URL fetch |

## ASVS Verification
- Target Level: L2
- Affected Requirements:
  - ASVS-V5.3 (input validation) — `@Valid` on all DTOs
  - ASVS-V12.1 (upload size/MIME) — `max-file-size=10MB` + MIME whitelist
- Unmet: none

## Threat Model (L3 features only)
[file path or inline STRIDE table]

## Verdict: PASS | BLOCK

## Blocking Issues (if BLOCK)
1. [issue + required fix]
```

Save `design-report.md` alongside `tasks/plan.md`. Downstream `/build` will refuse to proceed if it is absent or `verdict ≠ PASS`.

---

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "We reviewed security already" | OWASP Top 10 ≠ ASVS. Different axes, both required. |
| "This pattern is obvious" | Obvious patterns still need to be named and justified. Naming enables review. |
| "ASVS is too detailed for this size" | L1 is the floor. If the feature touches auth/files, L2 is mandatory regardless of size. |
| "Threat modeling is overkill" | L3 features without threat models are A04 Insecure Design. |

## Verification (Exit Gate)

- [ ] `design-report.md` exists next to `tasks/plan.md`
- [ ] Every new class has a pattern (or justified "none")
- [ ] All in-scope OWASP categories marked PASS
- [ ] All target-level ASVS requirements listed with satisfaction evidence
- [ ] Verdict = PASS
- [ ] Human reviewer signed off (name recorded)
