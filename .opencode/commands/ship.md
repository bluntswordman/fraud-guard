---
description: Final gate — run review, security, build verify, and prepare PR
---

Invoke the `shipping-and-launch` skill.

## Steps

1. Confirm all tasks in `tasks/todo.md` are checked
2. Confirm `design-report.md` exists and `verdict = PASS`
3. Run `/review` (parallel fan-out: `java-reviewer` + `security-reviewer`)
4. Run `/security` (standalone security check)
5. Run `./mvnw clean verify`
6. Sync docs:
   - `docs/11-API-SPEC.md` if API changed
   - `docs/07-DATABASE-SCHEMA.md` if schema changed
   - `docs/10-SETUP-GUIDE.md` if setup changed
7. Generate release notes (Added/Changed/Fixed)
8. Create PR using the template in `.opencode/skills/shipping-and-launch/SKILL.md`

**Gate: no step may be skipped. If any step fails, the ship is blocked.**
