---
name: test-architecture
description: "Architecture testing with ArchUnit. Use when enforcing layer boundaries, package dependency rules, naming conventions, Lombok usage, and Spring annotation constraints. Covers @AnalyzeClasses, @ArchTest, layered architecture rules, and custom conditions for this project's conventions."
---

# Architecture Testing — ArchUnit

## Dependency

```kotlin
// build.gradle.kts
testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
```

## Class Structure

One class per concern. Use `@AnalyzeClasses` pointing at the root package. `ImportOption.DoNotIncludeTests` prevents test classes from being analysed as production code.

```java
@AnalyzeClasses(
    packages = "com.demo.skills",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {
    // @ArchTest fields are static and final
}
```

## Layer Dependency Rules

Enforce that layers only call downward — controllers → services → repositories. No layer skipping.

```java
@ArchTest
static final ArchRule layered_architecture =
    layeredArchitecture()
        .consideringAllDependencies()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .layer("Repository").definedBy("..repository..")
        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
        .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");
```

## No Field Injection

```java
@ArchTest
static final ArchRule no_field_injection =
    noFields()
        .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
        .because("Use constructor injection via @RequiredArgsConstructor");
```

## No `System.out` / `System.err`

```java
@ArchTest
static final ArchRule no_system_out =
    noClasses()
        .should().callMethod(System.class, "out")
        .orShould().callMethod(System.class, "err")
        .because("Use @Slf4j and log.* instead of System.out.println");
```

## Controllers Must Be in a `controller` Package

```java
@ArchTest
static final ArchRule controllers_reside_in_controller_package =
    classes()
        .that().areAnnotatedWith(RestController.class)
        .should().resideInAPackage("..controller..")
        .because("All @RestController classes must be in a controller sub-package");
```

## Services Must Be in a `service` Package

```java
@ArchTest
static final ArchRule services_reside_in_service_package =
    classes()
        .that().areAnnotatedWith(Service.class)
        .should().resideInAPackage("..service..")
        .because("All @Service classes must be in a service sub-package");
```

## Naming Conventions

```java
@ArchTest
static final ArchRule controllers_named_correctly =
    classes()
        .that().areAnnotatedWith(RestController.class)
        .should().haveSimpleNameEndingWith("Controller");

@ArchTest
static final ArchRule services_named_correctly =
    classes()
        .that().areAnnotatedWith(Service.class)
        .should().haveSimpleNameEndingWith("Service");

@ArchTest
static final ArchRule repositories_named_correctly =
    classes()
        .that().areAnnotatedWith(Repository.class)
        .or().areAssignableTo(JpaRepository.class)
        .should().haveSimpleNameEndingWith("Repository");
```

## No Raw `null` Returns from Public Methods

```java
@ArchTest
static final ArchRule no_null_returns =
    noMethods()
        .that().arePublic()
        .and().areDeclaredInClassesThat().resideInAPackage("com.demo.skills..")
        .should(returnNull())
        .because("Return Optional<T> or throw instead of returning null");

// Custom condition
static ArchCondition<JavaMethod> returnNull() {
    return new ArchCondition<>("return null") {
        @Override
        public void check(JavaMethod method, ConditionEvents events) {
            method.getMethodCallsFromSelf().stream()
                .filter(call -> call.getTarget().getName().equals("returnNull"))
                .forEach(call -> events.add(SimpleConditionEvent.violated(method,
                    method.getFullName() + " returns null")));
        }
    };
}
```

## Entities Must Not Be Accessed by Controllers

```java
@ArchTest
static final ArchRule controllers_must_not_use_entities =
    noClasses()
        .that().areAnnotatedWith(RestController.class)
        .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
        .because("Controllers must work with DTOs/records, not JPA entities directly");
```

## `@Transactional` Belongs on Services, Not Controllers

```java
@ArchTest
static final ArchRule transactional_not_on_controllers =
    noClasses()
        .that().areAnnotatedWith(RestController.class)
        .should().beAnnotatedWith(Transactional.class)
        .as("@Transactional belongs on service methods, not controllers");
```

## Organising Multiple Rule Files

Split rules by concern into separate `@AnalyzeClasses` classes when they grow large:

```
src/test/java/com/demo/skills/
└── architecture/
    ├── LayerRulesTest.java        ← dependency direction
    ├── NamingRulesTest.java       ← naming conventions
    ├── SpringRulesTest.java       ← annotation rules (@Autowired, @Transactional)
    └── CodingRulesTest.java       ← null returns, System.out, etc.
```

## Running Arch Tests in CI

ArchUnit tests are plain JUnit 5 tests — they run automatically with `./gradlew test`. No separate configuration needed. Failed rules print a clear violation report:

```
Architecture Violation [Priority: MEDIUM] - Rule 'no field injection' was violated (1 times):
Field <com.demo.skills.order.OrderController.orderService> is annotated with @Autowired
```
