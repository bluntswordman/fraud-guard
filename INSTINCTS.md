# INSTINCTS.md — Learned Patterns

This file persists lessons learned across sessions, following the ECC instinct model with confidence scoring (threshold ≥ 0.7 for injection at session start).

## Format

Each instinct has:
- **Pattern**: what was learned
- **Context**: when it applies
- **Confidence**: 0.0–1.0 (injected at session start if ≥ 0.7)
- **Source**: session/PR where learned
- **Date**: when recorded

---

## Active Instincts

### [INSTINCT-001] Constructor injection via @RequiredArgsConstructor
- **Pattern:** All `@Service`, `@RestController`, `@Component` classes in both services use Lombok `@RequiredArgsConstructor` for constructor injection. Never use field `@Autowired`.
- **Context:** When writing any new Spring bean in detection-svc or ingestion-svc
- **Confidence:** 0.95
- **Source:** AGENTS.md rules/java-springboot.md
- **Date:** 2026-07-16

### [INSTINCT-002] NUMERIC(18,2) for all monetary columns
- **Pattern:** All columns representing money use `NUMERIC(18,2)` in PostgreSQL. Never use `FLOAT`, `DOUBLE`, or `REAL`. In Java, map to `BigDecimal`.
- **Context:** When creating JPA entities with monetary fields (amount, balance, etc.)
- **Confidence:** 0.95
- **Source:** docs/07-DATABASE-SCHEMA.md
- **Date:** 2026-07-16

### [INSTINCT-003] Strategy Pattern for detection rules
- **Pattern:** Each fraud detection rule implements `DetectionStrategy` interface with `getName()` + `detectFraud(Transaction tx)`. Spring IoC collects all `@Component` impls into `List<DetectionStrategy>` injected to `DetectionEngine`. Never use if-else chains.
- **Context:** When adding new fraud detection rules in detection-svc
- **Confidence:** 0.9
- **Source:** docs/06-DESIGN-PATTERNS.md
- **Date:** 2026-07-16

### [INSTINCT-004] ingestion-svc file uploads must validate MIME and size
- **Pattern:** `IngestionController` accepts multipart uploads; always validate MIME type, filename (path traversal prevention), and enforce `spring.servlet.multipart.max-file-size` before persisting.
- **Context:** When modifying ingestion-svc upload endpoints
- **Confidence:** 0.85
- **Source:** docs/03-NFR.md + OWASP A04/A12
- **Date:** 2026-07-16

### [INSTINCT-005] Publish Redis event AFTER database commit
- **Pattern:** Ingestion service publishes transaction ID to Redis channel `fraudguard.tx.channel` only after `saveAll()` commits successfully. Never publish inside the same `@Transactional` before commit.
- **Context:** When implementing the ingestion → detection pub/sub flow
- **Confidence:** 0.9
- **Source:** docs/04-ARCHITECTURE.md
- **Date:** 2026-07-16

### [INSTINCT-006] Detection service must be idempotent
- **Pattern:** Before updating `is_flagged_fraud`, detection-svc checks `if (tx.getIsFlaggedFraud()) return;`. The UPDATE uses `WHERE id = ? AND is_flagged_fraud = FALSE` to prevent double-processing Redis events.
- **Context:** When implementing DetectionEngine event handling
- **Confidence:** 0.9
- **Source:** docs/09-SLA.md
- **Date:** 2026-07-16

---

## Expired / Superseded
*(none yet)*

---

### [INSTINCT-007] TransactionRepository query methods use Spring Data JPA derived names with tenantId scope
- **Pattern:** Repository methods for strategy queries follow pattern `countBy<Fields>AndTenantId` / `existsBy<Fields>AndTenantId`. All detection queries are tenant-scoped. Never use raw native queries for strategy checks.
- **Context:** When adding new detection strategies that need historical data lookup in detection-svc
- **Confidence:** 0.85
- **Source:** VelocityStrategy, LifecycleStrategy implementation
- **Date:** 2026-07-16

### [INSTINCT-008] Dual output pattern: Redis result channel + Webhook HTTP delivery
- **Pattern:** After DetectionEngine produces DetectionResult, `RedisSubscriberService` and `RecoveryJob` publish to Redis `fraudguard.result.channel` (internal) AND call `webhookService.deliver(result)` (external). Redis for internal consumers, webhooks for external systems (ERP, membership, payment gateway). Webhook delivery is `@Async`, uses RestTemplate with 5s connect / 10s read timeout, SSRF-protected (HTTPS only, private IP blocked).
- **Context:** When integrating FraudGuard results with external systems
- **Confidence:** 0.85
- **Source:** RedisSubscriberService, RecoveryJob, WebhookService implementation
- **Date:** 2026-07-16

### [INSTINCT-009] LifecycleStateMachine via LifecycleStrategy
- **Pattern:** Membership/loyalty point systems enforce sequential state: TOPUP → REDEEM → EARN. LifecycleStrategy uses `Map<String,String>` (`REQUIRED_PRIOR`) mapping each type to its required predecessor. `existsByNameOrigAndTypeAndStepLessThanAndTenantId` checks historical data. Violation → `isFlaggedFraud=true`. New lifecycle rules added by extending the map.
- **Context:** When validating transaction state machines for membership, loyalty points, or ticket systems
- **Confidence:** 0.85
- **Source:** LifecycleStrategy implementation
- **Date:** 2026-07-16

---

## How to Update
At session end, extract patterns and add here. Patterns below confidence 0.7 are kept but not injected at session start. Patterns not reinforced within 30 days decay by 0.1. Use format above exactly.
