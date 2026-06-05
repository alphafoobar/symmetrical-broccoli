---
name: spring-security-jwt
description: "Spring Security JWT resource-server conventions for REST APIs. Use when configuring authentication, authorization, SecurityFilterChain, JWT claim mapping, method security, role/scope checks, or tests for protected endpoints."
---

# Spring Security JWT

## Defaults

- This API is a stateless OAuth2 resource server. Do not create server-side sessions.
- Use `SecurityFilterChain` beans. Do not extend removed/deprecated adapter classes.
- Enable method security when service-level authorization matters: `@EnableMethodSecurity`.
- Use JWT bearer tokens. Do not implement custom token parsing filters unless Spring Security cannot model the requirement.
- Public endpoints must be explicit and minimal: health/readiness, OpenAPI UI/spec, and authentication-free actuator endpoints only.

## Configuration Pattern

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth
                .requestMatchers("/actuator/health/**", "/openapi/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAuthority("SCOPE_api.read")
                .requestMatchers("/api/v1/**").hasAuthority("SCOPE_api.write")
                .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
  }
}
```

## Claims and Authorities

- Prefer standard `scope` / `scp` claims mapped to `SCOPE_*` authorities.
- If the identity provider uses custom role claims, provide a `JwtAuthenticationConverter`.
- Keep claim mapping in one configuration class and cover it with unit tests.
- Do not check raw JWT claims in controllers. Controllers should depend on Spring Security authorization results.

## Controller and Service Rules

- Use request-level authorization for broad endpoint access.
- Use `@PreAuthorize` on service methods for object-level or business-level authorization.
- Never trust tenant, user, or organization IDs from request bodies if the same value is present in token claims.
- Log authentication failures through Spring Security defaults; do not log raw JWTs or secrets.

## Testing

- Load `test-security` for security tests.
- Use `SecurityMockMvcRequestPostProcessors.jwt()` for MVC slice tests.
- Test unauthenticated, authenticated-but-forbidden, and authorized paths for every protected controller group.
- Prefer explicit authorities in tests, e.g. `jwt().authorities(new SimpleGrantedAuthority("SCOPE_api.read"))`.
