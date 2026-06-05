---
name: java-logging
description: "Structured logging and exception logging conventions for Spring Boot Java. Use when adding log statements, handling exceptions inline, writing request/global exception handlers, or reviewing exception flow. Prefers SLF4J key/value logging, mostly info-level business logging, and ProblemDetail-producing global exception handlers."
---

# Java Logging Conventions

## Logger Setup

Classes that log use Lombok `@Slf4j`. Do not create loggers manually.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    public OrderResponse create(final CreateOrderRequest request) {
        ...
    }
}
```

## Structured Key/Value Fields

Prefer SLF4J fluent logging with `addKeyValue` for every variable passed to a log statement.

```java
log.atInfo()
    .addKeyValue("orderId", order.id())
    .addKeyValue("customerId", order.customerId())
    .addKeyValue("status", order.status())
    .log("Order created");
```

Do not hide variables inside free-form message text.

```java
// WRONG - values are embedded in the message instead of structured fields
log.info("Order {} created for customer {}", order.id(), order.customerId());
```

Use stable, lower camel case key names that match domain language: `orderId`, `customerId`, `paymentProvider`, `attempt`, `durationMillis`.

## Log Levels

Use `info` for most application events that are useful for operating the system:

- Request-level business milestones
- State transitions
- External calls starting or completing
- Background job start/finish
- Important decisions that explain user-visible outcomes

Use `debug` only for diagnostic details that are too noisy for normal production logs.

Use `warn` when the application handled an exceptional or degraded condition and continued with a fallback, retry, skipped item, or alternate path.

Use `error` for unexpected failures at a boundary that are converted into a server error, usually in the global exception handler.

## Exception Flow

Do not catch an exception only to log it and rethrow it. Let it pass through to the caller or global exception handler.

```java
// WRONG - duplicate logging and no handling
try {
    paymentClient.charge(request);
} catch (PaymentClientException ex) {
    log.warn("Payment failed", ex);
    throw ex;
}
```

Each exception path must do exactly one of these:

- Handle it locally and log it if the exception explains an operationally useful event.
- Convert it to a domain exception without logging and let the domain exception pass through.
- Let it pass through unchanged.

```java
try {
    return paymentClient.charge(request);
} catch (PaymentTimeoutException ex) {
    log.atWarn()
        .setCause(ex)
        .addKeyValue("orderId", request.orderId())
        .addKeyValue("provider", request.provider())
        .log("Payment provider timeout handled with retry scheduling");

    retryScheduler.schedule(request);
    return PaymentResult.retryScheduled(request.orderId());
}
```

If the local code cannot truly handle the exception, do not catch it.

## Inline Exception Logging

When an exception must be handled inline, log it at `warn` or higher and include the exception as the cause so the stack trace is preserved.

```java
log.atWarn()
    .setCause(ex)
    .addKeyValue("fileName", file.getName())
    .addKeyValue("importId", importId)
    .log("Import row skipped after parse failure");
```

Do not log only `ex.getMessage()`. That loses the stack trace and exception type.

```java
// WRONG
log.warn("Import failed error={}", ex.getMessage());
```

## Global Request Exception Handler

Every Spring MVC API has a `@RestControllerAdvice` global exception handler that converts request-processing exceptions into `ProblemDetail` responses.

The handler should:

- Return `ProblemDetail`, never a custom error envelope.
- Log most exceptions with the stack trace using `setCause(ex)`.
- Include request context as structured key/value fields when available.
- Use `warn` for expected client/domain failures such as not found, validation, conflict, or business rule violations.
- Use `error` for unexpected exceptions that return `500`.
- Avoid duplicate logs from service/controller catch-and-rethrow blocks.

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleOrderNotFound(
        final OrderNotFoundException ex,
        final HttpServletRequest request
    ) {
        log.atWarn()
            .setCause(ex)
            .addKeyValue("path", request.getRequestURI())
            .addKeyValue("orderId", ex.orderId())
            .log("Order not found");

        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Order Not Found");
        problem.setType(URI.create("https://errors.demo.com/order-not-found"));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleUnexpected(final Exception ex, final HttpServletRequest request) {
        log.atError()
            .setCause(ex)
            .addKeyValue("path", request.getRequestURI())
            .addKeyValue("method", request.getMethod())
            .log("Unexpected request failure");

        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected server error"
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://errors.demo.com/internal-server-error"));
        return problem;
    }
}
```

## Rules Summary

| Rule | Detail |
| ---- | ------ |
| Logger | Use `@Slf4j`, never `LoggerFactory` |
| Values | Variables go in `addKeyValue`, not message templates |
| Default level | Prefer `info` for normal application events |
| Inline handled exceptions | Log `warn` or higher with `setCause(ex)` |
| Catch/rethrow | Never catch only to log and rethrow |
| Request failures | Global `@RestControllerAdvice` logs and returns `ProblemDetail` |
| Stack traces | Preserve exception stack traces with `setCause(ex)` |
