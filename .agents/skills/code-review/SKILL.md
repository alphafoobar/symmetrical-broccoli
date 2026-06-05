---
name: code-review
description: "Full project code review. Scans openapi/, src/, infra/, build.gradle.kts, and README.md against REQUIREMENTS.md and the skills selected from .agents/catalog. Use when asked to review, audit, or check the codebase for convention violations or unimplemented requirements."
---

# Code Review Workflow

## Scope

Review the following locations in order:

| Location           | What to check                                                                              |
| ------------------ | ------------------------------------------------------------------------------------------ |
| `REQUIREMENTS.md`  | Load first — establishes the functional requirements every other check is measured against |
| `build.gradle.kts` | Dependencies, plugins, Java toolchain version, Lombok config                               |
| `src/main/`        | All production Java source and resources                                                   |
| `src/test/`        | All test source                                                                            |
| `openapi/`         | OpenAPI spec files (if present)                                                            |
| `infra/`           | OpenTofu infrastructure-as-code (if present)                                               |
| `README.md`        | Accuracy — does it reflect the actual project?                                             |

## Step 1 — Run Mechanical Checks

Before source review, run the project validation commands and capture failures as findings:

- Run `./gradlew check` when available; it should cover compilation, tests, Checkstyle, ArchUnit, JaCoCo coverage verification, OpenAPI validation, Error Prone, NullAway, and project convention tests when configured.
- If `check` is unavailable or too broad for the current repository, run the closest explicit Gradle tasks, such as `./gradlew test checkstyleMain checkstyleTest jacocoTestReport jacocoTestCoverageVerification`.
- If infrastructure files under `infra/` changed and OpenTofu is configured, run `tofu fmt -check` and `tofu validate` in the relevant module or environment.
- Do not stop the review just because checks fail. Record the failed task, key error, and likely owner area, then continue reviewing the code.
- If a command cannot be run because a tool, daemon, container runtime, credentials, or network access is unavailable, report that as an explicit validation gap.

## Step 2 — Inspect Catalogs and Load Relevant Skills

Before reading source files, inspect the relevant catalogs in `.agents/catalog/` and use each catalog's "Load When" column to choose the smallest skill set that covers the code under review:

| Catalog                        | Use when reviewing                                                                              |
| ------------------------------ | ----------------------------------------------------------------------------------------------- |
| `.agents/catalog/java.md`      | `src/main/java/`, `src/test/java/`, `build.gradle.kts`, or Java-related resources               |
| `.agents/catalog/openapi.md`   | `openapi/`, generated API interfaces, controllers, DTOs, or API error contracts                 |
| `.agents/catalog/testing.md`   | `src/test/`, test fixtures, test infrastructure, or convention tests                            |
| `.agents/catalog/terraform.md` | `infra/` OpenTofu modules, providers, variables, outputs, plans, or state-related configuration |

After choosing skills from the catalogs, load and follow the referenced `.agents/skills/*/SKILL.md` files before reviewing that domain.

For full project reviews, always include the baseline Java skills listed in `AGENTS.md` when Java source is present:

- `.agents/skills/java-modern/SKILL.md`
- `.agents/skills/java-nullsafety/SKILL.md`
- `.agents/skills/java-imports/SKILL.md`

When multiple domains are present, review each domain against the skills selected by its catalog rather than relying on a hard-coded skill list in this workflow.

## Step 3 — Requirements Check

Read `REQUIREMENTS.md`. For each requirement:

- [ ] Is it implemented?
- [ ] Is it tested?
- [ ] Note any gap as a finding with the requirement reference

## Step 4 — Java Source Review (`src/main/java/`)

Walk every package and class. For each, check:

**Imports**

- [ ] No wildcard imports (`import foo.*`, `import static foo.*`)
- [ ] Every used symbol has an explicit import

**Package**

- [ ] `package-info.java` exists with `@NullMarked`
- [ ] Class is in the correct sub-package under `com.demo.skills`

**Class structure**

- [ ] No `@Autowired` on fields — `@RequiredArgsConstructor` only
- [ ] No `@Setter` on entities
- [ ] No `@Data` anywhere
- [ ] Services/components use `@Slf4j` — no `LoggerFactory` calls
- [ ] No `System.out.println`
- [ ] Log statements use structured key/value fields for variables
- [ ] No catch-log-rethrow blocks — exceptions are handled, converted without logging, or allowed through
- [ ] Inline handled exceptions are logged at `warn` or higher with stack traces

**Local variables**

- [ ] Every local variable uses `val` — no `var`, no repeated explicit types
- [ ] No split declaration + try-catch initialisation — extract to a method instead
- [ ] Method parameters declared `final`

**DTOs / records**

- [ ] All DTOs, request/response types are records
- [ ] Every record with more than one field has `@Builder`
- [ ] All construction uses `.builder()...build()` — no positional `new X(a, b, c)`

**Null safety**

- [ ] No raw `null` returns from public methods — `Optional<T>` or throw
- [ ] `@Nullable` only on fields/parameters that are intentionally nullable

**Controllers**

