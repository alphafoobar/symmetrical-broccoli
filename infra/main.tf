terraform {
  required_version = ">= 1.8"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile

  default_tags {
    tags = merge(
      {
        project     = var.project
        environment = var.environment
        managed-by  = "opentofu"
        gitRepo     = "https://github.com/alphafoobar/symmetrical-broccoli"
      },
      var.updated_at != null ? { updatedAt = var.updated_at } : {}
    )
  }
}

data "aws_caller_identity" "current" {}

locals {
  api_listener_is_https = var.api_certificate_arn != null
  api_url_scheme        = local.api_listener_is_https ? "https" : "http"
  image_tag             = var.image_tag != null ? var.image_tag : var.environment
  image_uri             = "${aws_ecr_repository.api.repository_url}:${local.image_tag}"
  frontend_bucket_name  = var.frontend_bucket_name != null ? var.frontend_bucket_name : "${var.project}-${var.environment}-${data.aws_caller_identity.current.account_id}-frontend"
  mock_issuer_uri       = "${local.api_url_scheme}://${aws_lb.main.dns_name}/default"
  mock_jwk_set_uri      = "${local.mock_issuer_uri}/jwks"
}
