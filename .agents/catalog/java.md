# Java Skills Catalog

Skills for Java 25 / Spring Boot 4 development. Load the relevant skill before writing code in that domain.

---

## Skill Index

| Skill                             | Path                                             | Load When                                                                                                 |
| --------------------------------- | ------------------------------------------------ | --------------------------------------------------------------------------------------------------------- |
| `java-lombok`                     | `.agents/skills/java-lombok`                     | Writing any Spring bean, service, component, entity, or class needing injection/builders                  |
| `java-logging`                    | `.agents/skills/java-logging`                    | Adding log statements, handling exceptions inline, or writing global request exception handlers           |
| `java-modern`                     | `.agents/skills/java-modern`                     | Writing any new Java class — enforces records, sealed types, pattern matching, text blocks                |
| `java-nullsafety`                 | `.agents/skills/java-nullsafety`                 | Creating any new class, package, or interface — `@NullMarked` on every package is mandatory               |
| `java-functional`                 | `.agents/skills/java-functional`                 | Writing any logic involving collections, filtering, mapping, optional values, or result types             |
| `java-imports`                    | `.agents/skills/java-imports`                    | Writing any Java class — Google Java import order, no wildcard imports                                    |
| `openapi-contract-first`          | `.agents/skills/openapi-contract-first`          | Designing OpenAPI specs before implementation or configuring generated Spring interfaces                  |
| `java-openapi`                    | `.agents/skills/java-openapi`                    | Implementing controllers from generated OpenAPI interfaces                                                |
| `spring-security-jwt`             | `.agents/skills/spring-security-jwt`             | Configuring JWT resource-server security, authorization, method security, or security tests               |
| `spring-observability`            | `.agents/skills/spring-observability`            | Adding metrics, tracing, structured logging, actuator endpoints, health checks, or dashboards             |
| `java-metrics`                    | `.agents/skills/java-metrics`                    | Adding custom Micrometer counters, timers, gauges, metric tags, actuator metric exposure, or metric tests |
| `spring-resilience4j`             | `.agents/skills/spring-resilience4j`             | Calling downstream services with circuit breakers, retries, timeouts, or fallbacks                        |
| `spring-data-jpa-entities`        | `.agents/skills/spring-data-jpa-entities`        | Creating or changing JPA entities, repositories, transactions, or persistence mappings                    |
| `flyway-postgres`                 | `.agents/skills/flyway-postgres`                 | Adding PostgreSQL schema migrations, indexes, constraints, data migrations, or migration tests            |
| `spring-exceptions-problemdetail` | `.agents/skills/spring-exceptions-problemdetail` | Creating API exceptions, `@RestControllerAdvice`, validation errors, or `ProblemDetail` responses         |

---

## Decision Guide

### "I'm creating a new package"

→ Load **`java-nullsafety`** first — every package needs `package-info.java` with `@NullMarked`.

### "I'm creating a new Spring service / component / repository"

→ Load **`java-lombok`**: constructor injection via `@RequiredArgsConstructor`.

### "I'm creating or changing a JPA entity / repository"

→ Load **`spring-data-jpa-entities`** + **`flyway-postgres`**: mappings and migrations must evolve together.

### "I'm adding authentication or authorization"

→ Load **`spring-security-jwt`**

### "I'm creating or changing REST endpoints"

→ Load **`openapi-contract-first`** + **`java-openapi`**

### "I'm calling a downstream service"

→ Load **`spring-resilience4j`** and **`spring-observability`**

### "I'm adding metrics, tracing, health checks, or logs"

→ Load **`spring-observability`** and **`java-logging`**

### "I'm adding custom application metrics"

→ Load **`java-metrics`** plus the baseline Java skills. Also load **`spring-observability`** for actuator/export changes.

### "I'm creating exception handling"

→ Load **`spring-exceptions-problemdetail`** and **`java-logging`**

### "I'm adding logging or handling exceptions inline"

→ Load **`java-logging`**: structured key/value logging, exception stack traces, and no catch-log-rethrow blocks.

### "I'm creating a DTO, request body, response body, or event"

→ Load **`java-modern`**: all data carriers are records — no POJOs with getters.

### "I'm writing collection logic, filtering, mapping, or handling Optional"

→ Load **`java-functional`**: stream pipelines over loops, no `Optional.get()`, sealed result types.

### "I'm writing a new class of any kind"

→ Load **`java-modern`** + **`java-nullsafety`** as a baseline pair.

---

## Quick Rules (always apply, no skill load needed)

- No `@Autowired` on fields — `@RequiredArgsConstructor` only
- No `System.out.println` — `@Slf4j` + structured `log.atInfo().addKeyValue(...)`
- No catch-log-rethrow blocks — either handle and log, convert without logging, or let exceptions pass through
- No raw `null` returns from public methods — `Optional<T>` or throw
- No wildcard imports — every import is declared explicitly
- No `Collectors.toList()` — use `Stream.toList()`
- No manual `instanceof` casts — use pattern matching binding variables
- Switch expressions, not switch statements
- REST APIs are contract-first: update `openapi/openapi.yaml`, then regenerate interfaces
- All config in `application.yaml`, never `.properties`
- All DB migrations are Flyway SQL in `src/main/resources/db/migration/`

---

## Package Structure

```
com.demo.skills
├── <domain>/           ← one package per bounded context (e.g. order, payment, user)
│   ├── package-info.java
│   ├── <Domain>Controller.java
│   ├── <Domain>Service.java
│   ├── <Domain>Repository.java
│   ├── <Domain>.java               ← JPA entity
│   └── dto/
│       ├── package-info.java
│       ├── Create<Domain>Request.java   ← record
│       └── <Domain>Response.java        ← record
└── config/
    ├── package-info.java
    └── <Feature>Config.java
```
