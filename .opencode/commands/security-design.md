---
description: Run the SECURE-DESIGN gate — pattern appropriateness + OWASP Top 10 + ASVS verification
---

Invoke the `secure-design-review` skill.

## Steps

1. Read `DECLARATIONS.md` and the three rule files:
   - `.opencode/rules/java-design-patterns.md`
   - `.opencode/rules/owasp-top10.md`
   - `.opencode/rules/owasp-asvs.md`
2. Read `tasks/plan.md` (if missing, invoke `/plan` first)
3. Execute the three-axis review:
   - **Axis 1:** Pattern appropriateness per new class
   - **Axis 2:** OWASP A01–A10 checklist (PASS / FAIL / N-A per category)
   - **Axis 3:** ASVS level (target L2; list affected `ASVS-V*` IDs)
4. For L3 features, write STRIDE threat model to `docs/threat-models/<feature>.md`
5. Produce `design-report.md` next to `tasks/plan.md`
6. If `verdict = BLOCK`: list blocking issues and required fixes; do NOT proceed to `/build`
7. If `verdict = PASS`: log to `INSTINCTS.md` any new pattern/security decision learned this session

**Output:** `design-report.md` with verdict + human sign-off line.
