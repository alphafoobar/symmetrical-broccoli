# Skills API

Spring Boot REST API for savings account workflows, with a static HTML UI for
local/manual testing.

## Prerequisites

- Docker with Docker Compose
- Java 25, if running the application from the host
- OpenTofu or Terraform, if deploying AWS infrastructure
- AWS CLI with a `projects` profile for the target account, if deploying to AWS

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
- ECR repository for the Spring Boot API image
- Application Load Balancer
- ECS Fargate service for the Spring Boot API
- ECS Fargate service for the mock OAuth2 issuer
- Aurora Serverless v2 PostgreSQL
- ElastiCache for Valkey
- CloudWatch logs
- S3 bucket and CloudFront distribution for static frontend assets, API calls,
  and mock OAuth2 calls

Deployment scripts are available from the repository root:

```sh
./deploy-api.sh dev
./deploy-ui.sh dev
```

The scripts default to `infra/<environment>.tfvars`, `tofu`, the AWS profile and
region from the tfvars file. API images are built for `linux/amd64` and tagged
with `<environment>-<UTC timestamp>` by default so ECS points at a specific
immutable artifact. Set `IMAGE_TAG` only when you need a custom, unused tag.
Set `AUTO_APPROVE=true` for non-interactive OpenTofu applies.

Required deployment inputs:

- `environment`: deployment environment name, for example `dev`

Optional inputs have defaults in `infra/variables.tf`, including
`aws_profile = "projects"`, `aws_region = "ap-southeast-6"` (Auckland),
`project = "skills"`, `db_name = "skills"`, `ecr_repository_name =
"skills-api"`, `mock_oauth2_image`, and `image_tag`.

The API load balancer serves HTTP by default. Set `api_certificate_arn` to an
ACM certificate ARN to enable HTTPS on port `443`.

### 1. Create Deployment Variables

Set common shell variables:

```sh
export AWS_PROFILE=projects
export AWS_REGION=ap-southeast-6
export ENVIRONMENT=dev
export ECR_REPOSITORY=skills-api
```

Create an environment tfvars file. Do not commit environment tfvars files if
they contain account-specific or sensitive values.

```sh
cat > infra/dev.tfvars <<EOF
environment = "dev"
aws_profile = "projects"
aws_region  = "ap-southeast-6"
EOF
```

### 2. Create ECR

Initialize OpenTofu and create the ECR repository first. This lets Docker push
the image before the ECS service is created.

```sh
cd infra
tofu init
tofu apply \
  -target=aws_ecr_repository.api \
  -target=aws_ecr_lifecycle_policy.api \
  -var-file=dev.tfvars
cd ..

export AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
export ECR_REPOSITORY_URL="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY"
export IMAGE_TAG="$ENVIRONMENT-$(date -u +%Y%m%dT%H%M%SZ)"
export IMAGE_URI="$ECR_REPOSITORY_URL:$IMAGE_TAG"
```

### 3. Build And Push The API Image

Build and push the image:

```sh
docker build --platform linux/amd64 -t "$IMAGE_URI" .

aws ecr get-login-password --region "$AWS_REGION" |
  docker login \
    --username AWS \
    --password-stdin "$ECR_REPOSITORY_URL"

docker push "$IMAGE_URI"
```

### 4. Plan And Apply Infrastructure

Initialize, validate, and plan:

```sh
cd infra
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

OpenTofu creates a separate Secrets Manager secret for the low-privilege
application database user. The initial password is generated through AWS Secrets
Manager and stored in the remote OpenTofu state, so treat state access as
privileged and keep the backend encrypted and access-controlled.

To rotate the application database password later, update Secrets Manager and
then alter the PostgreSQL role through an admin connection:

```sh
APP_DB_SECRET_ARN="$(tofu output -raw db_app_secret_arn)"
APP_DB_PASSWORD="$(aws secretsmanager get-random-password \
  --password-length 48 \
  --exclude-punctuation \
  --query RandomPassword \
  --output text)"

aws secretsmanager put-secret-value \
  --secret-id "$APP_DB_SECRET_ARN" \
  --secret-string "{\"username\":\"skills_app\",\"password\":\"$APP_DB_PASSWORD\"}"
```

The API service receives non-secret runtime config from the ECS task definition,
including `JDBC_DATABASE_URL`, Redis endpoints, JWT issuer metadata, and
`SPRING_CONFIG_IMPORT`. Spring Cloud AWS uses `SPRING_CONFIG_IMPORT` to read the
application database secret and RDS master secret from Secrets Manager at
startup. The normal datasource uses the low-privilege `skills_app` credentials;
Flyway uses the imported RDS master credentials only for migrations and grants.

### 5. Upload Frontend Assets

After the infrastructure exists, sync the static UI to the frontend bucket:

```sh
aws s3 sync ../public "s3://$(tofu output -raw s3_bucket_name)" --delete
```

CloudFront may take a few minutes to serve updated files. The hosted frontend
keeps the same paths as local Docker Compose: `/api/*` forwards to the API and
`/default/*` forwards to the mock OAuth2 issuer.

### 6. Destroy A Non-Production Environment

Use this only for disposable environments:

```sh
cd infra
tofu destroy -var-file=dev.tfvars
```

RDS has deletion protection enabled, so database teardown requires explicitly
changing that setting before destroy can complete.

### Useful AWS Commands

Check which account the `projects` profile points at:

```sh
aws sts get-caller-identity --profile projects
```

Force a new ECS deployment after pushing a replacement image with the same tag:

```sh
aws ecs update-service \
  --profile projects \
  --region "$AWS_REGION" \
  --cluster "$(tofu -chdir=infra output -raw cluster_name)" \
  --service "$(tofu -chdir=infra output -raw service_name)" \
  --force-new-deployment
```
