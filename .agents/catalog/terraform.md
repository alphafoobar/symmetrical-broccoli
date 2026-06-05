# OpenTofu Skills Catalog

Skills for infrastructure-as-code provisioning and configuration. Load before writing OpenTofu resources, modules, providers, or environment configuration for this project.

---

## Skill Index

| Skill                              | Path                                              | Load When                                                                                    |
| ---------------------------------- | ------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| `tf-aws-networking`                | `.agents/skills/tf-aws-networking`                | Creating AWS VPCs, subnets, route tables, NAT, security groups, or load balancers            |
| `tf-aws-rds-postgres`              | `.agents/skills/tf-aws-rds-postgres`              | Provisioning AWS RDS PostgreSQL, subnet groups, parameter groups, backups, or DB security    |
| `tf-aws-ecs-spring-app`            | `.agents/skills/tf-aws-ecs-spring-app`            | Deploying the Spring Boot API on AWS ECS Fargate behind an ALB                               |
| `tf-aws-secrets-observability`     | `.agents/skills/tf-aws-secrets-observability`     | Creating AWS Secrets Manager, IAM, CloudWatch logs, alarms, dashboards, or task permissions  |
| `tf-aws-cloudwatch-metrics`         | `.agents/skills/tf-aws-cloudwatch-metrics`         | Creating CloudWatch metric dashboards, application metric widgets, alarms, namespaces, or metric publishing IAM |

---

## Decision Guide

### "I'm provisioning a database"

→ Load **`tf-aws-rds-postgres`**

### "I'm deploying the application container"

→ Load **`tf-aws-ecs-spring-app`**

### "I'm managing API keys, DB passwords, or JWT secrets"

→ Load **`tf-aws-secrets-observability`**

### "I'm building CloudWatch dashboards or metric alarms"

→ Load **`tf-aws-cloudwatch-metrics`**. Also load the service skills whose metrics are shown, such as **`tf-aws-ecs-spring-app`** or **`tf-aws-rds-postgres`**.

### "I'm creating VPCs, subnets, load balancers, or security groups"

→ Load **`tf-aws-networking`**

---

## Quick Rules (always apply, no skill load needed)

- Use OpenTofu (`tofu`) for fmt, validate, init, plan, state inspection, and apply/destroy workflows
- Configure AWS provider `default_tags` so `project`, `environment`, and `managed-by = "opentofu"` apply automatically across the stack
- Remote state only — no local `terraform.tfstate` committed to git
- One workspace per environment (`dev`, `staging`, `prod`)
- Variables for anything environment-specific; no hardcoded region, account ID, or credentials
- `tofu fmt` and `tofu validate` must pass before commit
- Sensitive outputs marked `sensitive = true`
- Pin provider versions in `required_providers`

---

## Conventions

```hcl
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      project     = var.project
      environment = var.environment
      managed-by  = "opentofu"
    }
  }
}
```

```
infra/
├── main.tf
├── variables.tf
├── outputs.tf
├── versions.tf          ← required_providers + terraform block
├── terraform.tfvars     ← gitignored; values per environment
└── modules/
    └── <module-name>/
        ├── main.tf
        ├── variables.tf
        └── outputs.tf
```
