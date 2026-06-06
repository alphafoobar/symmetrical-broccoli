# Skills API

Spring Boot REST API for savings account workflows, with a static HTML UI for
local/manual testing.

## Prerequisites

- Docker with Docker Compose
- Java 25, if running the application from the host
- OpenTofu or Terraform, if deploying AWS infrastructure
- AWS CLI with credentials for the target account, if deploying to AWS

This repository uses OpenTofu-compatible Terraform files under `infra/`. The
commands below use `tofu`; replace `tofu` with `terraform` if that is the tool
installed in your environment.

## Run Locally With Docker

Start the full local stack:

```sh
docker compose up --build
```

The stack runs:

- UI: http://localhost:3000
- API: http://localhost:8080
- Mock OAuth2 issuer: http://localhost:9000/default
- PostgreSQL: localhost:5432, database/user/password `skills`
- Redis: localhost:6379

Generate a local access token for the UI:

```sh
curl -s \
  -u local-ui:local-secret \
  -X POST http://localhost:9000/default/token \
  -d grant_type=client_credentials \
  -d scope=customer.account.my
```

The UI can request this token for you after you enter a local Customer ID. That
Customer ID becomes the JWT subject and scopes the account forms. You can also
copy the `access_token` value into the UI's Bearer Token field, then use the
account forms at http://localhost:3000.

Check the local API health endpoint:

```sh
curl http://localhost:3000/api/v1/health
```

Stop the stack:

```sh
docker compose down
```

Reset local database state:

```sh
docker compose down -v
```

## Debug Locally

For application debugging, run only the local backing services in Docker and run
Spring Boot from the host JVM or IDE.

Start the backing services:

```sh
docker compose up -d postgres redis auth
```

Run the API from the host:

```sh
./gradlew bootRun
```

Or start it with a debugger listening on port `5005`:

```sh
./gradlew bootRun --debug-jvm
```

Attach your IDE debugger to `localhost:5005`. Gradle starts the JVM suspended
when `--debug-jvm` is used, so the application will wait until the debugger is
attached.

The default `application.yaml` already points at the local backing services:

- PostgreSQL: `jdbc:postgresql://localhost:5432/skills`
- Redis: `localhost:6379`
- JWT issuer: `http://localhost:9000`

Generate a token for curl or API client testing:

```sh
TOKEN="$(
  curl -s \
    -u local-ui:local-secret \
    -X POST http://localhost:9000/default/token \
    -d grant_type=client_credentials \
    -d scope=customer.account.my |
  sed -n 's/.*"access_token" : "\([^"]*\)".*/\1/p'
)"
```

Create an account through the locally running API:

```sh
curl -i \
  -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"customerName":"Alice Smith","nickName":"MySavings"}'
```

Run the project checks:

```sh
./gradlew check
```

Use the full Docker stack when you want to test the browser UI. The local nginx
container serves `public/index.html` and proxies `/api/v1` to the Dockerized API
so browser calls stay same-origin.

## Deploy To AWS

The AWS infrastructure in `infra/` provisions:

- VPC with public and private subnets
- Application Load Balancer
- ECS Fargate service for the Spring Boot API
- Aurora PostgreSQL
- ElastiCache for Valkey
- CloudWatch logs
- S3 bucket and CloudFront distribution for static frontend assets

Required deployment inputs:

- `environment`: deployment environment name, for example `dev`
- `image_uri`: ECR image URI for the API container

Optional inputs have defaults in `infra/variables.tf`, including
`aws_region = "ap-southeast-2"`, `project = "skills"`, and `db_name = "skills"`.

### 1. Build And Push The API Image

Set deployment variables:

```sh
export AWS_REGION=ap-southeast-2
export ENVIRONMENT=dev
export AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
export ECR_REPOSITORY=skills-api
export IMAGE_TAG="$ENVIRONMENT"
export IMAGE_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:$IMAGE_TAG"
```

Create the ECR repository if it does not already exist:

```sh
aws ecr describe-repositories \
  --region "$AWS_REGION" \
  --repository-names "$ECR_REPOSITORY" >/dev/null 2>&1 ||
aws ecr create-repository \
  --region "$AWS_REGION" \
  --repository-name "$ECR_REPOSITORY"
```

Build and push the image:

```sh
docker build -t "$ECR_REPOSITORY:$IMAGE_TAG" .

aws ecr get-login-password --region "$AWS_REGION" |
  docker login \
    --username AWS \
    --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"

docker tag "$ECR_REPOSITORY:$IMAGE_TAG" "$IMAGE_URI"
docker push "$IMAGE_URI"
```

### 2. Plan And Apply Infrastructure

Create an environment tfvars file. Do not commit environment tfvars files if
they contain account-specific or sensitive values.

```sh
cat > infra/dev.tfvars <<EOF
environment = "dev"
aws_region  = "ap-southeast-2"
image_uri   = "$IMAGE_URI"
EOF
```

Initialize, validate, and plan:

```sh
cd infra
tofu init
tofu fmt -check
tofu validate
tofu plan -var-file=dev.tfvars
```

Apply after reviewing the plan:

```sh
tofu apply -var-file=dev.tfvars
```

Read the deployed URLs:

```sh
tofu output api_url
tofu output frontend_url
```

### 3. Upload Frontend Assets

After the infrastructure exists, sync the static UI to the frontend bucket:

```sh
aws s3 sync ../public "s3://$(tofu output -raw s3_bucket_name)" --delete
```

CloudFront may take a few minutes to serve updated files. The current frontend
is designed for the local same-origin nginx proxy and calls `/api/v1`; deployed
frontend/API routing may need a CloudFront API origin or an API base URL change
before the hosted UI can call the ALB-backed API directly.

### 4. Destroy A Non-Production Environment

Use this only for disposable environments:

```sh
cd infra
tofu destroy -var-file=dev.tfvars
```

RDS has deletion protection enabled, so database teardown requires explicitly
changing that setting before destroy can complete.
