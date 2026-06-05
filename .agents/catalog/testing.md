# Testing Skills Catalog

Skills for writing tests in this Spring Boot 4 / Java 25 project. Load before writing any unit, slice, integration, or contract test.

---

## Skill Index

| Skill               | Path                               | Load When                                                                                                 |
| ------------------- | ---------------------------------- | --------------------------------------------------------------------------------------------------------- |
| `test-unit`         | `.agents/skills/test-unit`         | Writing unit tests for services, domain logic, and pure functions with JUnit 5 + Mockito                  |
| `test-slice-web`    | `.agents/skills/test-slice-web`    | Writing `@WebMvcTest` slice tests for controllers — MockMvc, security config, request/response assertions |
| `test-slice-data`   | `.agents/skills/test-slice-data`   | Writing `@DataJpaTest` slice tests for repositories with Testcontainers PostgreSQL                        |
| `test-integration`  | `.agents/skills/test-integration`  | Writing full `@SpringBootTest` integration tests with Testcontainers for the complete stack               |
| `test-security`     | `.agents/skills/test-security`     | Testing Spring Security: `@WithMockUser`, JWT token setup, method security assertions                     |
| `test-architecture` | `.agents/skills/test-architecture` | ArchUnit rules — package dependencies, layer boundaries, naming conventions                               |

---

## Decision Guide

### "I'm testing a service or domain class in isolation"

→ Load **`test-unit`**: plain JUnit 5, no Spring context, Mockito for dependencies.

### "I'm testing a controller"

→ Load **`test-slice-web`**: `@WebMvcTest`, MockMvc, test only the web layer.

### "I'm testing a repository or query"

→ Load **`test-slice-data`**: `@DataJpaTest` + Testcontainers PostgreSQL, real DB.

### "I'm testing a full request-to-database flow"

→ Load **`test-integration`**: `@SpringBootTest` + Testcontainers, all layers active.

### "I'm testing access control or JWT authentication"

→ Load **`test-security`**

### "I'm enforcing architectural rules (no layer skipping, naming, etc.)"

→ Load **`test-architecture`**: ArchUnit rules as tests.

---

## Quick Rules (always apply, no skill load needed)

- Test class name: `<Subject>Test` for unit, `<Subject>IT` for integration
- One assertion concept per test method — name the method to describe the scenario
- No `@SpringBootTest` for simple service logic — use plain unit tests
- Use `@DisplayName` for readability in CI output
- Testcontainers images pinned to explicit versions, never `:latest`
- No `Thread.sleep()` in tests — use `Awaitility` for async assertions
- Test data built via builders or factory methods — no magic strings scattered across tests

---

## Test Slice Cheat Sheet

| Annotation        | Loads          | Use For                               |
| ----------------- | -------------- | ------------------------------------- |
| _(none)_          | Nothing        | Pure unit tests — fastest             |
| `@WebMvcTest`     | Web layer only | Controller + filter + security tests  |
| `@DataJpaTest`    | JPA + DB only  | Repository, query, entity tests       |
| `@JsonTest`       | Jackson only   | Serialization / deserialization tests |
| `@SpringBootTest` | Full context   | End-to-end integration tests          |
