# DECLARATIONS.md — Non-Negotiable Standards

This file is the single source of truth for the three binding standards every AI agent and human contributor must apply on the FraudGuard detection platform. Reading this file is mandatory at session start.

## Binding Standards

### 1. Java Design Patterns
- **Source:** `https://github.com/iluwatar/java-design-patterns` (Java 17+, full GoF + enterprise + concurrency catalog)
- **Rule file:** `.opencode/rules/java-design-patterns.md`
- **Binding:** Every non-trivial class must map to a named pattern from the catalog, or document why none applies. Implementation follows the canonical structure in the repo's `<pattern>/README.md`.

### 2. OWASP Top 10
- **Source:** `https://owasp.org/www-project-top-ten/` (2021 edition canonical for review; 2025 edition is the latest release)
- **Rule file:** `.opencode/rules/owasp-top10.md`
- **Binding:** Every change touching controllers, persistence, config, external calls, or auth must pass the A01–A10 checklist. Any FAIL is a REVIEW-gate BLOCK.

### 3. OWASP ASVS
- **Source:** `https://owasp.org/www-project-application-security-verification-standard/` (v5.0.0 stable)
- **Rule file:** `.opencode/rules/owasp-asvs.md`
- **Binding:** Project target is **Level 2**. L1 is the floor for any merge. L2 is required for auth, persistence, file handling, external calls. L3 is required for crypto, multi-tenancy, audit. PRs must declare affected `ASVS-V*` IDs.

---

## Enforcement Hooks

### Lifecycle Gate

```
DEFINE → PLAN → SECURE-DESIGN (this declaration) → BUILD → VERIFY → REVIEW → SHIP
```

The SECURE-DESIGN gate invokes the `secure-design-review` skill, which consumes all three rule files and emits `design-report.md`. `BUILD` refuses to start if the report is missing or `verdict ≠ PASS`.

### Session Start
At session start, the agent loads:
1. `AGENTS.md` (master navigation)
2. This `DECLARATIONS.md`
3. `.opencode/rules/java-design-patterns.md`
4. `.opencode/rules/owasp-top10.md`
5. `.opencode/rules/owasp-asvs.md`
6. Top 6 instincts from `INSTINCTS.md`

### Review Gate
`/review` runs OWASP Top 10 scan and ASVS verification in parallel with code review. Security findings from either axis are blocking.

### Ship Gate
`/ship` requires:
- `design-report.md` verdict = PASS
- No OWASP A01–A10 FAIL
- All declared `ASVS-V*` requirements verified

---

## Anti-Rationalization (Project-Wide)

| Rationalization | Reality |
|---|---|
| "These standards slow us down" | A 30-minute design review prevents multi-day security rework and audit findings. |
| "We'll apply patterns/security before launch" | Retrofitting patterns reshapes APIs; retrofitting security breaks contracts. Both belong upstream. |
| "The AI agent will handle it" | The agent enforces these standards only if they are loaded into context. This file ensures that. |
| "OWASP is the security team's job" | OWASP Top 10 and ASVS L1/L2 are developer controls. The security team audits; developers implement. |

---

## Versioning
- Standards versions are pinned here. Updates require a PR editing this file and re-running `secure-design-review` on affected features.
- Last reviewed: 2026-07-16
- Next review: quarterly or on major framework upgrade
