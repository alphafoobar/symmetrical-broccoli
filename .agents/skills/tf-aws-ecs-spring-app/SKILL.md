---
name: tf-aws-ecs-spring-app
description: "OpenTofu AWS ECS Fargate deployment conventions for Spring Boot REST APIs. Use when provisioning task definitions, services, load balancers, IAM roles, autoscaling, logging, and environment variables."
---

# AWS ECS Spring Boot App

## Defaults

- Deploy Spring Boot APIs as ECS Fargate services behind an Application Load Balancer.
- Rely on AWS provider `default_tags` for baseline resource tags.
- Run tasks in private subnets.
- Use CloudWatch Logs with retention set explicitly.
- Store secrets in AWS Secrets Manager or SSM Parameter Store and inject them via task definition secrets.
- Use IAM task roles with least privilege for application AWS API access.

## Task Definition

- Container port defaults to `8080`.
- Configure health checks against `/actuator/health/readiness`.
- Set CPU and memory per environment through variables.
- Pass Spring profile and operational config through environment variables.
- Do not bake secrets into images or OpenTofu variables.

## Service and Load Balancer

- ALB listener terminates TLS.
- Target group health check path is `/actuator/health/readiness`.
- Desired count is at least `2` for production.
- Enable deployment circuit breaker with rollback.
- Configure autoscaling on CPU, memory, or request count.

## Outputs

- `service_name`
- `cluster_name`
- `load_balancer_dns_name`
- `task_role_arn`
- `execution_role_arn`
