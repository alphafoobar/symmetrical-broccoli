---
name: java-metrics
description: "Application metrics conventions for Spring Boot Java using Micrometer, Actuator, and CloudWatch-friendly meter design. Use when adding custom counters, timers, gauges, distribution summaries, meter filters, metric tags, actuator metric exposure, or metric tests in Java services."
---

# Java Metrics

## Defaults

- Use Micrometer APIs already provided by Spring Boot Actuator.
- Prefer built-in HTTP, JVM, datasource, cache, executor, and resilience metrics before adding custom meters.
- Add custom metrics only for business or operational signals that will be alerted on, dashboarded, or used for capacity decisions.
- Keep logs for high-cardinality event detail; keep metrics aggregated and bounded.

## Meter Design

- Name meters with stable dot notation: `orders.created`, `payments.authorization.duration`, `imports.rows.processed`.
- Use one clear unit per meter and include the base unit where Micrometer supports it.
- Use counters for events that only increase.
- Use timers for latency or duration.
- Use distribution summaries for observed amounts such as payload bytes, batch sizes, or row counts.
- Use gauges only for values that are naturally sampled from existing state; do not create mutable gauge state just to report events.

## Tags

- Tags must be low-cardinality bounded values such as `status`, `result`, `provider`, `operation`, `method`, or `exception`.
- Use enum names or a small fixed vocabulary for tag values.
- Never tag with user IDs, account IDs, order IDs, request IDs, email addresses, raw exception messages, URLs with IDs, UUIDs, timestamps, or free-form text.
- Keep tag sets consistent for a meter name. Do not register the same meter with different tag keys in different code paths.
- Prefer `result=success|failure|skipped` over separate success and failure meter names.

## Implementation

- Prefer constructor-injected `MeterRegistry` for imperative service instrumentation.
- Prefer `Timer.Sample` only when the duration spans multiple calls or includes conditional result tags.
- Register reusable meters once in constructors or configuration when the tag set is fixed.
- For dynamic but bounded tags, use registry builders at the recording site and make the bounded vocabulary obvious in code.
- Do not put metric recording in controllers unless the metric is specifically about request handling not already covered by Spring MVC instrumentation.
- Keep business behavior independent from metrics. Metric recording failures must not change application results.

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MeterRegistry meterRegistry;
    private final PaymentClient paymentClient;

    public PaymentResult authorize(final PaymentRequest request) {
        val sample = Timer.start(meterRegistry);

        try {
            val result = paymentClient.authorize(request);
            recordAuthorizationDuration(sample, result.provider(), "success");
            return result;
        } catch (PaymentDeclinedException ex) {
            recordAuthorizationDuration(sample, request.provider(), "declined");
            throw ex;
        }
    }

    private void recordAuthorizationDuration(
        final Timer.Sample sample,
        final PaymentProvider provider,
        final String result
    ) {
        sample.stop(Timer.builder("payments.authorization.duration")
            .description("Payment authorization duration")
            .tag("provider", provider.name())
            .tag("result", result)
            .register(meterRegistry));
    }
}
```

## Actuator and Export

- Expose `metrics` only when operators need protected diagnostic actuator access.
- Do not expose `prometheus` unless the deployment explicitly adds a protected Prometheus scrape path.
- Production actuator metric endpoints must be authenticated or reachable only by trusted infrastructure.
- Configure common tags such as `application` and `environment` centrally.
- Do not rely on per-request code to add deployment tags.
- For CloudWatch publishing, configure `management.cloudwatch.metrics.export.namespace`; do not use older `management.metrics.export.cloudwatch.*` property names.

```yaml
management:
  cloudwatch:
    metrics:
      export:
        namespace: ${METRICS_NAMESPACE:SkillsApi/Local/Application}
  metrics:
    tags:
      application: skills-api
      environment: ${APP_ENVIRONMENT}
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

## CloudWatch Compatibility

- Design meter names and tags so the Terraform CloudWatch dashboard can reference them predictably.
- Keep dimensions small. CloudWatch dashboards and alarms become noisy and expensive with high-cardinality dimensions.
- When exporting Micrometer metrics to CloudWatch directly, use a namespace containing project and environment.
- Publish directly only when the runtime role has narrow `cloudwatch:PutMetricData` permission for that namespace.
- Document the final namespace and dimensions in Terraform variables or module inputs.
- For demo APIs, prefer a small allowlist of application metrics over exporting every JVM, HTTP, and datasource meter if cost or dashboard noise matters.

## Tests

- Unit-test custom metric recording with `SimpleMeterRegistry` when business logic depends on a specific metric being emitted.
- Assert meter name, tags, count, and duration/count values; do not assert exact wall-clock timing unless a fake clock is used.
- Add slice or integration coverage only when actuator exposure, security, or export configuration changes.

## Load With

- Load `java-modern`, `java-nullsafety`, and `java-imports` before writing Java.
- Load `java-lombok` when adding Spring beans or constructor injection.
- Load `java-logging` when adding logs around the same operational event.
- Load `spring-observability` when changing tracing, health checks, actuator configuration, or broader observability behavior.
