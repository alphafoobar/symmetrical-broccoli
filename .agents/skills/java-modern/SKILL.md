---
name: java-modern
description: "Modern Java 25 language features. Use when writing any new Java code: records, sealed interfaces, pattern matching switch, text blocks, Lombok val, unnamed patterns, sequenced collections, and virtual threads. Replaces old-style instanceof casts, verbose DTOs, and imperative switch statements."
---

# Modern Java 25 Conventions

## Records — Default for Data Carriers

All DTOs, request/response bodies, events, and value objects are records. Never create a class with only getters for data transfer.

```java
// CORRECT
public record CreateOrderRequest(String customerId, List<OrderItem> items) {}
public record OrderResponse(UUID id, String status, BigDecimal total, Instant createdAt) {}

// WRONG
public class OrderResponse {
    private final UUID id;
    // ...getters, constructor, equals, hashCode
}
```

Records with validation use compact constructors:

```java
public record Money(BigDecimal amount, String currency) {
    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
    }
}
```

## Sealed Interfaces — Domain Modelling

Use sealed interfaces to represent closed sets of domain variants. Combine with pattern matching for exhaustive handling.

```java
public sealed interface PaymentResult
    permits PaymentResult.Success, PaymentResult.Failure, PaymentResult.Pending {

    record Success(UUID transactionId, Instant processedAt) implements PaymentResult {}
    record Failure(String reason, String errorCode) implements PaymentResult {}
    record Pending(UUID trackingId) implements PaymentResult {}
}
```

## Pattern Matching — Switch Expressions

Always use switch **expressions** (not statements) with pattern matching. The compiler enforces exhaustiveness on sealed types.

```java
// CORRECT — exhaustive switch expression
String message = switch (result) {
    case PaymentResult.Success s -> "Paid: " + s.transactionId();
    case PaymentResult.Failure f -> "Failed: " + f.reason();
    case PaymentResult.Pending p -> "Pending: " + p.trackingId();
};

// CORRECT — guarded patterns
BigDecimal fee = switch (order) {
    case Order o when o.total().compareTo(new BigDecimal("100")) > 0 -> o.total().multiply(new BigDecimal("0.02"));
    case Order o -> BigDecimal.ZERO;
};

// WRONG — old-style switch statement with fall-through risk
switch (result) {
    case "SUCCESS":
        // ...
        break;
}
```

## Pattern Matching for `instanceof`

Never cast manually after `instanceof`. Always use binding variables.

```java
// CORRECT
if (event instanceof OrderPlacedEvent e) {
    process(e.orderId());
}

// WRONG
if (event instanceof OrderPlacedEvent) {
    process(((OrderPlacedEvent) event).getOrderId());
}
```

## Text Blocks — Multiline Strings

Use text blocks for SQL, JSON, HTML, or any multiline string literal. The closing `"""` controls indentation stripping.

```java
// CORRECT
String sql = """
        SELECT o.id, o.status, o.total
        FROM orders o
        WHERE o.customer_id = :customerId
          AND o.status = 'ACTIVE'
        ORDER BY o.created_at DESC
        """;

String json = """
        {
          "event": "ORDER_PLACED",
          "orderId": "%s"
        }
        """.formatted(orderId);
```

## `val` — All Local Variables Are Final

Use Lombok's `val` for every local variable. Never use Java's `var` (not final) or repeat the type explicitly.

```java
import lombok.val;

// CORRECT — val is final + type-inferred
val orders = new ArrayList<Order>();
val response = orderRepository.findById(id);

// CORRECT — avoids long generic type repetition
val groupedByStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::status));

// WRONG — var is not final
var x = service.process(request);
```

See the `java-lombok` skill for full `val` rules including for-each and parameter `final` conventions.

## Unnamed Patterns & Variables

Use `_` for unused pattern variables and lambda parameters (Java 22+).

```java
// Unused exception variable
try {
    return parseId(raw);
} catch (NumberFormatException _) {
    throw new BadRequestException("Invalid id: " + raw);
}

// Unused lambda parameter
list.forEach(_ -> counter.increment());

// Deconstruction with unnamed component
if (result instanceof PaymentResult.Success(val id, _)) {
    audit(id);
}
```

## Collections — Modern APIs

```java
// Immutable creation
val ids = List.of(1, 2, 3);
val config = Map.of("timeout", 30, "retries", 3);
val roles = Set.of("ADMIN", "USER");

// Sequenced collections (Java 21+) — prefer over index gymnastics
val first = orders.getFirst();
val last = orders.getLast();
orders.reversed().forEach(this::process);

// Copy with modification
val updated = new ArrayList<>(existing);
updated.add(newItem);
val immutable = List.copyOf(updated);

// Stream.toList() — NOT Collectors.toList()
val names = orders.stream().map(Order::customerId).toList();
```

## `String.formatted()` over `String.format()`

```java
// CORRECT
val msg = "Order %s is in status %s".formatted(id, status);

// AVOID
String msg = String.format("Order %s is in status %s", id, status);
```

## Virtual Threads

For I/O-bound work (HTTP calls, DB queries outside of Spring Data), prefer virtual threads. In Spring Boot 4, virtual threads are enabled globally via:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

For explicit executor creation in tests or utilities:

```java
try (val executor = Executors.newVirtualThreadPerTaskExecutor()) {
    val future = executor.submit(() -> callExternalService(id));
    return future.get();
}
```
