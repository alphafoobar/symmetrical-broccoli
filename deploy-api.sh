#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$ROOT_DIR/infra"

usage() {
  cat <<'USAGE'
Usage: ./deploy-api.sh [environment]

Build and deploy the Spring Boot API to AWS:
  1. initialize OpenTofu
  2. create/update ECR
  3. build and push the Docker image
  4. apply the AWS stack
  5. force ECS to pull the pushed image tag

Environment variables:
  TF_BIN         OpenTofu/Terraform binary. Default: tofu, or terraform fallback
  TFVARS        tfvars file. Default: infra/<environment>.tfvars
  AWS_PROFILE   AWS CLI profile. Default: aws_profile from tfvars, then projects
  AWS_REGION    AWS region. Default: aws_region from tfvars, then ap-southeast-6
  IMAGE_TAG     Docker/ECS image tag. Default: <environment>-<UTC timestamp>
  DOCKER_PLATFORM  Docker target platform. Default: linux/amd64 for ECS Fargate
  AUTO_APPROVE  Set true to add -auto-approve to OpenTofu apply commands
  WAIT_FOR_ECS  Set false to skip waiting for ECS service stability
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

command_or_fallback() {
  if [[ -n "${TF_BIN:-}" ]]; then
    printf '%s\n' "$TF_BIN"
    return
  fi

  if command -v tofu >/dev/null 2>&1; then
    printf '%s\n' "tofu"
    return
  fi

  if command -v terraform >/dev/null 2>&1; then
    printf '%s\n' "terraform"
    return
  fi

  printf '%s\n' "tofu"
}

resolve_tfvars() {
  local requested="$1"

  if [[ "$requested" == /* ]]; then
    printf '%s\n' "$requested"
  elif [[ -f "$ROOT_DIR/$requested" ]]; then
    printf '%s\n' "$ROOT_DIR/$requested"
  else
    printf '%s\n' "$INFRA_DIR/$requested"
  fi
}

tfvar_string() {
  local name="$1"
  awk -v name="$name" '
    $0 ~ "^[[:space:]]*" name "[[:space:]]*=" {
      value = $0
      sub(/^[^=]*=/, "", value)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      gsub(/^"|"$/, "", value)
      print value
      exit
    }
  ' "$TFVARS_FILE"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Missing required command: %s\n' "$1" >&2
    exit 1
  fi
}

ENVIRONMENT="${1:-${ENVIRONMENT:-dev}}"
TFVARS_FILE="$(resolve_tfvars "${TFVARS:-${ENVIRONMENT}.tfvars}")"
TF_BIN="$(command_or_fallback)"

if [[ ! -f "$TFVARS_FILE" ]]; then
  printf 'Missing tfvars file: %s\n' "$TFVARS_FILE" >&2
  printf 'Create it or pass TFVARS=/path/to/file.\n' >&2
  exit 1
fi

AWS_PROFILE="${AWS_PROFILE:-$(tfvar_string aws_profile)}"
AWS_PROFILE="${AWS_PROFILE:-projects}"
AWS_REGION="${AWS_REGION:-$(tfvar_string aws_region)}"
AWS_REGION="${AWS_REGION:-ap-southeast-6}"
IMAGE_TAG="${IMAGE_TAG:-$ENVIRONMENT-$(date -u +%Y%m%dT%H%M%SZ)}"
DOCKER_PLATFORM="${DOCKER_PLATFORM:-linux/amd64}"
AUTO_APPROVE="${AUTO_APPROVE:-false}"
WAIT_FOR_ECS="${WAIT_FOR_ECS:-true}"

export AWS_PROFILE
export AWS_REGION

require_command "$TF_BIN"
require_command aws
require_command docker

TF_APPLY_ARGS=(-var-file="$TFVARS_FILE" -var="image_tag=$IMAGE_TAG")
if [[ "$AUTO_APPROVE" == "true" ]]; then
  TF_APPLY_ARGS+=(-auto-approve)
fi

printf 'Deploying API environment=%s region=%s profile=%s image_tag=%s platform=%s\n' \
  "$ENVIRONMENT" "$AWS_REGION" "$AWS_PROFILE" "$IMAGE_TAG" "$DOCKER_PLATFORM"

"$TF_BIN" -chdir="$INFRA_DIR" init
"$TF_BIN" -chdir="$INFRA_DIR" fmt -check -recursive
"$TF_BIN" -chdir="$INFRA_DIR" validate

"$TF_BIN" -chdir="$INFRA_DIR" apply \
  -target=aws_ecr_repository.api \
  -target=aws_ecr_lifecycle_policy.api \
  "${TF_APPLY_ARGS[@]}"

ECR_REPOSITORY_URL="$("$TF_BIN" -chdir="$INFRA_DIR" output -raw ecr_repository_url)"
ECR_REGISTRY="${ECR_REPOSITORY_URL%/*}"
IMAGE_URI="${ECR_REPOSITORY_URL}:${IMAGE_TAG}"

printf 'Building Docker image %s for platform %s\n' "$IMAGE_URI" "$DOCKER_PLATFORM"
docker build --platform "$DOCKER_PLATFORM" -t "$IMAGE_URI" "$ROOT_DIR"

printf 'Logging in to ECR registry %s\n' "$ECR_REGISTRY"
aws ecr get-login-password --region "$AWS_REGION" |
  docker login --username AWS --password-stdin "$ECR_REGISTRY"

printf 'Pushing Docker image %s\n' "$IMAGE_URI"
docker push "$IMAGE_URI"

"$TF_BIN" -chdir="$INFRA_DIR" apply "${TF_APPLY_ARGS[@]}"

CLUSTER_NAME="$("$TF_BIN" -chdir="$INFRA_DIR" output -raw cluster_name)"
SERVICE_NAME="$("$TF_BIN" -chdir="$INFRA_DIR" output -raw service_name)"

printf 'Forcing ECS service deployment cluster=%s service=%s\n' "$CLUSTER_NAME" "$SERVICE_NAME"
aws ecs update-service \
  --region "$AWS_REGION" \
  --cluster "$CLUSTER_NAME" \
  --service "$SERVICE_NAME" \
  --force-new-deployment >/dev/null

if [[ "$WAIT_FOR_ECS" == "true" ]]; then
  aws ecs wait services-stable \
    --region "$AWS_REGION" \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME"
fi

printf 'API URL: %s\n' "$("$TF_BIN" -chdir="$INFRA_DIR" output -raw api_url)"
