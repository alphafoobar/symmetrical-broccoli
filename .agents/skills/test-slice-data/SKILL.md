---
name: test-slice-data
description: "JPA repository slice testing with @DataJpaTest and Testcontainers. Use when testing Spring Data repositories, custom JPQL/native queries, entity mappings, and Flyway migrations against a real PostgreSQL database. Covers @ServiceConnection, @Sql, query result assertions with AssertJ."
---

# Data Slice Testing — `@DataJpaTest`

## Dependencies

Add to `build.gradle.kts` (versions managed by Spring Boot 4):

```kotlin
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:postgresql")
```

## What `@DataJpaTest` Loads

JPA repositories, the entity manager, Flyway, and the datasource. Nothing else — no web layer, no services. By default it replaces the datasource with H2 in-memory; override this with `@AutoConfigureTestDatabase(replace = NONE)` to use the real Testcontainers PostgreSQL.

## Class Structure

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;
}
```

`@ServiceConnection` automatically wires the container URL, username, and password into Spring's datasource — no manual `@DynamicPropertySource` needed.

## Basic CRUD Assertions

```java
@Test
@DisplayName("should persist and retrieve order by id")
void shouldPersistAndRetrieveOrder() {
    // given
    val order = Order.builder()
        .customerId(UUID.randomUUID())
        .status("PENDING")
        .total(new BigDecimal("75.00"))
        .build();

    // when
    val saved = orderRepository.save(order);
    entityManager.flush();
    entityManager.clear(); // evict from first-level cache — forces real DB read

    val found = orderRepository.findById(saved.getId());

    // then
    assertThat(found).isPresent();
    assertThat(found.get().getStatus()).isEqualTo("PENDING");
    assertThat(found.get().getTotal()).isEqualByComparingTo("75.00");
}
```

## Custom Query Testing

```java
@Test
@DisplayName("findByCustomerIdAndStatus returns only matching orders")
void shouldReturnMatchingOrdersOnly() {
    // given
    val customerId = UUID.randomUUID();
    val active = buildAndSave(customerId, "ACTIVE");
    val closed = buildAndSave(customerId, "CLOSED");
    buildAndSave(UUID.randomUUID(), "ACTIVE"); // different customer — should not appear

    entityManager.flush();
    entityManager.clear();

    // when
    val results = orderRepository.findByCustomerIdAndStatus(customerId, "ACTIVE");

    // then
    assertThat(results)
        .hasSize(1)
        .extracting(Order::getId)
        .containsOnly(active.getId());
}
```

## `@Sql` — Seed Data for Complex Scenarios

For complex query tests requiring many rows, use `@Sql` to load a fixture script instead of creating objects in Java.

```java
@Test
@Sql("/fixtures/orders-mixed-statuses.sql")
@DisplayName("countByStatus returns correct totals")
void shouldCountByStatus() {
    val counts = orderRepository.countByStatus();
    assertThat(counts).contains(
        entry("ACTIVE", 3L),
        entry("CLOSED", 2L)
    );
}
```

Place fixture scripts in `src/test/resources/fixtures/`.

## Flyway Migration Validation

`@DataJpaTest` runs Flyway by default when it's on the classpath. A migration error will fail the test, giving you fast feedback without starting the full app.

```java
@Test
@DisplayName("Flyway migrations apply cleanly")
void flywayMigrationsApplyCleanly() {
    // If this test runs, all migrations in db/migration/ are valid.
    // No body needed — the context startup is the assertion.
}
```

## `TestEntityManager` vs Repository

Use `TestEntityManager` to set up state (save + flush + clear) when you want to bypass the repository's caching. Use the repository itself only for the **action under test**.

```java
// Setup — use TestEntityManager for fixture data
val entity = entityManager.persistFlushFind(Order.builder()...build());

// Act — use the repository
val result = orderRepository.findByCustomerId(entity.getCustomerId());

// Assert
assertThat(result).hasSize(1);
```

## Shared Testcontainers Instance

For test suites with many `@DataJpaTest` classes, share a single container to avoid repeated startup cost. Create a base class:

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
abstract class PostgresSliceTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17");
}

class OrderRepositoryTest extends PostgresSliceTest {
    @Autowired OrderRepository orderRepository;
    // ...
}
```

## What NOT to Do

- No H2 in-memory DB — `@AutoConfigureTestDatabase(replace = NONE)` is mandatory
- No `postgres:latest` — pin to an explicit version matching production
- No `@SpringBootTest` for repository tests — `@DataJpaTest` is far faster
- No skipping `entityManager.flush(); entityManager.clear()` — without it you may be asserting against the first-level cache, not the database
