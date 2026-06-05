---
name: test-integration
description: "Full-stack integration testing with @SpringBootTest and Testcontainers. Use when testing complete request-to-database flows, Flyway migrations in context, cross-layer behaviour, or verifying the assembled application. Covers @ServiceConnection, TestRestTemplate, @Sql fixture setup, and shared container patterns."
---

# Integration Testing — `@SpringBootTest`

## Dependencies

```kotlin
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:postgresql")
```

## When to Use

Integration tests are **expensive** — load the full Spring context + real PostgreSQL. Only use them for:

- Full request → service → repository → DB → response flows
- Cross-layer behaviour that cannot be tested in slices
- Verifying security filters interact correctly with real request processing
- Smoke-testing the assembled application

For everything else, use unit tests or slice tests.

## Class Structure

Test class name: `<Feature>IT` (IT suffix = integration test, separates from unit test runs).

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private TestRestTemplate restTemplate;
}
```

`RANDOM_PORT` starts a real embedded server on a random port. `TestRestTemplate` is auto-configured to point at it.

## Full Request Flow Test

```java
@Test
@DisplayName("POST then GET returns the created order")
void shouldCreateAndRetrieveOrder() {
    // given
    val request = new CreateOrderRequest(
        UUID.randomUUID(),
        List.of(new OrderItemRequest(UUID.randomUUID(), 2))
    );

    // when — create
    val createResponse = restTemplate.postForEntity(
        "/api/v1/orders", request, OrderResponse.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    val created = createResponse.getBody();
    assertThat(created).isNotNull();

    // when — retrieve
    val getResponse = restTemplate.getForEntity(
        "/api/v1/orders/{id}", OrderResponse.class, created.id());

    // then
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody().id()).isEqualTo(created.id());
    assertThat(getResponse.getBody().status()).isEqualTo("PENDING");
}
```

## `@Sql` — Fixture Setup and Teardown

Use `@Sql` to put the database in a known state before a test and clean up after.

```java
@Test
@Sql(scripts = "/fixtures/three-active-orders.sql")
@Sql(scripts = "/fixtures/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@DisplayName("GET /api/v1/orders returns all active orders")
void shouldReturnAllActiveOrders() {
    val response = restTemplate.getForEntity("/api/v1/orders?status=ACTIVE", OrderResponse[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(3);
}
```

Alternatively, annotate the class with `@Transactional` so each test rolls back automatically — but only when `RANDOM_PORT` is not used (transactions don't span the HTTP boundary).

## Authenticated Requests

Inject a `TestRestTemplate` with credentials, or build a bearer token manually for JWT-secured endpoints:

```java
private HttpHeaders bearerHeaders(final String token) {
    val headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
}

@Test
@DisplayName("returns 401 when no token provided")
void shouldReturn401WithNoToken() {
    val response = restTemplate.getForEntity("/api/v1/orders", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
}

@Test
@DisplayName("returns 200 when valid token provided")
void shouldReturn200WithValidToken() {
    val token = generateTestJwt("user-id", List.of("ROLE_USER"));
    val entity = new HttpEntity<>(bearerHeaders(token));

    val response = restTemplate.exchange(
        "/api/v1/orders", HttpMethod.GET, entity, OrderResponse[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
}
```

## Shared Context with `@SpringBootTest`

Spring caches the application context between tests with identical configuration. Share the container across all integration tests using a base class to avoid redundant context restarts:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
abstract class IntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17");

    @Autowired
    protected TestRestTemplate restTemplate;
}

class OrderIT extends IntegrationTest {

    @Test
    void shouldCreateOrder() { ... }
}

class PaymentIT extends IntegrationTest {

    @Test
    void shouldProcessPayment() { ... }
}
```

This allows all `IT` classes to share **one** Spring context startup and **one** Testcontainers instance per test run.

## Resilience4j — Circuit Breaker Integration

Test circuit breaker behaviour by triggering consecutive failures:

```java
@Test
@DisplayName("circuit opens after threshold failures and returns fallback")
void circuitShouldOpenAfterFailures() {
    // Stub external service to fail
    wireMockServer.stubFor(post(urlEqualTo("/payments"))
        .willReturn(serverError()));

    // Exhaust the failure threshold
    IntStream.range(0, 5).forEach(_ ->
        restTemplate.postForEntity("/api/v1/orders/{id}/pay", null, String.class, orderId));

    // Circuit is now open — expect fallback response
    val response = restTemplate.postForEntity(
        "/api/v1/orders/{id}/pay", null, ProblemDetail.class, orderId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
}
```

## What NOT to Do

- No `@SpringBootTest` for logic testable in unit or slice tests — use the lightest test that covers the scenario
- No `postgres:latest` — pin to a specific version
- No `Thread.sleep()` for async flows — use `Awaitility.await().until(...)`
- No `@DynamicPropertySource` when `@ServiceConnection` covers it
- Do not reset application context between tests unnecessarily — share via base class
