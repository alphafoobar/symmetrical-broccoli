---
name: spring-exceptions-problemdetail
description: "Spring MVC exception handling with RFC 9457 ProblemDetail. Use when creating exceptions, @RestControllerAdvice, validation errors, business errors, not-found errors, or API error response tests."
---

# Spring ProblemDetail Exceptions

## Defaults

- All API errors return `ProblemDetail`.
- Do not create custom error envelope DTOs.
- Use stable `type` URIs for client-actionable error classes.
- Include safe operational context in `properties`; never include secrets, stack traces, SQL, or raw downstream payloads.
- Validation failures are `400 Bad Request`; business rule failures are `422 Unprocessable Entity`.
- Load `java-logging` with this skill so request-processing exceptions are logged once with structured key/value fields and stack traces.

## Exception Model

- Domain exceptions should be specific and carry typed context needed to build a problem response.
- Use unchecked exceptions for business/domain failures.
- Keep HTTP mapping out of domain classes; map exceptions in `@RestControllerAdvice`.

```java
public class OrderNotFoundException extends RuntimeException {

  private final UUID orderId;

  public OrderNotFoundException(final UUID orderId) {
    super("Order not found: %s".formatted(orderId));
    this.orderId = orderId;
  }

  public UUID orderId() {
    return orderId;
  }
}
```

## Handler Pattern

```java
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(OrderNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  ProblemDetail handleOrderNotFound(final OrderNotFoundException ex, final HttpServletRequest request) {
    log.atWarn()
        .setCause(ex)
        .addKeyValue("path", request.getRequestURI())
        .addKeyValue("orderId", ex.orderId())
        .log("Order not found");

    val problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Order Not Found");
    problem.setType(URI.create("https://errors.demo.com/order-not-found"));
    problem.setProperty("orderId", ex.orderId());
    return problem;
  }
}
```

## Validation Errors

- Convert `MethodArgumentNotValidException` and `ConstraintViolationException` into a `ProblemDetail`.
- Put field errors in a `violations` property as a list of maps or records.
- Keep violation messages client-safe and deterministic.

## OpenAPI and Tests

- Load `java-openapi` when documenting error responses.
- Every controller test should assert the HTTP status, `type`, `title`, and key properties for error cases.
- Error responses should use `application/problem+json` when explicitly asserted.
