# OWASP Top 10 — Always-Aware Security Rules

**Source:** OWASP Top 10 (2021 edition canonical for code review; 2025 edition is the latest release).

## Binding Declaration

Every code change touching controllers, persistence, configuration, external calls, or auth MUST pass the checklist below. **Failure on any category is a REVIEW-gate BLOCK.**

---

## Categories → Concrete Spring Boot Rules

### A01: Broken Access Control

**Rule:** Every endpoint handling sensitive data enforces `@PreAuthorize` or filter-chain auth.

```java
// VULNERABLE
@GetMapping("/api/transactions")
public List<Transaction> all() { return repo.findAll(); }

// SAFE
@GetMapping("/api/transactions")
@PreAuthorize("hasRole('ANALYST')")
public Page<TransactionDto> all(@AuthenticationPrincipal UserPrincipal user, Pageable p) {
    return repo.findByTenantId(user.getTenantId(), p);
}
```

**Verify:** `@WithMockUser(roles="GUEST")` test must return 403.

---

### A02: Cryptographic Failures

**Rule:** Passwords hashed with BCrypt (cost ≥ 12); sensitive fields encrypted at rest; TLS enforced.

**Vulnerable:** MD5, plain SHA-256 without salt, storing API keys in `application.properties`.

```java
// SAFE
@Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(12); }
```

**Verify:** `grep -r "MD5\|SHA1\|DES\b" src/` returns nothing; secrets are `${ENV}`.

---

### A03: Injection

**Rule:** All DB queries use parameterized JPA/`@Query`; no string-concatenated SQL/JPQL; no `EntityManager.createNativeQuery(userInput)`.

```java
// VULNERABLE
@Query("SELECT t FROM Transaction t WHERE t.type LIKE '%" + type + "%'")

// SAFE
@Query("SELECT t FROM Transaction t WHERE LOWER(t.type) LIKE LOWER(CONCAT('%', :type, '%'))")
Page<Transaction> searchByType(@Param("type") String type, Pageable p);
```

**ingestion-svc:** File path params canonicalized and bounded to base dir; command exec forbidden on user input.

---

### A04: Insecure Design

**Rule:** Every feature passes `secure-design-review` skill before implementation. Threat model recorded for high-risk features.

**Vulnerable:** Shipping an upload endpoint without size/MIME/rate limits because "MVP".

**Safe:** `spring.servlet.multipart.max-file-size=10MB`, MIME whitelist, rate limiter.

**Verify:** `design-report.md` exists and `verdict = PASS` for the feature.

---

### A05: Security Misconfiguration

**Rule:** `server.error.include-stacktrace=never`; `management.endpoints.web.exposure.include=health,info` only; CORS explicit, not `*`; debug disabled in prod profile.

```properties
# application-prod.properties — SAFE
server.error.include-stacktrace=never
management.endpoints.web.exposure.include=health,info
spring.web.cors.allowed-origins=https://your-domain.com
```

**Verify:** `application-prod.properties` review against this list.

---

### A06: Vulnerable and Outdated Components

**Rule:** No SNAPSHOT deps in prod; OWASP Dependency Check runs in CI; CVE CRITICAL/HIGH blocks ship.

**Safe:** Parent POM pins versions; `./mvnw org.owasp:dependency-check-maven:check` in CI gate.

**Verify:** `dependency-check-report.html` with no HIGH/CRITICAL findings.

---

### A07: Identification and Authentication Failures

**Rule:** Login rate-limited; session/JWT expiry enforced; credentials rotated; no default credentials.

**Vulnerable:** No lockout, JWT never expires, `admin/admin` in test profile leaking to prod.

**Safe:** Bucket4j on `/auth/login`; JWT TTL ≤ 15min with refresh rotation; `@Profile("test")` on any seeded credentials.

---

### A08: Software and Data Integrity Failures

**Rule:** CI pipeline signed; dependencies verified (`mvnw --strict-checksums`); no `curl | bash` in Dockerfiles; SBOM generated.

**Vulnerable:** Unsigned CI, `RUN curl https://x | sh` in Dockerfile.

**Safe:** `mvnw --strict-checksums`; CycloneDX BOM via `org.cyclonedx:cyclonedx-maven-plugin`.

---

### A09: Security Logging and Monitoring Failures

**Rule:** Auth events (login, logout, failed, privilege change) logged at INFO with structured fields; no sensitive data in logs.

```java
// SAFE
@Slf4j
void onLoginFail(String user, String ip) {
    log.info("event=auth_fail user={} ip={} ts={}", user, ip, Instant.now());
}
```

**Verify:** `grep` for `event=` structured logs; alert rules exist for `auth_fail` threshold.

---

### A10: Server-Side Request Forgery (SSRF)

**Rule:** `ingestion-svc` never fetches user-supplied URLs without an allowlist; outbound HTTP uses hardened `RestTemplate` with blocked private ranges.

**Vulnerable:** `new URL(userUrl).openConnection()` with no validation.

**Safe:** URL allowlist (scheme `https`, host in allowlist), reject private IPs (`10/8`, `172.16/12`, `192.168/16`, `127/8`), redirect-follow disabled.

---

## Anti-Rationalization

| Rationalization | Reality |
|---|---|
| "It's an internal service" | Lateral movement is real. A01/A05 still apply. |
| "We'll add auth later" | A04 Insecure Design. Retrofitting auth changes every endpoint signature. |
| "The framework handles it" | Spring handles DI. It does not pick `@PreAuthorize` scopes or validate your DTOs. |
| "Injection is a solved problem" | JPA still allows `createNativeQuery`. The rule still bites. |
