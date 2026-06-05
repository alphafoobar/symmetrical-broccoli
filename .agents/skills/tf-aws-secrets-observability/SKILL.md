---
name: tf-aws-secrets-observability
description: "OpenTofu AWS secrets, IAM, logs, metrics, alarms, and dashboards for Spring Boot API deployments. Use when provisioning Secrets Manager, SSM parameters, CloudWatch logs, alarms, or observability IAM permissions."
---

# AWS Secrets and Observability

## Secrets

- Use AWS Secrets Manager for database passwords, JWT issuer/client secrets, API keys, and credentials.
- Do not put secret values in OpenTofu state unless there is no alternative.
- Prefer creating secret containers in OpenTofu and populating values through a separate secret-management workflow.
- Grant application task roles read access only to the exact secret ARNs required.

## Logs

- Every ECS task has a CloudWatch log group with explicit retention.
- Rely on AWS provider `default_tags` for baseline resource tags.
- Log group names include project, environment, and service.
- Production retention is at least 30 days unless compliance requires more.

## Metrics and Alarms

- Alarm on ALB 5xx rate, target 5xx rate, unhealthy host count, ECS CPU/memory, task restarts, and RDS storage/CPU/connections.
- Use environment-specific thresholds.
- Route alarms to SNS or the platform alerting module.
- Load `tf-aws-cloudwatch-metrics` when defining detailed application metric namespaces, dashboard widgets, metric math, or custom metric alarms.

## Dashboards

- Create focused dashboards for API health, latency, error rate, saturation, and database health.
- Prefer metrics emitted by ALB, ECS, RDS, and Micrometer/CloudWatch before adding custom infrastructure metrics.
- Keep dashboard metric names and dimensions aligned with the Java `java-metrics` skill.

## IAM

- Separate execution role from task role.
- Execution role pulls images and reads container startup secrets.
- Task role grants application runtime permissions only.
