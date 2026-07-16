---
description: Implement the next task from tasks/todo.md using TDD
---

Invoke the `springboot-tdd` skill, delegating implementation to the `springboot-engineer` agent.

## Steps

1. Read `tasks/todo.md`, pick the next unchecked task
2. Read corresponding `tasks/plan.md` section
3. **GATE:** Confirm `design-report.md` exists and `verdict = PASS` — if not, invoke `/security-design` first
4. Delegate to `.opencode/agents/springboot-engineer.md`
5. Follow Red-Green-Refactor strictly
6. Run `./mvnw test` after each task
7. Check off the task in `tasks/todo.md`
8. If build error: delegate to `.opencode/agents/java-build-resolver.md`
9. If logic bug: invoke `debugging-and-error-recovery` skill

**Do not skip the RED step. Do not batch tasks.**
