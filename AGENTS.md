# AGENTS.md

This is a Spring Boot 4 / Java 25 REST API project.

Before writing or reviewing code, inspect the relevant catalog:

- Java: `.agents/catalog/java.md`
- OpenAPI: `.agents/catalog/openapi.md`
- Testing: `.agents/catalog/testing.md`
- OpenTofu/infra: `.agents/catalog/terraform.md`

Load and follow the relevant `.agents/skills/*/SKILL.md` files before changing code in that domain. Use the catalog "Load When" column to choose the smallest set of skills that covers the current change.

Always load these baseline skills before writing Java:

- `.agents/skills/java-modern/SKILL.md`
- `.agents/skills/java-nullsafety/SKILL.md`
- `.agents/skills/java-imports/SKILL.md`

Load additional skills by task:

- Spring beans, Lombok builders, constructor injection: `.agents/skills/java-lombok/SKILL.md`
- Logging, inline exception handling, global request exception logging: `.agents/skills/java-logging/SKILL.md`
- Collections, streams, Optional, result modelling: `.agents/skills/java-functional/SKILL.md`
- REST contract design or endpoint changes: `.agents/skills/openapi-contract-first/SKILL.md`
- Controller implementations, generated API interfaces, DTOs, error handlers: `.agents/skills/java-openapi/SKILL.md`
- Unit, slice, integration, security, or architecture tests: inspect `.agents/catalog/testing.md` and load the matching test skill
- OpenTofu/infra: inspect `.agents/catalog/terraform.md`

For code reviews, follow `.agents/skills/code-review/SKILL.md`.

Mechanical enforcement lives in the build and must be kept aligned with the skills:

- Google Java Checkstyle enforces wildcard import bans, import order, public Javadocs, and formatting.
- ArchUnit enforces package boundaries, no field injection, no Lombok `@Data`, no entity setters, and controller layering conventions.
- Error Prone + NullAway enforce null-safety for `@NullMarked` code.
- OpenAPI Generator validates `openapi/openapi.yaml` and generates Spring REST interfaces.
- JUnit convention tests enforce `application.yaml`, Flyway migration naming, and `package-info.java` coverage.

Project defaults:

- Java 25
- Spring Boot 4
- Spring MVC, not WebFlux
- Spring Data JPA, Flyway, PostgreSQL
- Spring Security
- Lombok
- Micrometer tracing and Prometheus
- Resilience4j
