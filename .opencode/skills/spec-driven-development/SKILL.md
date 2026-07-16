---
name: spec-driven-development
description: Creates specs before coding. Use when starting a new feature, endpoint, or significant change in detection-svc or ingestion-svc, and no specification exists yet. Use when requirements are unclear or only exist as a vague idea.
---

# Spec-Driven Development

## Overview

Write a structured specification before writing any code. The spec is the shared source of truth — it defines what we're building, why, and how we'll know it's done. Code without a spec is guessing.

## When to Use

- Starting a new endpoint, service module, or cross-service feature
- Requirements are ambiguous or incomplete
- The change touches multiple files or modules
- You're about to make an architectural decision
- Task would take more than 30 minutes

**When NOT to use:** Single-line fixes, typo corrections, log level changes.

---

## The Gated Workflow

```
SPECIFY ──→ PLAN ──→ TASKS ──→ IMPLEMENT
   │           │        │          │
   ▼           ▼        ▼          ▼
 Human       Human    Human      Human
reviews     reviews  reviews    reviews
```

Do not advance to the next phase until the current one is validated.

---

## Phase 1: Specify

Surface assumptions immediately before writing spec content:

```
ASSUMPTIONS I'M MAKING:
- This is a REST endpoint (not gRPC)
- Persistence uses JPA/Hibernate against PostgreSQL
- Detection is synchronous; ingestion is async via Redis Pub/Sub
- Java 17 LTS, Spring Boot 4.1.0
→ Correct me now or I'll proceed with these.
```

Write a spec covering six core areas:

1. **Objective** — What & why. User stories / acceptance criteria.
2. **Commands** — Full executable commands:
   ```
   Build:   ./mvnw clean package -DskipTests
   Test:    ./mvnw test
   Run dev: ./mvnw spring-boot:run
   ```
3. **Project Structure** —
   ```
   ingestion-svc/src/main/java/com/meingehirn/ingestion_svc/
   ├── controller/  → REST endpoints
   ├── service/     → Business logic
   ├── repository/  → JPA repositories
   ├── model/       → Entities & DTOs
   └── config/      → Spring configuration
   ```
4. **Code Style** — One real snippet beats three paragraphs.
5. **Testing Strategy** — JUnit 5, MockMvc for controllers, `@DataJpaTest` for repos, ≥80% coverage on service layer.
6. **Boundaries** —
   - **Always do:** Run tests before commits, validate inputs, use existing DTOs
   - **Ask first:** DB schema changes, adding dependencies, changing `application.properties`
   - **Never do:** Commit secrets, edit `target/`, remove failing tests, hardcode URLs

**Spec template:**
```
# Spec: [Feature Name]

## Objective
[What & why. Acceptance criteria.]

## Tech Stack
[Spring Boot version, key deps with versions]

## Commands
[Build, test, run]

## Project Structure
[Directory layout for affected service]

## Code Style
[Example snippet + conventions]

## Testing Strategy
[Framework, locations, coverage target]

## Boundaries
- Always: [...]
- Ask first: [...]
- Never: [...]

## Success Criteria
[Testable conditions]

## Open Questions
[Unresolved items needing human input]
```

---

## Phase 2: Plan

With validated spec, generate implementation plan:
1. Identify components & dependencies
2. Determine implementation order
3. Note risks & mitigations
4. Identify parallel vs sequential work
5. Define verification checkpoints

Save plan to `tasks/plan.md`, task list to `tasks/todo.md`.

---

## Phase 3: Tasks

- Each task completable in one focused session
- Each has explicit acceptance criteria
- Each includes a verification step
- Ordered by dependency, not importance
- No task touches more than ~5 files

**Task template:**
```
## Task: [Description]
- Acceptance: [What must be true when done]
- Verify: [How to confirm — ./mvnw test, build, manual check]
- Files: [Which files will be touched]
- Service: detection-svc | ingestion-svc
```

---

## Phase 4: Implement

Execute tasks one at a time following `springboot-tdd`. Load the right spec sections at each step.

---

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "This is simple, I don't need a spec" | Simple tasks need short specs, but still need acceptance criteria. |
| "I'll write the spec after I code it" | That's documentation, not specification. Spec value is clarity *before* code. |
| "The spec will slow us down" | A 15-minute spec prevents hours of rework. |
| "Requirements will change anyway" | That's why the spec is a living document. An outdated spec beats no spec. |

## Red Flags

- Writing code without any written requirements
- Asking "should I just start building?" before clarifying "done"
- Implementing features not in any spec or task list
- Making architectural decisions without documenting them

## Verification (Exit Gate)

- [ ] Spec covers all six core areas
- [ ] Assumptions are listed and acknowledged
- [ ] Success criteria are testable
- [ ] Plan saved to `tasks/plan.md`, tasks to `tasks/todo.md`
