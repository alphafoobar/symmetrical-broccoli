---
name: test-unit
description: "Unit testing with JUnit 5 and Mockito. Use when testing Spring services, domain logic, or any class in isolation — no Spring context loaded. Covers @ExtendWith(MockitoExtension.class), @Mock, @InjectMocks, BDD-style stubbing with given/willReturn, AssertJ assertions, and @Nested grouping."
---

# Unit Testing Conventions

## Dependencies

Included via `spring-boot-starter-test` — no extra declaration needed:

- JUnit 5 (Jupiter)
- Mockito (`mockito-core`, `mockito-junit-jupiter`)
- AssertJ

## Class Structure

Test class name: `<Subject>Test`. Use `@ExtendWith(MockitoExtension.class)` — never load a Spring context for unit tests.

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;
}
```

## BDD-Style Stubs — `given` / `willReturn`

Use Mockito's BDD API (`BDDMockito`) for readability. Import statically: `given`, `willThrow`, `then`, `never`.

```java
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Test
@DisplayName("should return order when found by id")
void shouldReturnOrderWhenFoundById() {
    // given
    val id = UUID.randomUUID();
    val order = Order.builder().id(id).status("ACTIVE").total(new BigDecimal("99.99")).build();
    given(orderRepository.findById(id)).willReturn(Optional.of(order));

    // when
    val result = orderService.findById(id);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().status()).isEqualTo("ACTIVE");
}
```

## Exception Testing

```java
@Test
@DisplayName("should throw OrderNotFoundException when order does not exist")
void shouldThrowWhenOrderNotFound() {
    // given
    val id = UUID.randomUUID();
    given(orderRepository.findById(id)).willReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> orderService.findById(id))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessageContaining(id.toString());
}
```

## Verifying Interactions

```java
@Test
@DisplayName("should not send notification when order creation fails")
void shouldNotSendNotificationOnFailure() {
    // given
    given(orderRepository.save(any())).willThrow(new DataIntegrityViolationException("duplicate"));

    // when / then
    assertThatThrownBy(() -> orderService.create(request))
        .isInstanceOf(DataIntegrityViolationException.class);

    then(notificationService).should(never()).send(any());
}
```

## `@Nested` — Grouping Related Scenarios

Group tests by method or behaviour with `@Nested` classes. Each nested class gets its own `@DisplayName`.

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @InjectMocks private OrderService orderService;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns order when found")
        void returnsOrderWhenFound() { ... }

        @Test
        @DisplayName("throws when not found")
        void throwsWhenNotFound() { ... }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("persists and returns order on valid request")
        void persistsOnValidRequest() { ... }
    }
}
```

## Test Data — Builders Not Magic Literals

Build test objects with builders or dedicated factory methods. Never scatter literal UUIDs and strings across multiple tests.

```java
// In a shared test helper or as static factory methods on the test class
static Order anOrder() {
    return Order.builder()
        .id(UUID.randomUUID())
        .customerId(UUID.randomUUID())
        .status("ACTIVE")
        .total(new BigDecimal("50.00"))
        .build();
}

static Order anOrder(UnaryOperator<Order.OrderBuilder> customizer) {
    return customizer.apply(Order.builder()
        .id(UUID.randomUUID())
        .status("ACTIVE"))
        .build();
}
```

## AssertJ Idioms

```java
// Collections
assertThat(result).hasSize(3).extracting(Order::status).containsOnly("ACTIVE");

// Optional
assertThat(result).isPresent().hasValueSatisfying(o -> {
    assertThat(o.id()).isNotNull();
    assertThat(o.status()).isEqualTo("ACTIVE");
});

// Exception message
assertThatThrownBy(() -> service.call())
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessage("expected message");

// Soft assertions (multiple failures reported together)
assertSoftly(softly -> {
    softly.assertThat(order.id()).isNotNull();
    softly.assertThat(order.status()).isEqualTo("ACTIVE");
    softly.assertThat(order.total()).isPositive();
});
```

## What NOT to Do

- No `@SpringBootTest` for service logic — it's 50-100× slower and unnecessary
- No `@MockBean` — it's removed in Spring Boot 4; use `@MockitoBean` in slice tests
- No `Mockito.when(...)` — use `BDDMockito.given(...)` for consistency
- No assertions on mock call count unless the interaction itself is the behaviour under test
- No `Thread.sleep()` — use `Awaitility` if testing async code
