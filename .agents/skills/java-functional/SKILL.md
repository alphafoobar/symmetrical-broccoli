---
name: java-functional
description: "Functional programming style with Java streams, Optional, and immutability. Use when writing any logic involving collections, data transformation, conditional logic, or absent values. Covers Stream API, Optional chaining, method references, immutable data, and sealed result types."
---

# Functional Programming Conventions

## Streams — Prefer Over Imperative Loops

Replace `for` loops with Stream pipelines wherever the intent is a transformation, filter, or aggregation.

```java
// CORRECT — declarative pipeline
val activeOrderIds = orders.stream()
    .filter(o -> o.status().equals("ACTIVE"))
    .map(Order::id)
    .toList();

// WRONG — imperative
val activeOrderIds = new ArrayList<UUID>();
for (val order : orders) {
    if (order.status().equals("ACTIVE")) {
        activeOrderIds.add(order.id());
    }
}
```

## `toList()` — Not `Collectors.toList()`

`Stream.toList()` (Java 16+) returns an unmodifiable list. Always prefer it.

```java
// CORRECT
val names = stream.map(User::name).toList();

// WRONG
val names = stream.map(User::name).collect(Collectors.toList());
```

## Method References Over Lambdas

Prefer method references when the lambda does nothing but delegate.

```java
// CORRECT
orders.stream().map(Order::customerId).toList();
orders.forEach(this::process);
orders.stream().filter(Order::isActive).toList();

// AVOID — unnecessary lambda wrapping
orders.stream().map(o -> o.customerId()).toList();
orders.forEach(o -> this.process(o));
```

## `Optional` — Chaining, Never `get()`

Never call `Optional.get()`. Chain with `map`, `flatMap`, `filter`, then terminate with `orElse`, `orElseGet`, or `orElseThrow`.

```java
// CORRECT — full pipeline
val label = findOrderById(id)
    .filter(o -> o.status().equals("ACTIVE"))
    .map(Order::customerId)
    .flatMap(userService::findById)
    .map(User::displayName)
    .orElse("Unknown customer");

// CORRECT — throw with context
val order = findOrderById(id)
    .orElseThrow(() -> new OrderNotFoundException(id));

// WRONG
val order = findOrderById(id).get(); // NoSuchElementException with no context
val order = findOrderById(id).orElse(null); // defeats the purpose
```

## Avoid `Optional` in Fields and Collections

`Optional` is a return-type tool, not a field type. Never store `Optional` in a collection or as a field.

```java
// WRONG
private Optional<String> cachedToken; // use @Nullable or a sentinel value
List<Optional<Order>> results;        // use filter to remove absent values instead

// CORRECT
val presentOrders = maybeOrders.stream()
    .flatMap(Optional::stream)
    .toList();
```

## Collectors — Aggregation Idioms

```java
// Group by field
val byStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::status));

// Partition into two groups
val partitioned = orders.stream()
    .collect(Collectors.partitioningBy(o -> o.total().compareTo(new BigDecimal("100")) > 0));

// Counting per group
val countByStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::status, Collectors.counting()));

// Joining strings
val ids = orders.stream()
    .map(o -> o.id().toString())
    .collect(Collectors.joining(", ", "[", "]"));

// teeing — two collectors, one merge function (Java 12+)
val stats = orders.stream().collect(Collectors.teeing(
    Collectors.counting(),
    Collectors.summingDouble(o -> o.total().doubleValue()),
    (count, sum) -> new OrderStats(count, BigDecimal.valueOf(sum))
));
```

## Immutable Collections

Always construct collections with factory methods. Never expose mutable internal state.

```java
// CORRECT — immutable at creation
val roles = Set.of("ADMIN", "USER");
val config = Map.of("timeout", 30, "retries", 3);
val items = List.of(a, b, c);

// Defensive copy on input
public record OrderGroup(List<Order> orders) {
    public OrderGroup {
        orders = List.copyOf(orders); // immutable snapshot
    }
}

// Unmodifiable view of a mutable internal list
public List<Order> getOrders() {
    return Collections.unmodifiableList(internalList);
}
```

