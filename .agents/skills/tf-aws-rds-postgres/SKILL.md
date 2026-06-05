---
name: tf-aws-rds-postgres
description: "OpenTofu AWS RDS PostgreSQL conventions. Use when provisioning PostgreSQL instances, subnet groups, parameter groups, backups, encryption, monitoring, and database connectivity for Spring Boot APIs."
---

# AWS RDS PostgreSQL

## Defaults

- Use RDS PostgreSQL with storage encryption enabled.
- Rely on AWS provider `default_tags` for baseline resource tags.
- Put database instances in private subnets only.
- Manage credentials with AWS Secrets Manager, not plaintext variables.
- Enable automated backups and deletion protection outside ephemeral environments.
- Pin engine major versions and plan upgrades deliberately.

## Required Settings

- `multi_az = true` for production.
- `backup_retention_period >= 7` for shared environments and production.
- `storage_encrypted = true`.
- `publicly_accessible = false`.
- Security group ingress only from the application security group.

## Spring Integration

- Export database host, port, database name, and secret ARN as outputs.
- Inject connection values into the application as environment variables or platform secrets.
- Use Flyway from the application deployment pipeline unless a separate migration job exists.

## Safety

- Never commit `terraform.tfvars` with credentials.
- Mark password and connection-string outputs `sensitive = true`.
- Use final snapshots for non-ephemeral database deletion.
- Review parameter changes for reboot requirements.
