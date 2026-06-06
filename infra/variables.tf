variable "project" {
  description = "Project tag value applied to AWS resources"
  type        = string
  default     = "skills"
}

variable "aws_region" {
  description = "AWS region to deploy to"
  type        = string
  default     = "ap-southeast-6"
}

variable "aws_profile" {
  description = "Local AWS shared config profile used by OpenTofu"
  type        = string
  default     = "projects"
}

variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
}

variable "updated_at" {
  description = "Optional timestamp string applied to AWS resources as the updatedAt tag. Pass a current timestamp at plan/apply time."
  type        = string
  default     = null
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "image_tag" {
  description = "ECR image tag for the Spring Boot application. Defaults to the environment name."
  type        = string
  default     = null
}

variable "ecr_repository_name" {
  description = "ECR repository name for the Spring Boot application image"
  type        = string
  default     = "skills-api"
}

variable "frontend_bucket_name" {
  description = "Optional globally unique S3 bucket name for static frontend assets"
  type        = string
  default     = null
}

variable "db_name" {
  description = "Aurora PostgreSQL database name"
  type        = string
  default     = "skills"
}

variable "redis_port" {
  description = "Valkey port exposed to the application"
  type        = number
  default     = 6379
}

variable "task_cpu" {
  description = "ECS task CPU units"
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "ECS task memory in MiB"
  type        = number
  default     = 1024
}

variable "desired_count" {
  description = "Desired ECS service task count"
  type        = number
  default     = 2
}

variable "api_certificate_arn" {
  description = "Optional DNS-validated ACM certificate ARN for HTTPS on the API load balancer. Required when CloudFront connects to the ALB over HTTPS; when omitted, the ALB serves HTTP."
  type        = string
  default     = null
}

variable "mock_oauth2_image" {
  description = "Container image for the example mock OAuth2 issuer"
  type        = string
  default     = "ghcr.io/navikt/mock-oauth2-server:2.2.1"
}
