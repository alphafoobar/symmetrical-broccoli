#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$ROOT_DIR/infra"
PUBLIC_DIR="$ROOT_DIR/public"

usage() {
  cat <<'USAGE'
Usage: ./deploy-ui.sh [environment]

Deploy the static UI to AWS:
  1. initialize OpenTofu
  2. read the frontend S3 bucket and CloudFront URL from outputs
  3. sync public/ to S3
  4. invalidate CloudFront

Environment variables:
  TF_BIN                OpenTofu/Terraform binary. Default: tofu, or terraform fallback
  TFVARS                tfvars file. Default: infra/<environment>.tfvars
  AWS_PROFILE           AWS CLI profile. Default: aws_profile from tfvars, then projects
  AWS_REGION            AWS region. Default: aws_region from tfvars, then ap-southeast-6
  INVALIDATE_CLOUDFRONT Set false to skip CloudFront invalidation
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
INVALIDATE_CLOUDFRONT="${INVALIDATE_CLOUDFRONT:-true}"

export AWS_PROFILE
export AWS_REGION

require_command "$TF_BIN"
require_command aws

if [[ ! -d "$PUBLIC_DIR" ]]; then
  printf 'Missing public asset directory: %s\n' "$PUBLIC_DIR" >&2
  exit 1
fi

printf 'Deploying UI environment=%s region=%s profile=%s\n' \
  "$ENVIRONMENT" "$AWS_REGION" "$AWS_PROFILE"

"$TF_BIN" -chdir="$INFRA_DIR" init
"$TF_BIN" -chdir="$INFRA_DIR" validate

S3_BUCKET="$("$TF_BIN" -chdir="$INFRA_DIR" output -raw s3_bucket_name)"
FRONTEND_URL="$("$TF_BIN" -chdir="$INFRA_DIR" output -raw frontend_url)"
FRONTEND_DOMAIN="${FRONTEND_URL#https://}"

printf 'Syncing %s to s3://%s\n' "$PUBLIC_DIR" "$S3_BUCKET"
aws s3 sync "$PUBLIC_DIR" "s3://$S3_BUCKET" --delete --region "$AWS_REGION"

if [[ "$INVALIDATE_CLOUDFRONT" == "true" ]]; then
  DISTRIBUTION_ID="$(aws cloudfront list-distributions \
    --query "DistributionList.Items[?DomainName=='${FRONTEND_DOMAIN}'].Id | [0]" \
    --output text)"

  if [[ -z "$DISTRIBUTION_ID" || "$DISTRIBUTION_ID" == "None" ]]; then
    printf 'Could not find CloudFront distribution for %s; skipping invalidation.\n' "$FRONTEND_DOMAIN" >&2
  else
    printf 'Invalidating CloudFront distribution %s\n' "$DISTRIBUTION_ID"
    aws cloudfront create-invalidation \
      --distribution-id "$DISTRIBUTION_ID" \
      --paths '/*' >/dev/null
  fi
fi

printf 'Frontend URL: %s\n' "$FRONTEND_URL"
