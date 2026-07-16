---
name: planning-and-task-breakdown
description: Breaks down features into dependency-ordered, verifiable tasks. Use after a spec exists and before implementation. Canonical source for task sizing and dependency ordering.
---

# Planning and Task Breakdown

## Overview

Convert a validated spec into an implementation plan and a dependency-ordered task list. The plan must be reviewable: a human should read it and say "yes, that's the right approach" or "no, change X."

## When to Use

- After `spec-driven-development` Phase 1 completes
- Before invoking `/build` or `springboot-tdd`
- When a feature spans multiple files, services, or modules

---

## Process

### 1. Component Mapping

For each service affected (detection-svc / ingestion-svc):
- List controllers, services, repositories, models touched
- Identify shared contracts (DTOs, API spec in `docs/11-API-SPEC.md`)
- Map cross-service dependencies (does detection-svc call ingestion-svc?)

### 2. Dependency Graph

```
[Entity/Model] → [Repository] → [Service] → [Controller] → [Test]
      ↑                                                         ↓
[DB schema]                                          [Integration test]
```

Build bottom-up: schema → entity → repository → service → controller → test.

### 3. Vertical Slicing

Slice work into vertical increments that each deliver testable value:
- Slice 1: Entity + Repository + unit test
- Slice 2: Service layer + business logic + unit test
- Slice 3: Controller + MockMvc integration test
- Slice 4: Cross-service integration (Redis pub/sub) + contract test

### 4. Task Sizing Rules

- One task = one focused session (≤ 2 hours)
- One task touches ≤ 5 files
- Each task has: acceptance criteria, verification command, file list
- Tasks ordered by dependency, not perceived importance

### 5. Risk Annotation

For each task, mark risk: Low / Medium / High. High-risk tasks need a mitigation note.

---

## Output Convention

Save to `tasks/plan.md` (narrative plan) and `tasks/todo.md` (checkbox list). Create `tasks/` if absent. Downstream commands (`/build`) expect these paths.

## Task Template

```
## [S1-T1] Create Transaction entity
- Service: ingestion-svc
- Acceptance: Entity maps to transactions table with all columns from docs/07-DATABASE-SCHEMA.md
- Verify: ./mvnw test -Dtest=TransactionTest
- Files: model/entity/Transaction.java, test/.../TransactionTest.java
- Risk: Low
- Dependencies: None
```

---

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "I'll just start coding and figure it out" | That's how cross-service contracts break. Plan first. |
| "Tasks are too granular" | Granular tasks are reviewable and resumable. Coarse tasks hide risk. |
| "Dependency order doesn't matter for Spring Boot" | JPA entities must exist before repositories; repositories before services. Order matters. |

## Verification (Exit Gate)

- [ ] `tasks/plan.md` exists with component mapping + dependency graph
- [ ] `tasks/todo.md` exists with checkbox tasks
- [ ] Every task has acceptance criteria + verify command + file list
- [ ] No task touches > 5 files
- [ ] High-risk tasks have mitigations
