# AGENTS.md — FraudGuard Detection Platform

This file configures AI coding agents (OpenCode, Claude Code, Cursor, Codex) working on this repository.
Read this file first. Every session starts here.

---

## Repository Overview

Real-time fraud detection platform (FraudGuard) using mini-microservices architecture.

```
mandiri/
├── ingestion-svc/     → CSV ingestion pipeline (port 7777)
├── detection-svc/     → Fraud detection engine (port 7778)
├── docs/01-BRD.md → docs/11-API-SPEC.md  → canonical project documents
├── .opencode/
│   ├── agents/        → Subagent persona files
│   ├── commands/      → Slash command definitions
│   ├── skills/        → SKILL.md workflow files
│   └── rules/         → Always-follow coding rules
├── AGENTS.md          → This file (master navigation)
├── DECLARATIONS.md    → Binding standards (Design Patterns + OWASP)
└── INSTINCTS.md       → Learned patterns (confidence-scored)
```

---

## Tech Stack

- Java 17, Spring Boot 4.1.0, Maven wrapper (`./mvnw`)
- PostgreSQL 15 (`NUMERIC(18,2)`, BOOLEAN native)
- Redis 7 (Pub/Sub message broker)
- JUnit 5 + MockMvc + Mockito for tests
- Lombok, Spring Data JPA, Spring Validation

---

## Core Rules (Non-Negotiable)

1. **Never read or modify** `target/`, `*.class`, `*.jar`, `HELP.md`, `.DS_Store` — build artifacts.
2. **Skills first:** If task matches a skill in `.opencode/skills/`, invoke it — never implement directly.
3. **Always follow** `.opencode/rules/java-springboot.md` on every code change.
4. **DECLARATIONS binding:** Load `DECLARATIONS.md` at session start. Design Patterns + OWASP Top 10 + ASVS are non-negotiable.
5. **SECURE-DESIGN gate:** Between PLAN and BUILD, invoke `secure-design-review` skill. BUILD refuses to start if `design-report.md` is missing or `verdict ≠ PASS`.
6. **Pattern naming:** Every new class maps to a named pattern from `rules/java-design-patterns.md` or justifies absence in PR.
7. **ASVS target L2:** PRs declare affected `ASVS-V*` IDs. L2 required for auth/persistence/files/external calls; L3 for crypto/tenancy/audit.
8. Consult `docs/` (BRD → PRD → ARCHITECTURE → API-SPEC) before architectural decisions.
9. Run `./mvnw test` before declaring a task complete.
10. Use `./mvnw` wrapper — never system `mvn`.

---

## Intent → Skill Mapping

| User Intent | Skill | Command |
|---|---|---|
| New feature / requirement | `spec-driven-development` | `/spec` |
| Planning / breakdown | `planning-and-task-breakdown` | `/plan` |
| Implement feature | `springboot-tdd` | `/build` |
| Bug / failure | `debugging-and-error-recovery` | `/debug` |
| Code review | `code-review-and-quality` | `/review` |
| Security review | `security-and-hardening` | `/security` |
| Pre-implementation security & design gate | `secure-design-review` | `/security-design` |
| Release | `shipping-and-launch` | `/ship` |

---

## Lifecycle

```
DEFINE → PLAN → SECURE-DESIGN → BUILD → VERIFY → REVIEW → SHIP
```

Each phase has a gate. Do not advance until current phase is validated.

- **SECURE-DESIGN:** `secure-design-review` skill → `design-report.md` (verdict PASS required before BUILD)
- **REVIEW:** re-runs OWASP Top 10 + ASVS (parallel with code review)
- **SHIP:** requires `design-report.md` PASS + no OWASP FAIL + ASVS verified

---

## Agent Delegation

Delegate specialized work to subagents in `.opencode/agents/`:

| Agent | Role |
|---|---|
| `planner.md` | Implementation planning → `tasks/plan.md` + `tasks/todo.md` |
| `architect.md` | System design & ADRs → `docs/adrs/` |
| `springboot-engineer.md` | Spring Boot TDD implementation |
| `java-reviewer.md` | Java/Spring code review (4 axes) |
| `security-reviewer.md` | Vulnerability analysis |
| `java-build-resolver.md` | Maven/build error resolution |

**Rules:** Subagents cannot spawn subagents. Parallel fan-out only for review: run `java-reviewer` + `security-reviewer` concurrently, then merge.

---

## Document Navigation

Read docs in order for context:

| File | Read When |
|---|---|
| `docs/01-BRD.md` | Business context & objectives |
| `docs/02-PRD.md` | Feature requirements & user stories |
| `docs/03-NFR.md` | Non-functional requirements |
| `docs/04-ARCHITECTURE.md` | System architecture & data flow |
| `docs/05-TECH-STACK.md` | Technology choices & justification |
| `docs/06-DESIGN-PATTERNS.md` | Strategy Pattern & Stream API usage |
| `docs/07-DATABASE-SCHEMA.md` | PostgreSQL schema & type choices |
| `docs/08-SQL-QUERIES.md` | Window Functions & CTE queries |
| `docs/09-SLA.md` | Availability & performance targets |
| `docs/10-SETUP-GUIDE.md` | Docker setup & execution |
| `docs/11-API-SPEC.md` | API contracts & request/response shapes |

---

## Memory & Instincts

Learned patterns persist in `INSTINCTS.md` (confidence-scored, threshold ≥ 0.7).
At session start, inject top 6 instincts. Cap SessionStart context at 4000 chars for local models.

---

## Anti-Rationalization

The following thoughts are incorrect and must be ignored:

- "This is too small for a skill"
- "I can just quickly implement this"
- "I'll add tests later"
- "The spec will slow us down"
- "Security review is the security team's job"
- "I'll add patterns/security before launch"
