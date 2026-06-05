---
name: test-security
description: "Spring Security testing. Use when testing authentication requirements, role-based access, method security, or JWT token validation. Covers @WithMockUser, @WithAnonymousUser, SecurityMockMvcRequestPostProcessors, custom JWT setup for @WebMvcTest and @SpringBootTest."
---

# Security Testing Conventions

## Dependencies

Included in `spring-boot-starter-security-test` (pulled by `spring-boot-starter-test`):

- `spring-security-test`
- `@WithMockUser`, `@WithAnonymousUser`, `SecurityMockMvcRequestPostProcessors`

## Unauthenticated Access — Baseline

Always test that protected endpoints return `401` when no credentials are provided. This test requires no setup.

```java
@WebMvcTest(OrderController.class)
class OrderControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("returns 401 when no authentication provided")
    void shouldReturn401WithNoAuth() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }
}
```

## `@WithMockUser` — Role-Based Tests

`@WithMockUser` injects a mock `SecurityContext` without needing a real token. Use it for testing that business logic executes correctly when the user is authenticated.

```java
@Test
@WithMockUser(username = "alice", roles = "USER")
@DisplayName("returns 200 when authenticated as USER")
void shouldReturn200ForAuthenticatedUser() throws Exception {
    val id = UUID.randomUUID();
    given(orderService.findById(id)).willReturn(Optional.of(anOrderResponse(id)));

    mockMvc.perform(get("/api/v1/orders/{id}", id))
        .andExpect(status().isOk());
}

@Test
@WithMockUser(roles = "ADMIN")
@DisplayName("DELETE returns 204 for ADMIN")
void shouldReturn204ForAdmin() throws Exception {
    mockMvc.perform(delete("/api/v1/orders/{id}", UUID.randomUUID()))
        .andExpect(status().isNoContent());
}

@Test
@WithMockUser(roles = "USER")
@DisplayName("DELETE returns 403 for USER")
void shouldReturn403ForInsufficientRole() throws Exception {
    mockMvc.perform(delete("/api/v1/orders/{id}", UUID.randomUUID()))
        .andExpect(status().isForbidden());
}
```

## `@WithAnonymousUser` — Explicit Unauthenticated

```java
@Test
@WithAnonymousUser
@DisplayName("returns 401 for anonymous user explicitly")
void shouldReturn401ForAnonymous() throws Exception {
    mockMvc.perform(get("/api/v1/orders"))
        .andExpect(status().isUnauthorized());
}
```

## JWT Bearer Token — `SecurityMockMvcRequestPostProcessors`

For JWT-secured endpoints, build a mock JWT with `jwt()` post-processor from `SecurityMockMvcRequestPostProcessors`:

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@Test
@DisplayName("returns 200 with valid JWT bearing USER scope")
void shouldReturn200WithJwt() throws Exception {
    val id = UUID.randomUUID();
    given(orderService.findById(id)).willReturn(Optional.of(anOrderResponse(id)));

    mockMvc.perform(get("/api/v1/orders/{id}", id)
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isOk());
}

@Test
@DisplayName("returns 403 when JWT lacks required scope")
void shouldReturn403WithInsufficientScope() throws Exception {
    mockMvc.perform(delete("/api/v1/orders/{id}", UUID.randomUUID())
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isForbidden());
}
```

## JWT with Custom Claims

```java
mockMvc.perform(get("/api/v1/orders")
    .with(jwt()
        .jwt(token -> token
            .subject("user-123")
            .claim("email", "user@example.com")
            .claim("tenant_id", "tenant-abc"))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
    .andExpect(status().isOk());
```

## Method Security — `@PreAuthorize` Testing

Test `@PreAuthorize` by enabling method security in the test slice. Spring Boot 4's `@WebMvcTest` does not load `@Service` beans, so method security on controllers is exercised naturally. For services annotated with `@PreAuthorize`, write a dedicated unit or integration test:

```java
@SpringBootTest
@Testcontainers
class OrderServiceSecurityIT extends IntegrationTest {

    @Autowired
    private OrderService orderService;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("admin-only operation throws AccessDeniedException for USER role")
    void shouldDenyAdminOperationForUser() {
        assertThatThrownBy(() -> orderService.forceClose(UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("admin-only operation succeeds for ADMIN role")
    void shouldAllowAdminOperationForAdmin() {
        assertThatNoException().isThrownBy(() -> orderService.forceClose(UUID.randomUUID()));
    }
}
```

## Security Test Checklist per Controller

For every `@RestController`, the security test class should cover:

| Scenario                             | Expected Status    |
| ------------------------------------ | ------------------ |
| No credentials                       | `401 Unauthorized` |
| Valid credentials, insufficient role | `403 Forbidden`    |
| Valid credentials, correct role      | `2xx`              |
| Expired / malformed token            | `401 Unauthorized` |

## What NOT to Do

- No disabling security entirely in `@WebMvcTest` with `excludeAutoConfiguration` just to avoid writing auth tests — security tests are mandatory
- No sharing mutable `SecurityContext` state between tests — let annotations set it per method
- No testing Spring Security internals (filter chain, token parsing) — test the HTTP contract only
