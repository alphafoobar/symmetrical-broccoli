# Basic solution thinking

1. Use JWT security for all account creation requests, this will have a subject claim that includes the customer id. This is what will make the customer unique. We will build a basic
   security service that will produce a JWT for the customer. This will just be a stub server for
   the sake of the demo.
   - assume customer id is a seven digit number.
1. Create unit tests to cover all key business logic and end points.
1. Create integration tests using test containers, including Redis and Postgres to verify the end to end flow.
1. Create a basic front end to test the account behaviour.
1. Create terraform to deploy build the service in AWS.
   - use s3 bucket to host the frontend UI
   - use api gateway to the ecs service, use generic AWS TLS to the gateway
   - terminate the TLS at the ALB
   - use valkey in aws instead of redis - add comments in terraform as to why.
   - use aurora serverless for the postgresql
1. use mapstruct to convert the request object to DB object.
1. use spring validation of the request object.
1. use flyway for db migration.
   - Account number should be formed as per [docs/BANK-NUMBER.md](docs/BANK-NUMBER.md)
   - Store the base account number (bank code + branch + account number) as a single `account_number` string, and `suffix` as a separate column — only the suffix varies between sub-accounts
   - Unique index on `(account_number, suffix)` as the natural key
   - Separate index on `customer_id` for fetching all accounts belonging to a customer
   - `account_id` UUID is the surrogate primary key — the PK index covers UUID lookups
   - Customer name is a UTF8 string, support upto 250 characters.
   - Account nick name can be up to 30 characters optional, allow UTF8.
   - Allow accounts to be active/inactive/frozen/closed.
1. Business logic a customer can have up to 5 accounts. this logic should be applied when opening an account. we should count how many accounts they have that are not closed in an atomic transaction.
   - If they have 5 or more accounts, throw a `422 Unprocessable Entity` with a RFC 9457 `ProblemDetail` (`type: account-limit-exceeded`) and do not create a new account.
1. Use a DB-based profanity blocklist seeded from [docs/PROFANITY-LIST.md](docs/PROFANITY-LIST.md) via a Flyway migration.
   - Check is an exact case-insensitive match against the `blocked_nickname` table.
   - If matched, throw a `422 Unprocessable Entity` with a RFC 9457 `ProblemDetail` (`type: nickname-not-allowed`).
   - TODO: in production, consider delegating to a managed service (e.g. Perspective API) for ML-based detection beyond exact matching.
1. Handle error scenarios using GlobalExceptionHandler, returning ProblemDetail.
1. Use Resiliance4j to handle database availability issues.
1. Cache get account requests using Redis, invalidate cache when creating an account.
