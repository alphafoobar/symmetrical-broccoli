---
name: spring-resilience4j
description: "Resilience4j circuit breaker, retry, timeout, bulkhead, fallback, and downstream integration conventions for Spring Boot REST APIs. Use when calling external HTTP services, databases outside repositories, queues, or third-party APIs."
---

# Spring Resilience4j

## Defaults

- Protect every network call to a downstream service with a timeout and circuit breaker.
- Use retries only for idempotent operations or operations with idempotency keys.
- Prefer small, explicit named resilience instances per downstream capability.
- Fallbacks must preserve correctness; never hide failed writes as successful operations.

## Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentProvider:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 20
        minimumNumberOfCalls: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
  timelimiter:
    instances:
      paymentProvider:
        timeoutDuration: 2s
  retry:
    instances:
      paymentProvider:
        maxAttempts: 2
        waitDuration: 200ms
```

## Usage Rules

- Keep resilience annotations on adapter/client classes, not controllers.
- Name instances after the downstream capability: `paymentProvider`, `customerProfileApi`.
- Do not retry validation errors, authentication failures, authorization failures, or 4xx business responses.
- Map exhausted retries and open circuits to domain-specific integration exceptions.
- Surface integration failures through `ProblemDetail` using `spring-exceptions-problemdetail`.

## Fallbacks

- Use fallbacks for reads only when stale/default data is explicitly acceptable.
- Fallback methods must have the same parameters plus a trailing `Throwable`.
- Log fallback activation with downstream name and exception class.

## Testing

- Unit-test fallback and exception mapping without sleeping.
- Use Awaitility for state transitions when testing circuit behavior.
- Assert that non-idempotent operations are not retried unless an idempotency key is present.
