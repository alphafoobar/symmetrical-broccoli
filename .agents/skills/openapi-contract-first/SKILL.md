---
name: openapi-contract-first
description: "OpenAPI contract-first workflow with OpenAPI Generator Gradle plugin. Use when designing API resources before implementation, configuring code generation, validating specs, reviewing generated REST interfaces, or migrating away from SpringDoc-generated contracts."
---

# OpenAPI Contract-First Workflow

## Workflow

1. Edit `openapi/openapi.yaml`.
2. Validate the spec with `openApiValidate`.
3. Generate Spring interfaces with `openApiGenerate`.
4. Implement generated interfaces in controllers.
5. Add controller/service/tests against the generated contract.

The spec is the public contract. Java annotations are implementation detail.

## Generator Defaults

- Generator: `spring`
- Mode: `interfaceOnly`
- Packages:
  - API interfaces: `com.demo.skills.api`
  - Models: `com.demo.skills.api.model`
- Spring generation: `useSpringBoot3 = true`
- Documentation annotations: disabled with `annotationLibrary = none` and `documentationProvider = none`
- Nullable wrapper: disabled with `openApiNullable = false`

## Contract Rules

- Every path has complete success and error responses.
- Every operation has a unique `operationId`.
- All schemas are reusable components unless they are genuinely one-off primitives.
- Use `application/problem+json` for error responses.
- Include security at the top level and override per operation only for public endpoints.
- Do not hand-edit generated code.

## Implementation Rules

- Controllers implement generated interfaces.
- Services own business logic.
- Exception handlers map domain failures to the spec's Problem Details responses.
- Regenerate after every spec change before writing controller code.

## Validation

- `check` depends on `openApiValidate`.
- `compileJava` depends on `openApiGenerate`.
- Generated source may be excluded from Checkstyle, but hand-written controllers and models must pass Google Java Checkstyle.
