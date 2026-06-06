package com.demo.skills.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Spring Security configuration: stateless JWT resource server. */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /** Scope required for customers to manage their own accounts. */
  public static final String CUSTOMER_ACCOUNT_SCOPE = "customer.account.my";

  private static final String CUSTOMER_ACCOUNT_AUTHORITY = "SCOPE_" + CUSTOMER_ACCOUNT_SCOPE;

  /** Configures the security filter chain. */
  @Bean
  SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/health")
                    .permitAll()
                    .requestMatchers("/actuator/health/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/accounts", "/api/v1/accounts/**")
                    .hasAuthority(CUSTOMER_ACCOUNT_AUTHORITY)
                    .requestMatchers(HttpMethod.POST, "/api/v1/accounts")
                    .hasAuthority(CUSTOMER_ACCOUNT_AUTHORITY)
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
  }
}