- [ ] Endpoint changes start in `openapi/openapi.yaml`
- [ ] Controllers implement generated OpenAPI interfaces
- [ ] Generated API/model code is not hand-edited
- [ ] Errors use `ProblemDetail` (RFC 9457)
- [ ] Global request exception handler logs most request-processing exceptions once with stack traces

**Security**

- [ ] Stateless JWT resource-server configuration
- [ ] Public endpoints are explicit and minimal
- [ ] Role/scope checks are request-level or method-level, not ad hoc controller logic
- [ ] Tests cover unauthenticated, forbidden, and authorized paths

**Persistence**

- [ ] JPA entities use controlled mutation, no `@Data`, no `@Setter`
- [ ] Required columns are `nullable = false` in mappings and `not null` in migrations
- [ ] Aggregate roots use optimistic locking where concurrent writes are possible
- [ ] Repository methods return `Optional<T>` for absent single-row lookups

**Resilience and observability**

- [ ] Downstream network calls have timeout and circuit-breaker coverage
- [ ] Retries are only used for idempotent operations
- [ ] Metrics use bounded low-cardinality tags
- [ ] Logs do not include secrets, JWTs, or raw request/response bodies

**Functional style**

- [ ] No imperative loops where a stream pipeline would be clearer
- [ ] No `Optional.get()` — always chained or terminated with `orElseThrow`
- [ ] No `else` after a `return` — use early-return / guard-clause style
- [ ] `Stream.toList()` not `Collectors.toList()`

## Step 5 — Test Review (`src/test/java/`)

For each test class, identify its type (unit / web slice / data slice / integration / security / architecture) and check against the corresponding skill.

**All tests**

- [ ] `val` for all local variables
- [ ] No wildcard imports
- [ ] `@DisplayName` on every `@Test` method
- [ ] BDD comments: `// given`, `// when`, `// then`
- [ ] Builders for all test data — no scattered magic literals

**Unit tests**

- [ ] `@ExtendWith(MockitoExtension.class)` — no Spring context
- [ ] `BDDMockito.given(...)` — not `Mockito.when(...)`
- [ ] `@Nested` groups related scenarios

**Slice / integration tests**

- [ ] No `@MockBean` (removed in Spring Boot 4) — use `@MockitoBean`
- [ ] Testcontainers via `@ServiceConnection` — no `@DynamicPropertySource`
- [ ] No H2 — real PostgreSQL via Testcontainers

## Step 6 — Configuration

- [ ] `src/main/resources/application.yaml` exists (not `.properties`)
- [ ] No configuration in `.properties` files
- [ ] Flyway migrations in `src/main/resources/db/migration/` with sequential versioning (`V1__`, `V2__`, …)

## Step 7 — Build (`build.gradle.kts`)

- [ ] Java toolchain targets version 25
- [ ] Lombok dependency and annotation processor configured
- [ ] Google Java Checkstyle configured and part of `check`
- [ ] JaCoCo report and coverage verification configured and part of `check`
- [ ] Error Prone + NullAway configured for `@NullMarked` code
- [ ] ArchUnit test dependency present when architecture rules exist
- [ ] OpenAPI Generator plugin configured for contract-first generated interfaces
- [ ] `spring-boot-starter-test` present (covers JUnit 5, Mockito, AssertJ)
- [ ] Testcontainers BOM imported
- [ ] No deprecated or conflicting dependency versions

## Step 8 — OpenAPI Spec (`openapi/`)

If OpenAPI spec files are present, select the matching skills from `.agents/catalog/openapi.md` and verify:

- [ ] `openApiValidate` passes
- [ ] Generated interfaces compile
- [ ] Every implemented controller corresponds to a generated interface
- [ ] Error responses declare `application/problem+json` content type

## Step 9 — OpenTofu Infrastructure (`infra/`)

If `infra/` is present, select the matching skills from `.agents/catalog/terraform.md` and verify:

- [ ] `tofu fmt` and `tofu validate` pass for edited modules
- [ ] Providers and modules are version-pinned
- [ ] AWS provider `default_tags` include `project`, `environment`, and `managed-by = "opentofu"`
- [ ] Remote state is configured and local state files are not committed
- [ ] Environment-specific values are variables, not hardcoded account IDs, regions, credentials, or secrets
- [ ] Sensitive outputs are marked `sensitive = true`
- [ ] ECS, RDS, networking, secrets, logs, alarms, dashboards, and metric widgets follow the catalog-selected AWS skills

## Step 10 — README (`README.md`)

- [ ] Describes what the project does
- [ ] Documents how to run locally (prerequisites, `./gradlew bootRun`)
- [ ] Documents how to run tests (`./gradlew test`)
- [ ] Documents any environment variables or config required

## Output Format

Report findings grouped by category. For each finding:

```
[CATEGORY] File: path/to/File.java (line N if known)
Rule violated: <short description from the relevant skill>
Fix: <concrete correction>
```

Summarise with counts at the end:

```
Requirements gaps:  X
Mechanical failures: X
Java violations:    X
Test violations:    X
Config violations:  X
Build violations:   X
OpenAPI violations: X
Infra violations:   X
```
