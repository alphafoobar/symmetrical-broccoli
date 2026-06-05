# Copilot Instructions

This is a **Spring Boot 4 / Java 25** project. Code must be modern, opinionated, and consistent.

## Tech Stack

- Java 25, Spring Boot 4, Spring MVC (not WebFlux)
- Spring Data JPA + Flyway + PostgreSQL
- Spring Security
- Lombok
- Micrometer tracing + Prometheus
- Resilience4j circuit breaker
- Google Java Checkstyle

## Skills

Detailed, enforceable rules live in `.agents/skills/`. Discover which skill to load via the catalogs:

| Domain         | Catalog                        |
| -------------- | ------------------------------ |
| Java           | `.agents/catalog/java.md`      |
| OpenAPI        | `.agents/catalog/openapi.md`   |
| Testing        | `.agents/catalog/testing.md`   |
| OpenTofu/infra | `.agents/catalog/terraform.md` |

**Workflow skill** — for reviewing or auditing the codebase:

| Task        | Skill                        |
| ----------- | ---------------------------- |
| Code review | `.agents/skills/code-review` |

## Non-Negotiable Defaults

- Every new class is in the correct sub-package under `com.demo.skills`
- No `@Autowired` on fields — constructor injection via `@RequiredArgsConstructor`
- No raw `null` returns — use `Optional<T>` or throw
- No `System.out.println` — use `@Slf4j`
- All DTOs / response bodies are **records** with `@Builder`
- All multi-field types constructed via `.builder()...build()` — never positional `new X(a, b, c)`
- All local variables use Lombok `val` — never Java `var`
- No wildcard imports (`import foo.*` or `import static foo.*`) — every import is explicit
- Imports follow Google Java Checkstyle ordering
- REST APIs are contract-first: update `openapi/openapi.yaml`, then generate Spring interfaces with OpenAPI Generator
- Do not use SpringDoc-generated OpenAPI as the source of truth
- All database migrations are Flyway SQL files in `src/main/resources/db/migration/`
- All configuration is in `application.yaml` (never `.properties`)
