# Java / Spring Boot Coding Rules — Always Follow

These rules apply to every code change in `detection-svc/` and `ingestion-svc/`. Non-negotiable.

---

## Project Conventions

- Java 17 LTS, Spring Boot 4.1.0
- Package root: `com.meingehirn.<service_name>`
- Service modules: `detection_svc`, `ingestion_svc`
- Build: Maven wrapper (`./mvnw`), never system `mvn`
- Ports: `ingestion-svc` → 7777, `detection-svc` → 7778

---

## Package Layout (per service)

```
com.meingehirn.<service>/
├── controller/     → @RestController endpoints
├── service/        → @Service business logic
├── repository/     → JpaRepository interfaces
├── model/
│   ├── entity/     → JPA @Entity
│   └── dto/        → Request/Response DTOs
├── config/         → @Configuration classes
├── exception/      → Custom exceptions + @ControllerAdvice
└── util/           → Helper utilities (static, pure)
```

---

## Dependency Injection

- Constructor injection only — use Lombok `@RequiredArgsConstructor`
- No `@Autowired` on fields
- No `@Autowired` on setters

```java
// CORRECT
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository repository;
}

// WRONG
@Service
public class TransactionService {
    @Autowired
    private TransactionRepository repository;
}
```

---

## Entities & DTOs

- Never expose `@Entity` directly in API responses — always map to DTO
- Use `@MappedSuperclass` for shared audit fields (`createdAt`, `updatedAt`)
- DTOs use Bean Validation: `@NotNull`, `@NotBlank`, `@Size`, `@Pattern`, `@Email`
- Monetary fields: `BigDecimal` in Java, `NUMERIC(18,2)` in PostgreSQL — never `double`/`float`
- Boolean fields: `Boolean` in Java, native `BOOLEAN` in PostgreSQL — never `TINYINT`

---

## Transactions

- `@Transactional` on service write methods (not controllers, not repositories)
- `@Transactional(readOnly = true)` on read methods
- Never call external services (Redis publish) inside a `@Transactional` block before commit

---

## REST API

- Base path: `/api/<domain>` (e.g., `/api/transactions`)
- Standard methods: `GET` (list/one), `POST` (create), `PUT` (update), `DELETE` (remove)
- Pagination via `Pageable`, return `Page<T>`
- HTTP status codes: 200 OK, 201 Created, 204 No Content, 400 Bad Request, 404 Not Found, 500 Internal
- `@Valid` on all `@RequestBody` parameters — no exceptions

---

## Exception Handling

- Custom exceptions extend a base `ServiceException`
- `@RestControllerAdvice` centralizes error responses
- Error response shape:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "..." }
```

- `server.error.include-stacktrace=never` in all profiles

---

## Testing

- JUnit 5 (`@Test`, `@ExtendWith`)
- Mockito for unit tests (`@Mock`, `@InjectMocks`)
- `@WebMvcTest` + `MockMvc` for controllers
- `@DataJpaTest` for repositories
- `@SpringBootTest` only for full integration
- Test naming: `should_<expected>_when_<condition>`
- Coverage ≥ 80% on service layer (jacoco plugin)

---

## Configuration

- `application.properties` uses `${ENV_VAR}` placeholders — never hardcoded values
- Profiles: `default`, `dev`, `test`, `prod`
- Secrets via environment variables — never committed
- `.gitignore` excludes `application-local.properties`, `.env`

---

## Dependencies

- No SNAPSHOT versions in production paths
- New dependencies require security review (`security-and-hardening` skill)
- Parent POM manages shared dependency versions

---

## Logging

- SLF4J via Lombok `@Slf4j`
- Never log secrets, tokens, passwords, or PII
- Log levels: ERROR (failures), WARN (recoverable), INFO (lifecycle), DEBUG (diagnostics)
- No `System.out.println` in production code
- Structured fields for security events: `log.info("event=auth_fail user={} ip={}", user, ip)`

---

## Code Quality

- No method > 50 lines
- No class > 500 lines
- Cyclomatic complexity ≤ 10 per method
- No TODO/FIXME without a linked issue
- Public API has Javadoc
- Lombok: `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j` — use consistently

---

## FraudGuard-Specific Rules

- `DetectionStrategy` interface: every detection rule must implement it, be annotated `@Component`
- Redis publish: always after DB commit, never inside `@Transactional`
- Detection idempotency: check `is_flagged_fraud` before processing; use conditional UPDATE
- CSV ingestion: filter rows with `amount <= 0`; skip invalid rows, never throw fatal exception
