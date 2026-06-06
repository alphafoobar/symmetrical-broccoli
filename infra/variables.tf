variable "project" {
  description = "Project tag value applied to AWS resources"
  type        = string
  default     = "skills"
}

variable "aws_region" {
  description = "AWS region to deploy to"
  type        = string
  default     = "ap-southeast-2"
}

variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "image_uri" {
  description = "Docker image URI for the Spring Boot application (ECR URI with tag)"
  type        = string
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
