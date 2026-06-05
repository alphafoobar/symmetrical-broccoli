---
name: spring-observability
description: "Micrometer, tracing, CloudWatch, structured logging, actuator, and production observability conventions for Spring Boot REST APIs. Use when adding metrics, spans, logs, health checks, CloudWatch export, or monitoring configuration."
---

# Spring Observability

## Defaults

- For toy/demo APIs on AWS, prefer ECS Fargate, Spring Boot Actuator, Micrometer CloudWatch registry, CloudWatch Metrics, OpenTofu-managed CloudWatch dashboards, and optional CloudWatch alarms.
- Use Spring Boot Actuator, Micrometer, CloudWatch-compatible metric export, and tracing bridge declared by the project.
- Keep observability code low-cardinality. Never put raw user IDs, emails, tokens, UUID-heavy IDs, or free-form messages into metric tags.
- Use logs for high-cardinality detail, metrics for aggregate signals, and traces for request flow.
- Keep logs separate from metrics: SLF4J/Logback writes to stdout and ECS sends stdout to CloudWatch Logs.
- Do not require JSON logging just to publish metrics.
- Avoid Prometheus/Grafana unless the demo specifically needs PromQL, Grafana, cloud-portable dashboards, Kubernetes-style monitoring patterns, or observability-platform examples.

## Dependencies

- Include `org.springframework.boot:spring-boot-starter-actuator`.
- Include `io.micrometer:micrometer-registry-cloudwatch2` for CloudWatch metric publishing.
- Remove `io.micrometer:micrometer-registry-prometheus` unless Prometheus is explicitly required.

## Actuator Exposure

- Expose only required endpoints per environment.
- Public unauthenticated access is limited to health/readiness/liveness.
- Diagnostic actuator metric endpoints must be protected in production.
- Do not expose `/actuator/prometheus` unless a deployment explicitly adds a protected Prometheus scrape path.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      probes:
        enabled: true
```

## Metrics

- Prefer framework-provided HTTP, datasource, JVM, and resilience metrics before custom meters.
- Use custom counters/timers only for business events that operators will alert or dashboard.
- Meter names use dot notation: `orders.created`, `payments.authorization.duration`.
- Tags are bounded enums such as `status`, `result`, `provider`, or `exception`.
- Keep meter names and tags aligned with the CloudWatch namespace and dimensions consumed by OpenTofu dashboards and alarms.
- Load `java-metrics` before adding custom Micrometer meters, tags, actuator metric exposure, or metric tests.

## CloudWatch Export

- Prefer CloudWatch as the production metrics backend.
- Publish Micrometer application metrics directly to CloudWatch only when the ECS task role has narrow `cloudwatch:PutMetricData` permission for the configured namespace.
- Use `management.cloudwatch.metrics.export.namespace`; Spring Cloud AWS requires this property for CloudWatch metric publishing.
- Use a stable namespace that includes project and environment, such as `${METRICS_NAMESPACE:SkillsApi/Local/Application}`.
- Use the default 1 minute step unless there is a concrete operator need for a different publishing interval.
- Treat CloudWatch dimensions as the exported form of Micrometer tags; keep them bounded and predictable.
- Do not add the Prometheus registry, scrape endpoint, or dashboard assumptions unless the deployment explicitly uses Prometheus.

```yaml
management:
  cloudwatch:
    metrics:
      export:
        namespace: ${METRICS_NAMESPACE:SkillsApi/Local/Application}
```

## Tracing

- Let Spring MVC and HTTP clients create spans automatically where possible.
- Add manual spans only around meaningful internal work or external calls not covered by instrumentation.
- Propagate trace IDs through logs. Do not pass trace IDs manually in domain APIs.

## Logging

- Use `@Slf4j`; never use `System.out.println`.
- Load `java-logging` before adding or changing application log statements.
- Logs should include stable structured keys with SLF4J fluent `addKeyValue`: `orderId`, `provider`, `result`.
- Do not log request/response bodies by default.
- Warn on recoverable downstream failures; error on failed user/business operations only when action is needed.

## Health Indicators

- Add custom `HealthIndicator` only for dependencies not already covered by Spring Boot.
- Health checks must be fast, bounded, and side-effect free.
- Do not make health checks perform write operations.

## Dashboards

- Load `tf-aws-cloudwatch-metrics` when adding or changing CloudWatch dashboard widgets, alarms, metric namespaces, or metric publishing IAM.