## `Predicate` Composition

Build complex filters by composing `Predicate` instances rather than writing deeply nested conditions.

```java
Predicate<Order> isActive   = o -> o.status().equals("ACTIVE");
Predicate<Order> isHighValue = o -> o.total().compareTo(new BigDecimal("500")) > 0;
Predicate<Order> isVip      = isActive.and(isHighValue);

val vipOrders = orders.stream().filter(isVip).toList();
```

## `flatMap` for Nested Structures

```java
// Flatten nested collections
val allItems = orders.stream()
    .flatMap(o -> o.items().stream())
    .toList();

// Flatten Optional stream (Java 9+)
val loadedOrders = ids.stream()
    .map(orderRepository::findById)
    .flatMap(Optional::stream)
    .toList();
```

## Anti-Patterns

| Pattern                                   | Problem                         | Fix                                          |
| ----------------------------------------- | ------------------------------- | -------------------------------------------- |
| `stream().filter(...).findFirst().get()`  | Unsafe unwrap                   | Use `orElseThrow()`                          |
| Side effects inside `map()`               | Breaks referential transparency | Use `forEach` or move side effects out       |
| Mutating external state in stream         | Race condition, hard to test    | Return new values; accumulate with `collect` |
| `Optional.isPresent()` + `Optional.get()` | Verbose null check reborn       | Use `map`/`orElseThrow` chain                |
| `new ArrayList<>()` as `collect` target   | Mutable result leaks            | Use `toList()` or `List.copyOf(...)`         |

## Declare and Initialise on One Line

Always declare and initialise a variable in a single statement. Never split declaration and assignment in order to wrap the initialisation in a `try-catch`.

If the initialisation can throw, extract it into a private method that returns the value (or an `Optional` / sealed result), then assign the return value inline.

```java
// WRONG — split declaration + try-catch assignment
val result;
try {
    result = riskyOperation();
} catch (SomeException e) {
    result = "fallback";
}

// CORRECT — extract to a method and assign inline
val result = tryRiskyOperation();

// CORRECT — or use a functional wrapper
val result = tryOrElse(this::riskyOperation, "fallback");

private String tryRiskyOperation() {
    try {
        return riskyOperation();
    } catch (SomeException ex) {
        log.atWarn()
            .setCause(ex)
            .addKeyValue("fallback", "defaultResult")
            .log("Operation failed, using fallback");
    }
    return "fallback";
}
```

The rule applies equally in tests:

```java
// WRONG
val id;
try {
    id = UUID.fromString(raw);
} catch (IllegalArgumentException e) {
    id = UUID.randomUUID();
}

// CORRECT
val id = parseOrRandom(raw);
```

## Early Return — Drop Unnecessary `else` After `return`

When a branch ends with `return`, the subsequent `else` is redundant. Prefer a trailing return statement over symmetrical `if/else` returns. This reduces nesting and makes the exit path visible.

```java
// WRONG — else is unnecessary after return
int classify(int n) {
    if (n % 2 == 0) {
        return 2;
    } else {
        return 1;
    }
}

// CORRECT — trailing return, no else
int classify(int n) {
    if (n % 2 == 0) {
        return 2;
    }
    return 1;
}
```

The same applies to guard clauses — validate/reject early, then return the happy path at the end:

```java
// CORRECT — guards at the top, single success return at the bottom
OrderResponse process(final CreateOrderRequest request) {
    if (request.items().isEmpty()) {
        throw new InvalidOrderException("Order must have at least one item");
    }
    if (!inventoryService.allAvailable(request.items())) {
        throw new InsufficientInventoryException();
    }
    val saved = orderRepository.save(toEntity(request));
    return toResponse(saved);
}
```
