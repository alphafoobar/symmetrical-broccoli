---
name: java-nullsafety
description: "Null safety with JSpecify @NullMarked. Use when creating any new Java class, package, or interface. Covers package-info.java setup, @Nullable usage, Optional patterns, and eliminating raw nulls from APIs. Required for every new package in this project."
---

# Null Safety — JSpecify `@NullMarked`

## Dependency

JSpecify is included via Spring Boot's managed dependencies. No extra dependency declaration needed in Spring Boot 4.

## Package-Level Annotation — `package-info.java`

Every package **must** have a `package-info.java` annotated with `@NullMarked`. This sets non-null as the default for all types in the package.

```java
// src/main/java/com/demo/skills/order/package-info.java
@NullMarked
package com.demo.skills.order;

import org.jspecify.annotations.NullMarked;
```

Create this file for every new sub-package. Never skip it.

## Default: Everything is Non-Null

Within a `@NullMarked` package, all types, parameters, fields, and return values are non-null by default. Annotate only the exceptions.

```java
// Both parameters and return type are non-null by default — no annotation needed
public OrderResponse findOrder(UUID id) { ... }

// Explicit opt-in for nullable
public @Nullable OrderResponse findOrderOrNull(UUID id) { ... }
```

## `@Nullable` — Opt-In for Absent Values

Use `@Nullable` (from `org.jspecify.annotations`) sparingly and deliberately. Prefer `Optional<T>` over `@Nullable` return types on public APIs.

```java
import org.jspecify.annotations.Nullable;

// ACCEPTABLE — internal/private helper that may return null
private @Nullable String extractToken(HttpServletRequest request) {
    return request.getHeader("Authorization");
}

// PREFERRED for public API — make absence explicit via Optional
public Optional<Order> findById(UUID id) {
    return orderRepository.findById(id);
}
```

## `Optional<T>` — Public API Absence

Return `Optional<T>` from any public method that may not produce a value. Never return `null` from a public method.

```java
// CORRECT
public Optional<Order> findById(UUID id) {
    return orderRepository.findById(id);
}

// WRONG — raw null return
public Order findById(UUID id) {
    return orderRepository.findById(id).orElse(null); // never
}
```

## Never Call `Optional.get()`

`Optional.get()` throws `NoSuchElementException` with no context. Always use a terminal with fallback or exception:

```java
// CORRECT
val order = findById(id)
    .orElseThrow(() -> new OrderNotFoundException(id));

val name = findById(id)
    .map(Order::customerId)
    .orElse("unknown");

// WRONG
val order = findById(id).get(); // NoSuchElementException with no context
```

## Constructor Validation

For classes that aren't records (e.g., entities, config objects), validate non-null constructor parameters explicitly with `@NonNull` (Lombok) or `Objects.requireNonNull`.

```java
// Lombok @NonNull generates null check + NullPointerException with field name
@RequiredArgsConstructor
public class OrderService {
    private final @NonNull OrderRepository orderRepository;
}

// Or explicitly for complex validation
public Order(UUID id, String customerId) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
}
```

## Records — Compact Constructor Validation

```java
public record CreateOrderRequest(UUID customerId, List<OrderItem> items) {
    public CreateOrderRequest {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(items, "items");
        if (items.isEmpty()) throw new IllegalArgumentException("items must not be empty");
        items = List.copyOf(items); // defensive copy
    }
}
```

## Null-Safe Collection Handling

```java
// WRONG — NPE if list is null
for (var item : order.items()) { ... }

// CORRECT — validate at construction, rely on @NullMarked
// If the field is List (non-null by @NullMarked), iterate freely.
// Never assign null to a non-null field.
```

## Interop with Un-annotated Libraries

When calling methods from libraries without JSpecify annotations, treat returned values as potentially null and wrap defensively:

```java
// External API without null annotations
val rawValue = legacyClient.getValue(key); // unknown nullability
val value = Optional.ofNullable(rawValue)
    .orElseThrow(() -> new IntegrationException("No value for key: " + key));
```
