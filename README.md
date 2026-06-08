# Skills API

Spring Boot REST API for savings account workflows, with a static HTML UI for
local/manual testing.

~~ Deployed UI: https://d3ce8dxzblk8d3.cloudfront.net ~~ AWS terraform deployment destroyed.

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
  -d '{"customerName":"Alice Smith","nickname":"MySavings"}'
```

Run the project checks:

```sh
./gradlew check
```

Use the full Docker stack when you want to test the browser UI. The local nginx
container serves `public/index.html` and proxies `/api/v1` to the Dockerized API
so browser calls stay same-origin.

## Deploy To AWS

Deployment is script-driven from the repository root. The infrastructure code in
`infra/` provisions:

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

### 1. Create Environment Variables

Each environment needs a matching tfvars file. For `dev`, create
`infra/dev.tfvars`:

```hcl
environment = "dev"
aws_profile = "projects"
aws_region  = "ap-southeast-6"
```

Do not commit tfvars files if they contain account-specific or sensitive values.

The scripts default to `infra/<environment>.tfvars`, read `aws_profile` and
`aws_region` from that file, and use `tofu` with a `terraform` fallback. You can
override those defaults with environment variables:

- `TF_BIN`: OpenTofu/Terraform binary, for example `tofu` or `terraform`
- `TFVARS`: tfvars file path
- `AWS_PROFILE`: AWS CLI profile
- `AWS_REGION`: AWS region

### 2. Deploy The API

Run:

```sh
./deploy-api.sh dev
```

The script:

1. Initializes OpenTofu.
2. Formats and validates the infrastructure.
3. Creates or updates the ECR repository.
4. Builds the Spring Boot Docker image for ECS Fargate.
5. Pushes the image to ECR.
6. Applies the AWS stack.
7. Forces ECS to pull the pushed image tag.
8. Prints the API URL.

API images are built for `linux/amd64` and tagged with
`<environment>-<UTC timestamp>` by default so ECS points at a specific immutable
artifact.

Useful API deployment overrides:

- `IMAGE_TAG`: custom Docker/ECS image tag
- `DOCKER_PLATFORM`: Docker target platform, default `linux/amd64`
- `AUTO_APPROVE=true`: pass `-auto-approve` to OpenTofu apply commands
- `WAIT_FOR_ECS=false`: skip waiting for ECS service stability

The API load balancer serves HTTP by default. Set `api_certificate_arn` in the
environment tfvars file to an ACM certificate ARN to enable HTTPS on port `443`.

### 3. Deploy The UI

After the infrastructure exists, run:

```sh
./deploy-ui.sh dev
```

The script:

1. Initializes OpenTofu.
2. Validates the infrastructure.
3. Reads the frontend S3 bucket and CloudFront URL from OpenTofu outputs.
4. Syncs `public/` to S3 with `--delete`.
5. Invalidates CloudFront.
6. Prints the frontend URL.

Set `INVALIDATE_CLOUDFRONT=false` if you want to skip the invalidation step.

The hosted frontend keeps the same paths as local Docker Compose: `/api/*`
forwards to the API and `/default/*` forwards to the mock OAuth2 issuer.

### Runtime Secrets

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

### Destroy A Non-Production Environment

Use this only for disposable environments:

```sh
cd infra
tofu destroy -var-file=dev.tfvars
```

RDS has deletion protection enabled, so database teardown requires explicitly
changing that setting before destroy can complete.
