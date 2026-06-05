---
name: flyway-postgres
description: "Flyway SQL migration conventions for PostgreSQL. Use when adding schema changes, indexes, constraints, seed/reference data, repeatable migrations, or reviewing migration safety."
---

# Flyway PostgreSQL

## Migration Location and Naming

- Versioned migrations live in `src/main/resources/db/migration/`.
- Use `V<integer>__<lower_snake_case>.sql`, for example `V1__create_orders.sql`.
- Do not edit an applied versioned migration. Add a new migration.
- Use repeatable migrations only for views, functions, and derived database objects: `R__refresh_order_view.sql`.

## PostgreSQL DDL Rules

- Every table has a primary key.
- Use `uuid` for externally visible resource IDs.
- Use `timestamptz` for instants.
- Required fields are `not null`.
- Add explicit foreign keys and indexes for lookup/join columns.
- Use check constraints for bounded domain values when the set is stable.

```sql
create table orders (
    id uuid primary key,
    version bigint not null,
    status text not null,
    created_at timestamptz not null,
    constraint orders_status_check check (status in ('PENDING', 'PAID', 'CANCELLED'))
);

create index orders_status_idx on orders (status);
```

## Safety

- Prefer additive migrations for production systems.
- For destructive changes, use expand-migrate-contract:
  1. Add the new structure.
  2. Backfill safely.
  3. Deploy application compatibility.
  4. Remove old structure in a later release.
- Large backfills must be batched or operationally planned.
- Avoid long exclusive locks in migrations.

## Data Migrations

- Reference data migrations must be idempotent when possible with `insert ... on conflict`.
- Do not put environment-specific secrets or credentials in migrations.
- Keep test fixture data out of production migrations.

## Verification

- Load `test-slice-data` or `test-integration` when validating migrations.
- Run Flyway against PostgreSQL in tests.
- Confirm JPA entity mappings match the migrated schema.
