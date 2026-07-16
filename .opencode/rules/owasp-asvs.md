# OWASP ASVS — Verification Standard

**Source:** OWASP Application Security Verification Standard v5.0.0 (stable). Three verification levels: L1 (low assurance, pen-testable), L2 (sensitive data), L3 (high assurance, critical).

## Binding Declaration

**Project target: ASVS Level 2** (detection-svc & ingestion-svc handle financial transaction data + potential tenant data).

- L1 controls are MINIMUM for any merge.
- L2 controls are REQUIRED for any feature touching auth, persistence, file handling, or external calls.
- L3 controls are REQUIRED for crypto, multi-tenancy isolation, and audit trails.

**Each PR must declare which ASVS requirements it implements or affects, in the PR description.**

---

## ASVS 5.0 Chapters → Verification Gate

### V1: Architecture & Threat Modeling
- **L2 required:** Threat model documented for features touching cross-service contracts or new external integrations.
- **Verify:** `docs/threat-models/<feature>.md` exists with STRIDE or DFD.

### V2: Authentication
- **L2 required:** Login throttled (Bucket4j), credential rotation policy, no session fixation.
- **Verify:** `@SpringBootTest` proving lockout after N failed attempts; `server.servlet.session.cookie.http-only=true`.

### V3: Session Management
- **L2 required:** Session timeout ≤ 30min idle; logout invalidates server-side session; JWT refresh rotation.
- **Verify:** `server.servlet.session.timeout=30m`; test asserting post-logout endpoint returns 401.

### V4: Access Control
- **L2 required:** Tenant isolation enforced at repository layer (`findByTenantId`), not just controller; IDOR tests exist.
- **L3 required:** Privilege escalation test matrix per role.
- **Verify:** Test that user A cannot read user B's transaction data by ID.

### V5: Validation, Sanitization & Encoding
- **L2 required:** All `@RequestBody` use `@Valid` + Bean Validation; output encoding for any HTML/JSON interpolation.
- **Verify:** `grep -rL "@Valid" src/main/java/**/controller/` returns empty.

### V6: Stored Cryptography
- **L2 required:** Sensitive fields (API keys, PII) encrypted at rest with AES-GCM; keys in KMS/env, not code.
- **L3 required:** Key rotation procedure documented.
- **Verify:** No plaintext sensitive columns; encryption config present.

### V7: Error Handling & Logging
- **L2 required:** `@ControllerAdvice` returns sanitized error shape; no stack traces in responses; security events logged with `event=` structured fields.
- **Verify:** `server.error.include-stacktrace=never`; test that 500 response has no `trace` field.

### V8: Data Protection
- **L2 required:** PII minimized in logs; `@JsonIgnore` on sensitive DTO fields; retention policy documented in `docs/03-NFR.md`.
- **Verify:** grep for forbidden fields (`password`, `token`, `ssn`) in log statements.

### V9: Communications Security
- **L2 required:** TLS 1.2+ enforced; HSTS header; internal service-to-service TLS or mTLS in prod.
- **Verify:** `server.ssl.enabled=true` in prod profile; HSTS filter registered.

### V10: Malicious Code
- **L2 required:** Dependencies from official sources; SBOM (CycloneDX); no `eval`/reflection on user input.
- **Verify:** `cyclonedx-maven-plugin` runs in CI; `dependency-check` clean.

### V11: Business Logic
- **L2 required:** Rate limits on high-value flows (bulk CSV ingestion, detection rule creation); resource exhaustion tests.
- **Verify:** Bucket4j config; test that N+1 rapid requests return 429.

### V12: Files & Resources
- **L2 required (ingestion-svc):** Upload size limit, MIME whitelist, path traversal protection, zip-slip protection, uploaded files stored outside webroot and not executable.
- **L3 required:** Antivirus scan on uploaded files.
- **Verify:** `@WebMvcTest` uploading `../../etc/passwd` returns 400; archive test with `../` entry rejected.

### V13: API & Web Service
- **L2 required:** REST schema documented in `docs/11-API-SPEC.md`; OpenAPI generated; input validation on every endpoint.
- **Verify:** `springdoc-openapi` plugin generates spec matching `docs/11-API-SPEC.md`.

### V14: Configuration
- **L2 required:** Profiles isolated (`default`/`dev`/`test`/`prod`); no prod secrets in repo; hardening script for prod.
- **L3 required:** Immutable infrastructure (container images, no SSH).
- **Verify:** `.gitignore` excludes `application-local.properties`; `docker-compose-prod.yml` has `read_only: true`.

---

## ASVS Requirement ID Convention

Reference requirements in PRs and `design-report.md` as: `ASVS-V4.2.3` (chapter.requirement.sub). This makes audit traceable to the standard.

## Verification Levels Recap

| Level | Applies To | Pen-Testable? |
|---|---|---|
| L1 | All code (minimum bar) | Yes |
| L2 | Auth, persistence, files, external calls | Yes + source review |
| L3 | Crypto, multi-tenancy, audit | Source review + architecture review |

---

## Anti-Rationalization

| Rationalization | Reality |
|---|---|
| "ASVS is for security teams" | ASVS is for developers. L1/L2 controls are code-level. |
| "We'll do L2 before launch" | L2 controls shape schema and endpoints. Retrofitting breaks contracts. |
| "L3 is overkill" | Multi-tenant isolation without L3 is a breach waiting to happen. |
| "Verification IDs are bureaucracy" | IDs make findings auditable and traceable to the standard. |
