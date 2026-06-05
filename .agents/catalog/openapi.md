# OpenAPI Skills Catalog

Skills for contract-first REST API design and generated Spring interface development. Load before writing OpenAPI specs, controllers, request/response types, or error handlers.

---

## Skill Index

| Skill                              | Path                                              | Load When                                                                                |
| ---------------------------------- | ------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `openapi-contract-first`           | `.agents/skills/openapi-contract-first`           | Designing or changing `openapi/openapi.yaml`, generator configuration, or generated APIs |
| `java-openapi`                     | `.agents/skills/java-openapi`                     | Implementing controllers from generated interfaces or changing OpenAPI-backed schemas    |
| `java-logging`                     | `.agents/skills/java-logging`                     | Writing request exception logging in a `@RestControllerAdvice`                           |
| `spring-exceptions-problemdetail`  | `.agents/skills/spring-exceptions-problemdetail`  | Writing API exceptions, validation errors, or RFC 9457 `ProblemDetail` responses         |

---

## Decision Guide

### "I'm creating a new REST controller"

→ Load **`openapi-contract-first`** + **`java-openapi`**: define the operation in `openapi/openapi.yaml`, generate the interface, then implement it.

### "I'm creating request or response DTOs"

→ Load **`openapi-contract-first`** + **`java-openapi`**: define component schemas in `openapi/openapi.yaml`; generated models are the API boundary.

### "I'm writing an exception handler / error response"

→ Load **`openapi-contract-first`** + **`spring-exceptions-problemdetail`** + **`java-logging`**: declare error responses in the spec, implement them with `ProblemDetail`, and log request exceptions once with structured fields.

### "I'm adding path or query parameters"

→ Load **`openapi-contract-first`**: define parameters in the spec and regenerate interfaces.

---

## Quick Rules

- OpenAPI spec lives at `openapi/openapi.yaml`
- The spec is written before controller code
- Controllers implement generated interfaces
- Generated code is not edited by hand
- Error bodies are `ProblemDetail`
- Every operation has a stable `operationId`
- Every reachable status code is declared in the spec, including 401 and 403
- Security schemes are declared in `components.securitySchemes`
- Never version individual endpoints — version the whole resource
- Do not use SpringDoc-generated OpenAPI as the source of truth

---

## HTTP Status Code Conventions

| Scenario                 | Status                      |
| ------------------------ | --------------------------- |
| Resource created         | `201 Created`               |
| Successful read / update | `200 OK`                    |
| Async accepted           | `202 Accepted`              |
| Empty success (delete)   | `204 No Content`            |
| Validation failure       | `400 Bad Request`           |
| Unauthenticated          | `401 Unauthorized`          |
| Insufficient permission  | `403 Forbidden`             |
| Resource not found       | `404 Not Found`             |
| Business rule violation  | `422 Unprocessable Entity`  |
| Unexpected server error  | `500 Internal Server Error` |

---

## Generator Rules

- Use OpenAPI Generator Gradle plugin `org.openapi.generator`
- `compileJava` depends on `openApiGenerate`
- `check` depends on `openApiValidate`
- Generated code may be excluded from Checkstyle
- Hand-written controller implementations must pass Google Java Checkstyle
