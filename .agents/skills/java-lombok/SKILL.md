---
name: java-lombok
description: "Lombok annotation rules for Spring Boot Java. Use when writing Spring services, components, repositories, JPA entities, or any class requiring logger setup, builders, or constructor injection. Covers @RequiredArgsConstructor, @Slf4j, @Builder, @FieldDefaults, and what NOT to use."
---

# Lombok Conventions

## Dependency Injection — `@RequiredArgsConstructor`

**Never** `@Autowired` on fields. Always constructor-inject via Lombok.

```java
// CORRECT
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;
}

// WRONG — never do this
@Service
public class OrderService {
    @Autowired private OrderRepository orderRepository;
}
```

## Logging — `@Slf4j`

Every class that logs uses `@Slf4j`. No manual `LoggerFactory` calls.

Load `java-logging` before adding or changing log statements. This skill only covers logger setup.

```java
// CORRECT
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    public void process(Order order) {
        log.atInfo()
            .addKeyValue("orderId", order.id())
            .log("Processing order");
    }
}

// WRONG
private static final Logger log = LoggerFactory.getLogger(OrderService.class);
```

## Immutability — prefer records over `@Value`

For DTOs, responses, and value objects, **use Java records** with `@Builder`. Lombok `@Value` is only acceptable for cases where a record genuinely cannot be used (e.g., JPA embeddables in legacy contexts).

```java
// CORRECT — record with builder for response body
@Builder
public record OrderResponse(UUID id, String status, BigDecimal total) {}

// AVOID — Lombok @Value when a record works
@Value
public class OrderResponse {
    UUID id;
    String status;
    BigDecimal total;
}
```

## Builders — `@Builder` / `@SuperBuilder`

Use `@Builder` on **every** DTO, record, POJO, and JPA entity that has more than one field. Callers must always construct via the builder — never via positional `new X(a, b, c)` calls, which break silently when fields are reordered.

Use `@SuperBuilder` when inheritance is involved.

Lombok supports `@Builder` directly on records:

```java
// CORRECT — record with builder
@Builder
public record CreateOrderRequest(UUID customerId, List<OrderItemRequest> items) {}

// CORRECT — record response with builder
@Builder
public record OrderResponse(UUID id, String status, BigDecimal total) {}

// Usage — always use the builder
val request = CreateOrderRequest.builder()
    .customerId(UUID.randomUUID())
    .items(List.of(item))
    .build();
```

For JPA entities, `@Builder` combines with the standard entity annotations:

```java
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Order {
    @Id
    private UUID id;
    private String status;
    private BigDecimal total;
}

// Usage
val order = Order.builder()
    .id(UUID.randomUUID())
    .status("PENDING")
    .total(new BigDecimal("99.99"))
    .build();
```

**WRONG — never construct multi-field types positionally:**

```java
// WRONG — positional constructor is fragile
new CreateOrderRequest(customerId, items);
new OrderResponse(id, status, total);
```

## `val` — Local Variables Are Always Final

Use Lombok's `val` for **every** local variable declaration. `val` is `final` + type-inferred. Never use Java's `var` (which is mutable) or explicit repeated types.

```java
import lombok.val;

// CORRECT — val is final and type-inferred
val order = orderRepository.findById(id)
    .orElseThrow(() -> new OrderNotFoundException(id));
val items = new ArrayList<OrderItem>();
val groupedByStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::status));

// WRONG — var is not final
var order = orderRepository.findById(id); // mutable, avoid

// WRONG — explicit type repeated unnecessarily
OrderResponse order = orderRepository.findById(id);
```

`val` also works in for-each loops:

```java
for (val item : order.items()) {
    process(item);
}
```

For method parameters, declare them `final` explicitly:

```java
public OrderResponse create(final CreateOrderRequest request) {
    val saved = orderRepository.save(toEntity(request));
    return toResponse(saved);
}
```

## Services and Components — `@FieldDefaults`

Combine `@FieldDefaults` with `@RequiredArgsConstructor` to keep service classes clean. All injected fields are implicitly `private final`.

```java
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentService {
    PaymentRepository paymentRepository;
    NotificationService notificationService;
}
```

## JPA Entities — explicit annotations

For JPA entities, be explicit: `@Getter` only (not `@Setter`), `@Builder`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor`. See the Builders section above for the full entity pattern.

Never use `@Data` or `@Setter` on entities — mutations must go through domain methods.

## Prohibited Annotations

| Annotation            | Why Banned                                                            | Use Instead                                                    |
| --------------------- | --------------------------------------------------------------------- | -------------------------------------------------------------- |
| `@Data`               | Generates mutable setters, unsafe `equals`/`hashCode` on JPA entities | `@Value`, record, or explicit `@Getter` + `@EqualsAndHashCode` |
| `@Setter` on entities | Entities should be mutated through domain methods                     | Explicit domain methods                                        |
| `@SneakyThrows`       | Hides checked exceptions from callers                                 | Handle or declare exceptions                                   |
| `@Cleanup`            | Superseded by try-with-resources                                      | `try (val x = ...)`                                            |
| `@UtilityClass`       | Hides intent                                                          | Explicit `private` constructor                                 |
| `@Wither`             | Deprecated                                                            | `@With`                                                        |
| Java `var`            | Not final — use Lombok `val` instead                                  | `val`                                                          |

## `@With` for Immutable Copies

When you need a modified copy of an immutable object (e.g., updating a field on a record-like Lombok class), use `@With`.

```java
@Value
@With
public class Config {
    int timeout;
    String endpoint;
}

val updated = config.withTimeout(30);
```

For records, use the compact constructor pattern or a custom `with` method instead.
