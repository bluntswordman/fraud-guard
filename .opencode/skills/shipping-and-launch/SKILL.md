---
name: shipping-and-launch
description: Final gate before merge/release. Runs code-review and security review in parallel, verifies build, and produces release notes. Use when a feature is ready to ship.
---

# Shipping and Launch

## Overview

The ship phase is the last gate. It runs the full verification suite, ensures docs are updated, and produces a mergeable PR with release notes.

---

## Pre-Ship Gate

```
┌─────────────────────────────────────────────────┐
│  1. /review  (code-review-and-quality)           │
│  2. /security (security-and-hardening)           │ ← parallel
│  3. ./mvnw clean verify                          │
│  4. Docs sync (docs/11-API-SPEC.md, etc.)        │
│  5. Release notes                                │
└─────────────────────────────────────────────────┘
```

---

## Checklist

- [ ] All tasks in `tasks/todo.md` checked
- [ ] `code-review-and-quality` verdict = APPROVE
- [ ] `security-and-hardening` verdict = PASS
- [ ] `./mvnw clean verify` passes (includes tests)
- [ ] No SNAPSHOT dependencies bumped to release
- [ ] `docs/11-API-SPEC.md` updated if API changed
- [ ] `docs/07-DATABASE-SCHEMA.md` updated if schema changed
- [ ] `docs/10-SETUP-GUIDE.md` updated if setup changed
- [ ] `design-report.md` verdict = PASS
- [ ] CHANGELOG entry added
- [ ] PR description links to spec (`tasks/plan.md`)

---

## PR Description Template

```markdown
## What
[1-2 sentence summary]

## Why
[Link to spec / BRD / PRD section]

## How
[Key implementation decisions]

## Verification
- [ ] ./mvnw clean verify passes
- [ ] Code review: APPROVE
- [ ] Security review: PASS
- [ ] Coverage ≥ 80% on changed lines

## Spec
Link: tasks/plan.md

## Patterns Applied
| Class | Pattern | Reason |
|---|---|---|
| [class] | [pattern] | [reason] |

## ASVS Requirements
- ASVS-V[X.Y.Z] — [how satisfied]

## Release Notes
- Added: [...]
- Changed: [...]
- Fixed: [...]
```

---

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "Docs can be updated later" | Undocumented shipped features are tech debt. Update before merge. |
| "Security already reviewed similar code" | Each PR is reviewed. Patterns drift. |
| "I'll fix the lint warnings after merge" | Lint failures block the gate. Fix now. |

## Verification (Exit Gate)

- [ ] All checklist items checked
- [ ] PR created with template filled
- [ ] `./mvnw clean verify` green
- [ ] At least one human approval recorded
