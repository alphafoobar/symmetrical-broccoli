---
name: spring-data-jpa-entities
description: "Spring Data JPA entity, repository, transaction, auditing, optimistic locking, and PostgreSQL mapping conventions. Use when creating or modifying @Entity classes, repositories, JPQL/native queries, transactions, or persistence tests."
---

# Spring Data JPA Entities

## Entity Defaults

- Entities are mutable persistence models with controlled domain methods, not DTOs.
- Do not use records for JPA entities.
- Use `@Getter`, `@Builder`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, and `@AllArgsConstructor`.
- Do not use `@Data` or `@Setter` on entities.
- Use `UUID` primary keys for externally visible resources.
- Add `@Version` for optimistic locking on aggregate roots.

```java
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Order {

  @Id
  private UUID id;

  @Version
  private long version;

  @Column(nullable = false)
  private String status;

  public void markPaid() {
    status = "PAID";
  }
}
```

## Mapping Rules

- Every required column has `nullable = false`.
- Use enums deliberately. Prefer string persistence with explicit enum names if values are stable.
- Avoid eager relationships unless the aggregate always needs the child data.
- Use `@ManyToOne(fetch = FetchType.LAZY)` by default.
- Avoid bidirectional relationships unless queries and lifecycle rules justify them.
- Use database constraints in Flyway migrations; JPA annotations are not enough.

## Repositories

- Repository interfaces stay narrow. Do not expose query methods that bypass aggregate rules.
- Use derived queries for simple lookups and JPQL for explicit joins/projections.
- Use native queries only when PostgreSQL-specific features are required.
- Return `Optional<T>` for single-row lookups that may be absent.

## Transactions

- Put `@Transactional` on service methods, not controllers.
- Read-only queries use `@Transactional(readOnly = true)`.
- Keep transaction boundaries short; do not perform slow external calls inside write transactions.

## Testing

- Load `test-slice-data` for repository/entity mapping tests.
- Use PostgreSQL Testcontainers, not H2.
- Verify Flyway migrations and JPA mappings together for new entities.
