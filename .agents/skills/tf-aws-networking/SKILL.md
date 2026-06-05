---
name: tf-aws-networking
description: "OpenTofu AWS networking conventions for Spring Boot APIs. Use when creating VPCs, subnets, route tables, NAT, security groups, load balancers, AWS provider default_tags, or network module contracts."
---

# AWS Networking with OpenTofu

## Defaults

- Use OpenTofu commands (`tofu fmt`, `tofu validate`, `tofu plan`).
- Configure AWS provider `default_tags` at the stack/root module so baseline tags apply automatically.
- Use one VPC per environment unless platform networking is already provided.
- Place application tasks in private subnets.
- Public subnets are for load balancers and NAT only.
- Security groups are least privilege and named by purpose.

## Module Shape

```hcl
module "network" {
  source = "./modules/network"

  project     = var.project
  environment = var.environment
  cidr_block  = var.cidr_block
}
```

Required outputs:

- `vpc_id`
- `private_subnet_ids`
- `public_subnet_ids`
- `app_security_group_id`
- `load_balancer_security_group_id`

## Rules

- Do not repeat baseline tags on every resource; rely on AWS provider `default_tags`.
- Add resource-specific tags only when they add useful context beyond the provider defaults.
- Do not allow `0.0.0.0/0` ingress directly to application tasks.
- Restrict database ingress to the application security group.
- Enable load balancer access logs when the account has a logging bucket pattern.
- Keep CIDR ranges environment-specific through variables.
