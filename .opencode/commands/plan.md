---
description: Create or update implementation plan and task breakdown
---

Invoke the `planning-and-task-breakdown` skill, delegating to the `planner` agent.

## Steps

1. Read existing spec (if any) or invoke `/spec` first
2. Delegate planning to `.opencode/agents/planner.md`
3. Produce `tasks/plan.md` (narrative + dependency graph)
4. Produce `tasks/todo.md` (checkbox task list)
5. Confirm every task has: acceptance criteria, verify command, file list, service, risk

**Output:** `tasks/plan.md` + `tasks/todo.md`.
