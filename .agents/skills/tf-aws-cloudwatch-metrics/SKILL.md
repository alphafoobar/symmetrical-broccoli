---
name: tf-aws-cloudwatch-metrics
description: "OpenTofu conventions for CloudWatch metrics, alarms, dashboards, metric streams, and IAM needed to visualize Spring Boot Micrometer application metrics on AWS. Use when creating CloudWatch dashboards, application metric widgets, alarm rules, metric namespaces, ECS/RDS/ALB metric views, or task permissions for metric publishing."
---

# AWS CloudWatch Metrics

## Defaults

- Use OpenTofu (`tofu`) for fmt, validate, plan, and apply/destroy workflows.
- Rely on AWS provider `default_tags` for baseline resource tags.
- For toy/demo APIs, prefer AWS-native observability: ECS/Fargate, CloudWatch Logs, Micrometer CloudWatch metrics, CloudWatch dashboards, and optional CloudWatch alarms.
- Build dashboards from signals operators will actually use: traffic, errors, latency, saturation, and business outcomes.
- Use AWS service metrics from ALB, ECS, and RDS before adding custom application metrics.
- Use Micrometer application metrics for domain-specific counters, timers, and summaries.
- Keep environment-specific names, namespaces, thresholds, and ARNs as variables.
- Do not provision Prometheus or Grafana unless the demo specifically needs that stack.

## Metric Namespaces and Dimensions

- Use a predictable namespace for custom application metrics, such as `${var.project}/${var.environment}/application`.
- Keep dimensions low-cardinality and aligned with Java metric tags: `application`, `environment`, `service`, `result`, `provider`, `operation`.
- Do not create dashboard widgets or alarms for dimensions that can contain user IDs, account IDs, UUIDs, request IDs, emails, or raw paths.
- Document expected custom metric names and dimensions in module variables so Java and Terraform stay aligned.

## Dashboards

- Create one focused CloudWatch dashboard per service/environment unless the platform module already aggregates them.
- Include, at minimum:
  - ALB request count, target response time, HTTP 4xx, and HTTP 5xx.
  - ECS CPU, memory, running task count, and deployment health.
  - RDS CPU, connections, storage, read/write latency, and deadlocks when a database exists.
  - Application latency, error/result counters, and key business event counters from Micrometer.
- Keep widgets grouped by operator workflow: health first, then latency/errors, then saturation, then business metrics.
- Prefer metric math for rates and percentages instead of showing raw error counts alone.
- Use explicit periods and statistic choices per metric. Do not let dashboards inherit ambiguous defaults.

## ECS Container Insights

- Enable Container Insights on demo ECS clusters when the extra CloudWatch cost is acceptable.
- Use it for ECS-level CPU, memory, network, task count, and service health signals instead of recreating those metrics in application code.

```hcl
resource "aws_ecs_cluster" "main" {
  name = var.cluster_name

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}
```

## Alarms

- Alarm on symptoms before internals: user-visible 5xx rate, high latency, unhealthy targets, and failed business operations.
- Add saturation alarms for ECS CPU/memory and RDS resource pressure.
- Use environment-specific thresholds and evaluation periods.
- Route alarm actions through SNS or the platform alerting module.
- Avoid alarms on sparse custom metrics unless missing data behavior is intentional and documented.

## IAM for Publishing Metrics

- If the application publishes directly to CloudWatch, grant the ECS task role only the CloudWatch permissions required for metric publishing.
- Prefer the narrowest practical policy. Avoid broad write permissions unrelated to metrics.
- Keep execution role and task role separate.

```hcl
data "aws_iam_policy_document" "application_metrics" {
  statement {
    actions = [
      "cloudwatch:PutMetricData",
    ]

    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "cloudwatch:namespace"
      values   = [var.application_metrics_namespace]
    }
  }
}
```

## Dashboard Widget Pattern

- Use `jsonencode` for `aws_cloudwatch_dashboard.dashboard_body`.
- Keep repeated metric definitions in locals when it improves readability.
- Include `region`, `title`, `period`, `stat`, and clear labels.

```hcl
resource "aws_cloudwatch_dashboard" "service" {
  dashboard_name = "${var.project}-${var.environment}-${var.service_name}"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          region = var.aws_region
          title  = "Application latency"
          view   = "timeSeries"
          period = 60
          stat   = "p95"
          metrics = [
            [
              var.application_metrics_namespace,
              "payments.authorization.duration",
              "application",
              var.service_name,
              "environment",
              var.environment,
            ],
          ]
        }
      },
    ]
  })
}
```

## Validation

- Run `tofu fmt` after editing OpenTofu files.
- Run `tofu validate` when provider/backend setup is available.
- Review generated dashboard JSON shape before applying when widgets are complex.
- Confirm Java metric names, tags, and final CloudWatch dimensions match the dashboard and alarm definitions.

## Load With

- Load `terraform-opentofu` for general OpenTofu editing and validation.
- Load `tf-aws-ecs-spring-app` when dashboards or alarms depend on ECS service/task outputs.
- Load `tf-aws-rds-postgres` when adding database widgets or alarms.
- Load `tf-aws-secrets-observability` when changing log groups, alert destinations, or broader observability IAM.
