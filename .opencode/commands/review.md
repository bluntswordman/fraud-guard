---
description: Run five-axis code review before merge (parallel fan-out)
---

Invoke the `code-review-and-quality` skill with parallel fan-out.

## Steps

1. Identify changed files (`git diff --name-only main...HEAD`)
2. Run **in parallel:**
   - `.opencode/agents/java-reviewer.md` (correctness, tests, performance, maintainability)
   - `.opencode/agents/security-reviewer.md` (security axis)
3. Merge both reports into a single review
4. Produce verdict: `APPROVE | REQUEST_CHANGES | BLOCK`
5. If `REQUEST_CHANGES` or `BLOCK`: list blocking issues with `file:line` + fix

**Do not merge reports until both reviewers complete. Blocking findings from either are blocking.**
