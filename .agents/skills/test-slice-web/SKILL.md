---
name: test-slice-web
description: "Controller slice testing with @WebMvcTest and MockMvc. Use when testing REST controllers, request validation, response serialization, and HTTP status codes without loading the full application context. Covers @MockitoBean, MockMvc fluent API, JSON path assertions, and validation error testing."
---

# Web Slice Testing — `@WebMvcTest`

## What `@WebMvcTest` Loads

Only the web layer: controllers, filters, `@ControllerAdvice`, argument resolvers, and `WebMvcConfigurer`. Nothing else. All service and repository dependencies must be provided as mocks with `@MockitoBean`.

## Class Structure

Test class name: `<Controller>Test`. Specify the controller under test in `@WebMvcTest` to avoid loading unrelated controllers.

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;
}
```

> **Note:** `@MockitoBean` replaces the deprecated `@MockBean` (removed in Spring Boot 4).

## GET — Happy Path

```java
@Test
@DisplayName("GET /api/v1/orders/{id} returns 200 with order body")
void shouldReturn200WithOrderBody() throws Exception {
    // given
    val id = UUID.randomUUID();
    val response = new OrderResponse(id, "ACTIVE", new BigDecimal("99.99"), Instant.now());
    given(orderService.findById(id)).willReturn(Optional.of(response));

    // when / then
    mockMvc.perform(get("/api/v1/orders/{id}", id)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.total").value(99.99));
}
```

## GET — Not Found → `ProblemDetail`

```java
@Test
@DisplayName("GET /api/v1/orders/{id} returns 404 ProblemDetail when order not found")
void shouldReturn404WhenOrderNotFound() throws Exception {
    val id = UUID.randomUUID();
    given(orderService.findById(id)).willReturn(Optional.empty());

    mockMvc.perform(get("/api/v1/orders/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Order Not Found"))
        .andExpect(jsonPath("$.status").value(404));
}
```

## POST — Created

```java
@Test
@DisplayName("POST /api/v1/orders returns 201 with location header")
void shouldReturn201OnCreate() throws Exception {
    // given
    val request = new CreateOrderRequest(UUID.randomUUID(), List.of(...));
    val created = new OrderResponse(UUID.randomUUID(), "PENDING", new BigDecimal("49.99"), Instant.now());
    given(orderService.create(any())).willReturn(created);

    // when / then
    mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.status").value("PENDING"));
}
```

## POST — Bean Validation Failure → 400

```java
@Test
@DisplayName("POST /api/v1/orders returns 400 when request is invalid")
void shouldReturn400OnInvalidRequest() throws Exception {
    // Empty items list — fails @NotEmpty
    val request = new CreateOrderRequest(UUID.randomUUID(), List.of());

    mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid Request"))
        .andExpect(jsonPath("$.violations").isArray());
}
```

## Asserting Response Structure with `andDo(print())`

During development, add `.andDo(print())` to log the full request and response. Remove before committing.

```java
mockMvc.perform(get("/api/v1/orders/{id}", id))
    .andDo(print())  // remove before commit
    .andExpect(status().isOk());
```

## Reusable `perform` Helpers

For controllers with many endpoints, extract common setup into private helpers:

```java
private ResultActions getOrder(UUID id) throws Exception {
    return mockMvc.perform(get("/api/v1/orders/{id}", id)
        .accept(MediaType.APPLICATION_JSON));
}

private ResultActions createOrder(CreateOrderRequest request) throws Exception {
    return mockMvc.perform(post("/api/v1/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)));
}
```

## `@Nested` — Group by Endpoint

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean OrderService orderService;

    @Nested
    @DisplayName("GET /api/v1/orders/{id}")
    class GetById {

        @Test @DisplayName("returns 200 when found") void returns200() throws Exception { ... }
        @Test @DisplayName("returns 404 when not found") void returns404() throws Exception { ... }
    }

    @Nested
    @DisplayName("POST /api/v1/orders")
    class Create {

        @Test @DisplayName("returns 201 on valid request") void returns201() throws Exception { ... }
        @Test @DisplayName("returns 400 on invalid body") void returns400() throws Exception { ... }
    }
}
```

## What NOT to Do

- No `@SpringBootTest` for controller tests — `@WebMvcTest` is 10× faster
- No calling real service methods — always `@MockitoBean` the service
- No `@MockBean` — removed in Spring Boot 4
- No asserting internal service state — test the HTTP contract only
- No ignoring error response structure — always assert `ProblemDetail` fields on error paths
