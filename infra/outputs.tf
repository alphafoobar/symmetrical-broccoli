output "load_balancer_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "api_url" {
  description = "URL of the Application Load Balancer"
  value       = "${local.api_url_scheme}://${aws_lb.main.dns_name}"
}

output "frontend_url" {
  description = "CloudFront distribution URL for the frontend"
  value       = "https://${aws_cloudfront_distribution.frontend.domain_name}"
}

output "rds_endpoint" {
  description = "Aurora Serverless cluster writer endpoint"
  value       = aws_rds_cluster.main.endpoint
  sensitive   = true
}

output "db_secret_arn" {
  description = "ARN of the Secrets Manager secret containing database credentials"
  value       = aws_rds_cluster.main.master_user_secret[0].secret_arn
  sensitive   = true
}

output "redis_host" {
  description = "Primary endpoint for the Valkey replication group"
  value       = aws_elasticache_replication_group.main.primary_endpoint_address
}

output "s3_bucket_name" {
  description = "Frontend S3 bucket name"
  value       = aws_s3_bucket.frontend.bucket
}

output "cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.app.name
}

output "auth_service_name" {
  description = "Mock OAuth2 ECS service name"
  value       = aws_ecs_service.auth.name
}

output "mock_issuer_uri" {
  description = "Mock OAuth2 issuer URI used by the API"
  value       = local.mock_issuer_uri
}

output "task_role_arn" {
  description = "ECS task role ARN"
  value       = aws_iam_role.task.arn
}

output "execution_role_arn" {
  description = "ECS execution role ARN"
  value       = aws_iam_role.execution.arn
}

output "ecr_repository_url" {
  description = "ECR repository URL for the API image"
  value       = aws_ecr_repository.api.repository_url
}

output "image_uri" {
  description = "API image URI referenced by the ECS task definition"
  value       = local.image_uri
}
