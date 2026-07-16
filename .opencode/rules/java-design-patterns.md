# Java Design Patterns — Mandatory Reference

**Source:** `https://github.com/iluwatar/java-design-patterns` (Java 17+, 4000+ commits, full GoF + enterprise + concurrency catalog).

## Binding Declaration

Before writing any class that solves a non-trivial problem, you MUST:

1. Identify which pattern from the catalog below applies (or justify why none applies).
2. Implement following the canonical structure in the repo's `<pattern>/README.md`.
3. Record the chosen pattern in the PR description under "Patterns Applied".

**Skipping this step is a REVIEW-gate failure.**

---

## Pattern Catalog (mapped to detection-svc & ingestion-svc)

### Creational — object construction

| Pattern | Use When (in this project) | Canonical Spring Boot Snippet | Anti-Pattern |
|---|---|---|---|
| **Builder** | DTOs with >4 fields, immutable request/response objects | `TransactionDto.builder().amount(...).type(...).build()` with Lombok `@Builder` | Telescoping constructors, setters on immutable DTOs |
| **Abstract Factory** | Creating detection strategies per transaction-type family | `DetectionStrategyFactory.forType(TxType.TRANSFER)` returns `TransferDetectionStrategy` | `switch` statements scattered in services |
| **Factory Method** | Creating `Transaction` instances from CSV rows with variant config | `Transaction.fromCsvRow(row)` static factory | `new` with conditional logic in callers |
| **Prototype** | Cloning detection rule templates for bulk configuration | `ruleTemplate.copy()` via copy-constructor | Manual field-by-field copy |
| **Singleton** | Stateless Spring beans (default by `@Service`/`@Component`) | `@Service` — Spring manages singleton scope | Hand-rolled `getInstance()` with `synchronized` |

### Behavioral — object interaction

| Pattern | Use When | Snippet | Anti-Pattern |
|---|---|---|---|
| **Strategy** | Pluggable fraud detection algorithms (velocity, balance anomaly) | `DetectionStrategy` interface + `@Component` impls, injected via `List<DetectionStrategy>` by Spring | `if-else` chains on algorithm type |
| **Chain of Responsibility** | Multi-stage ingestion pipeline (validate → filter → persist → publish) | `IngestionHandler` interface, `@Order`-annotated handlers, `@Autowired List<IngestionHandler>` | God-service doing all stages in one method |
| **Template Method** | Common ingestion skeleton with format-specific steps | `abstract class AbstractCsvIngestionService { final void ingest() {...} }` with `@Override` hooks | Copy-pasted service classes |
| **Command** | Audit-loggable, replayable detection actions | `DetectionCommand` interface with `execute()`, queued for retry | Direct service calls with no encapsulation |
| **Observer** | Domain events: fraud flagged → notify multiple subscribers | Spring `@EventListener` / `ApplicationEventPublisher` | Direct calls to every subscriber in publisher |
| **Visitor** | Applying operations across stable `DetectionResult` type hierarchy | `DetectionResultVisitor` with `visit(FraudMatch)`, `visit(CleanResult)` | `instanceof` chains |

### Structural — composition

| Pattern | Use When | Snippet | Anti-Pattern |
|---|---|---|---|
| **Adapter** | Wrapping a legacy/third-party detection library into `DetectionStrategy` | `LegacyDetectorAdapter implements DetectionStrategy` | Leaking third-party types into your domain |
| **Facade** | Simplifying a complex subsystem for the controller layer | `IngestionFacade.upload(file)` delegating to parser+validator+repo+publisher | Controllers calling 5 services |
| **Decorator** | Adding cross-cutting concerns (caching, logging, metrics) without altering core | `@Decorator`-style wrapper or Spring AOP `@Around` | Mixing caching logic into business method |
| **Proxy** | Lazy loading heavy detection models | `@Lazy` Spring proxy | Eager initialization of all strategies at startup |
| **Composite** | Tree-shaped rule composition (AND/OR rule groups) | `RuleComponent` with `Rule` (leaf) and `RuleGroup` (composite) | Recursive `if` over rule types |

### Concurrency — ingestion-svc heavy use

| Pattern | Use When | Snippet | Anti-Pattern |
|---|---|---|---|
| **Active Object** | Decoupling ingestion method invocation from execution | `@Async` method returning `CompletableFuture` | Blocking the request thread for long CSV processing |
| **Balking** | Skip processing if precondition not met | `synchronized` guard: `if (!ready) return;` | Starting work that will fail halfway |
| **Circuit Breaker** | Calls to external APIs that may fail | Resilience4j `@CircuitBreaker` on external detector | Retry storms cascading failures |
| **Backpressure** | Stream ingestion faster than detection can process | Project Reactor `onBackpressureBuffer` | Unbounded queue causing OOM |

### Enterprise / Architectural

| Pattern | Use When | Snippet | Anti-Pattern |
|---|---|---|---|
| **Anti-Corruption Layer** | Translating CSV column names to domain model | `CsvRowTranslator` mapping `nameOrig` → `nameOrig` domain field | CSV column names leaking into services/repos |
| **Front Controller** | Single entry for all API requests | Spring `DispatcherServlet` (default) + `@RestControllerAdvice` | Multiple servlets/filters doing routing |
| **Intercepting Filter** | Auth, logging, request-id propagation | Spring `OncePerRequestFilter` beans in security chain | Auth logic inside controllers |

---

## Decision Heuristic

- Solving a **creation problem**? → Creational (Builder / Factory / Abstract Factory)
- Solving an **interaction problem**? → Behavioral (Strategy / Chain / Template / Command)
- Solving a **composition problem**? → Structural (Adapter / Facade / Decorator / Proxy)
- Solving a **concurrency problem**? → Concurrency (Active Object / Circuit Breaker / Backpressure)
- Solving an **integration problem**? → Enterprise (ACL / Front Controller / Intercepting Filter)

When two patterns seem to fit, prefer the one with fewer classes. A pattern's value is reducing cognitive load, not adding ceremony.

---

## Anti-Rationalization

| Rationalization | Reality |
|---|---|
| "This is too small for a pattern" | Small problems still need the right structure. Builder for a 5-field DTO is not overkill. |
| "I'll refactor to a pattern later" | Retrofitting patterns into tangled code is 5x harder than applying them upfront. |
| "Patterns add complexity" | Misapplied patterns add complexity. The right pattern removes it. |
| "Spring already handles this" | Spring implements Front Controller, DI, Proxy. It does not pick your domain patterns. |
