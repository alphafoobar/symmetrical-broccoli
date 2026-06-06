package com.demo.skills.account.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.demo.skills.api.model.AccountListResponse;
import com.demo.skills.api.model.AccountResponse;
import com.demo.skills.api.model.AccountStatus;
import com.demo.skills.api.model.CreateAccountRequest;
import com.demo.skills.security.SecurityConfig;
import java.time.Instant;
import java.util.Map;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class AccountApiIntegrationTest {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

  @Container
  @ServiceConnection(name = "redis")
  @SuppressWarnings("resource")
  static final GenericContainer<?> redis =
      new GenericContainer<>("redis:7").withExposedPorts(6379);

  @Autowired
  private TestRestTemplate restTemplate;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  private static final String TEST_TOKEN = "test-token";

  /** Use a unique customer ID per test to avoid hitting the 5-account limit. */
  private String customerId;

  @BeforeEach
  void setupJwtDecoder() {
    // Unique 7-digit ID per test so tests are fully isolated without DB cleanup
    customerId = String.valueOf(1000000 + (int) (Math.random() * 8999999));

    given(jwtDecoder.decode(anyString())).willReturn(jwtFor(TEST_TOKEN, customerId));
  }

  @Test
  @DisplayName("POST /api/v1/accounts creates account and GET /api/v1/accounts/{id} retrieves it")
  void createAndRetrieveAccount() {
    // given
    val createRequest = new CreateAccountRequest();
    createRequest.setCustomerName("Alice Smith");
    createRequest.setNickName("MySavings");

    // when — create
    val createResponse =
        restTemplate.exchange(
            "/api/v1/accounts",
            HttpMethod.POST,
            new HttpEntity<>(createRequest, bearerHeaders()),
            AccountResponse.class);

    // then
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    val created = createResponse.getBody();
    assertThat(created).isNotNull();
    assertThat(created.getCustomerName()).isEqualTo("Alice Smith");
    assertThat(created.getNickName()).isEqualTo("MySavings");
    assertThat(created.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(created.getAccountNumber()).startsWith("03 1509");
    assertThat(created.getAccountNumber()).endsWith("00");

    // when — retrieve by id
    val getResponse =
        restTemplate.exchange(
            "/api/v1/accounts/{accountId}",
            HttpMethod.GET,
            new HttpEntity<>(bearerHeaders()),
            AccountResponse.class,
            created.getAccountId());

    // then
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody()).isNotNull();
    assertThat(getResponse.getBody().getAccountId()).isEqualTo(created.getAccountId());
  }

  @Test
  @DisplayName("GET /api/v1/accounts/{id} returns 404 for another customer's account")
  void returns404ForAnotherCustomersAccount() {
    // given
    val ownerToken = "owner-token";
    val otherToken = "other-token";
    val ownerCustomerId = "%s-owner".formatted(customerId);
    val otherCustomerId = "%s-other".formatted(customerId);
    given(jwtDecoder.decode(ownerToken)).willReturn(jwtFor(ownerToken, ownerCustomerId));
    given(jwtDecoder.decode(otherToken)).willReturn(jwtFor(otherToken, otherCustomerId));

    val createRequest = new CreateAccountRequest();
    createRequest.setCustomerName("Alice Smith");
    val createResponse =
        restTemplate.exchange(
            "/api/v1/accounts",
            HttpMethod.POST,
            new HttpEntity<>(createRequest, bearerHeaders(ownerToken)),
            AccountResponse.class);
    val created = createResponse.getBody();
    assertThat(created).isNotNull();

    // when
    val getResponse =
        restTemplate.exchange(
            "/api/v1/accounts/{accountId}",
            HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(otherToken)),
            Map.class,
            created.getAccountId());

    // then
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/v1/accounts lists all accounts for authenticated customer")
  void listAccountsForCustomer() {
    // given — create an account first
    val createRequest = new CreateAccountRequest();
    createRequest.setCustomerName("Bob Jones");
    restTemplate.exchange(
        "/api/v1/accounts",
        HttpMethod.POST,
        new HttpEntity<>(createRequest, bearerHeaders()),
        AccountResponse.class);

    // when
    val listResponse =
        restTemplate.exchange(
            "/api/v1/accounts",
            HttpMethod.GET,
            new HttpEntity<>(bearerHeaders()),
            AccountListResponse.class);

    // then
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponse.getBody()).isNotNull();
    assertThat(listResponse.getBody().getAccounts()).isNotEmpty();
  }

  @Test
  @DisplayName("POST /api/v1/accounts returns 422 when nickname is on profanity blocklist")
  void returns422WhenNicknameBlocked() {
    // given
    val createRequest = new CreateAccountRequest();
    createRequest.setCustomerName("Charlie Brown");
    createRequest.setNickName("bullshit");

    // when
    val response =
        restTemplate.exchange(
            "/api/v1/accounts",
            HttpMethod.POST,
            new HttpEntity<>(createRequest, bearerHeaders()),
            Map.class);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(422);
  }

  @Test
  @DisplayName("GET /api/v1/health returns 200 without authentication")
  void healthEndpointIsPublic() {
    val response = restTemplate.getForEntity("/api/v1/health", Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("POST /api/v1/accounts returns 401 without a bearer token")
  void returns401WithoutToken() {
    val createRequest = new CreateAccountRequest();
    createRequest.setCustomerName("Dave");
    val response =
        restTemplate.postForEntity("/api/v1/accounts", createRequest, Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("POST /api/v1/accounts returns 403 when JWT lacks customer account scope")
  void returns403WithoutScope() {
    // given
    val token = "missing-scope-token";
    given(jwtDecoder.decode(token)).willReturn(jwtWithoutScope(customerId));
    val createRequest = new CreateAccountRequest();
    createRequest.setCustomerName("Eve");

    // when
    val response =
        restTemplate.exchange(
            "/api/v1/accounts",
            HttpMethod.POST,
            new HttpEntity<>(createRequest, bearerHeaders(token)),
            Map.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  private static HttpHeaders bearerHeaders() {
    return bearerHeaders(TEST_TOKEN);
  }

  private static HttpHeaders bearerHeaders(final String token) {
    val headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private static Jwt jwtFor(final String token, final String subject) {
    return Jwt.withTokenValue(token)
        .header("alg", "none")
        .subject(subject)
        .claim("scope", SecurityConfig.CUSTOMER_ACCOUNT_SCOPE)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }

  private static Jwt jwtWithoutScope(final String subject) {
    return Jwt.withTokenValue("missing-scope-token")
        .header("alg", "none")
        .subject(subject)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }
}
