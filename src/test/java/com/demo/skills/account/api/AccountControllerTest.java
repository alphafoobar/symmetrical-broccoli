package com.demo.skills.account.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.demo.skills.account.service.AccountService;
import com.demo.skills.api.model.AccountListResponse;
import com.demo.skills.api.model.AccountResponse;
import com.demo.skills.api.model.AccountStatus;
import com.demo.skills.exception.AccountLimitExceededException;
import com.demo.skills.exception.AccountNotFoundException;
import com.demo.skills.exception.NicknameNotAllowedException;
import com.demo.skills.security.SecurityConfig;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

  private static final String CUSTOMER_ID = "1234567";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private AccountService accountService;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @Nested
  @DisplayName("POST /api/v1/accounts")
  class CreateAccount {

    @Test
    @DisplayName("returns 201 with account body when valid request")
    void returns201OnValidRequest() throws Exception {
      // given
      val response = anAccountResponse(UUID.randomUUID());
      given(accountService.createAccount(eq(CUSTOMER_ID), any())).willReturn(response);

      // when / then
      mockMvc
          .perform(
              post("/api/v1/accounts")
                  .with(accountJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of("customerName", "Alice Smith"))))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.accountNumber").value("03 1509 0000001 00"))
          .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID))
          .andExpect(jsonPath("$.nickname").doesNotExist());
    }

    @Test
    @DisplayName("returns 400 when customerName is blank")
    void returns400WhenCustomerNameBlank() throws Exception {
      mockMvc
          .perform(
              post("/api/v1/accounts")
                  .with(accountJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("customerName", ""))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @DisplayName("returns 400 when customerName is whitespace only")
    void returns400WhenCustomerNameWhitespaceOnly() throws Exception {
      mockMvc
          .perform(
              post("/api/v1/accounts")
                  .with(accountJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("customerName", "   "))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @DisplayName("returns 400 when customerName contains repeated spaces")
    void returns400WhenCustomerNameContainsRepeatedSpaces() throws Exception {
      mockMvc
          .perform(
              post("/api/v1/accounts")
                  .with(accountJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of("customerName", "Alice  Smith"))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @DisplayName("returns 400 when nickname is too short (< 5 chars)")
    void returns400WhenNicknameTooShort() throws Exception {
      mockMvc
          .perform(
              post("/api/v1/accounts")
                  .with(accountJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of("customerName", "Alice Smith", "nickname", "ab"))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @DisplayName("returns 400 when provided nickname is whitespace only")
    void returns400WhenNicknameWhitespaceOnly() throws Exception {
      mockMvc
          .perform(
              post("/api/v1/accounts")
                  .with(accountJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of("customerName", "Alice Smith", "nickname", "     "))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @DisplayName("returns 400 when provided nickname contains repeated spaces")
    void returns400WhenNicknameContainsRepeatedSpaces() throws Exception {
      mockMvc
          .perform(
              post("/api/v1/accounts")
                  .with(accountJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of("customerName", "Alice Smith", "nickname", "My  Savings"))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @DisplayName("returns 422 when customer has reached 5-account limit")
    void returns422WhenAccountLimitExceeded() throws Exception {
      // given
      willThrow(new AccountLimitExceededException(CUSTOMER_ID))
          .given(accountService)
          .createAccount(eq(CUSTOMER_ID), any());

      // when / then
      mockMvc
          .perform(
              post("/api/v1/accounts")
                  .with(accountJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(Map.of("customerName", "Alice Smith"))))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.type").value("https://errors.demo.com/account-limit-exceeded"))
          .andExpect(jsonPath("$.detail").value("Maximum account limit reached"))
          .andExpect(jsonPath("$.customerId").doesNotExist())
          .andExpect(
              result ->
                  assertThat(result.getResponse().getContentAsString())
                      .doesNotContain(CUSTOMER_ID));
    }

    @Test
    @DisplayName("returns 422 when nickname is on profanity blocklist")
    void returns422WhenNicknameNotAllowed() throws Exception {
      // given
      willThrow(new NicknameNotAllowedException())
          .given(accountService)
          .createAccount(eq(CUSTOMER_ID), any());

      // when / then
      mockMvc
          .perform(
              post("/api/v1/accounts")
                  .with(accountJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of("customerName", "Alice", "nickname", "badword"))))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.type").value("https://errors.demo.com/nickname-not-allowed"));
    }

    @Test
    @DisplayName("returns 503 when database is unavailable")
    void returns503WhenDatabaseUnavailable() throws Exception {
      // given
      willThrow(new DataAccessResourceFailureException("database unavailable"))
          .given(accountService)
          .createAccount(eq(CUSTOMER_ID), any());

      // when / then
      mockMvc
          .perform(
              post("/api/v1/accounts")
                  .with(accountJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(Map.of("customerName", "Alice Smith"))))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.type").value("https://errors.demo.com/database-unavailable"))
          .andExpect(jsonPath("$.title").value("Service Unavailable"));
    }

    @Test
    @DisplayName("returns 500 when an unexpected error occurs")
    void returns500WhenUnexpectedErrorOccurs() throws Exception {
      // given
      willThrow(new IllegalStateException("unexpected"))
          .given(accountService)
          .createAccount(eq(CUSTOMER_ID), any());

      // when / then
      mockMvc
          .perform(
              post("/api/v1/accounts")
                  .with(accountJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(Map.of("customerName", "Alice Smith"))))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.type").value("https://errors.demo.com/internal-server-error"))
          .andExpect(jsonPath("$.title").value("Internal Server Error"));
    }

    @Test
    @DisplayName("returns 401 when not authenticated")
    void returns401WhenNotAuthenticated() throws Exception {
      mockMvc
          .perform(
              post("/api/v1/accounts")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("customerName", "Alice"))))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("returns 403 when JWT lacks customer account scope")
    void returns403WhenScopeMissing() throws Exception {
      mockMvc
          .perform(
              post("/api/v1/accounts")
                  .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(CUSTOMER_ID)))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("customerName", "Alice"))))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/accounts/{accountId}")
  class GetAccount {

    @Test
    @DisplayName("returns 200 with account body when found")
    void returns200WhenFound() throws Exception {
      // given
      val accountId = UUID.randomUUID();
      val response = anAccountResponse(accountId);
      given(accountService.getAccount(CUSTOMER_ID, accountId)).willReturn(response);

      // when / then
      mockMvc
          .perform(
              get("/api/v1/accounts/{accountId}", accountId)
                  .with(accountJwt()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accountId").value(accountId.toString()))
          .andExpect(jsonPath("$.nickname").doesNotExist());
    }

    @Test
    @DisplayName("returns 404 when account not found")
    void returns404WhenNotFound() throws Exception {
      // given
      val accountId = UUID.randomUUID();
      given(accountService.getAccount(CUSTOMER_ID, accountId))
          .willThrow(new AccountNotFoundException(accountId));

      // when / then
      mockMvc
          .perform(
              get("/api/v1/accounts/{accountId}", accountId)
                  .with(accountJwt()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.type").value("https://errors.demo.com/account-not-found"));
    }

    @Test
    @DisplayName("returns 401 when not authenticated")
    void returns401WhenNotAuthenticated() throws Exception {
      mockMvc
          .perform(get("/api/v1/accounts/{accountId}", UUID.randomUUID()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("returns 403 when JWT lacks customer account scope")
    void returns403WhenScopeMissing() throws Exception {
      mockMvc
          .perform(
              get("/api/v1/accounts/{accountId}", UUID.randomUUID())
                  .with(jwtWithoutAccountScope()))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/accounts")
  class ListAccounts {

    @Test
    @DisplayName("returns 200 with accounts list")
    void returns200WithAccountsList() throws Exception {
      // given
      val listResponse = new AccountListResponse();
      listResponse.setAccounts(List.of(anAccountResponse(UUID.randomUUID())));
      given(accountService.listAccounts(CUSTOMER_ID)).willReturn(listResponse);

      // when / then
      mockMvc
          .perform(
              get("/api/v1/accounts").with(accountJwt()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accounts").isArray())
          .andExpect(jsonPath("$.accounts.length()").value(1))
          .andExpect(jsonPath("$.accounts[0].nickname").doesNotExist());
    }

    @Test
    @DisplayName("returns 401 when not authenticated")
    void returns401WhenNotAuthenticated() throws Exception {
      mockMvc.perform(get("/api/v1/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("returns 403 when JWT lacks customer account scope")
    void returns403WhenScopeMissing() throws Exception {
      mockMvc
          .perform(get("/api/v1/accounts").with(jwtWithoutAccountScope()))
          .andExpect(status().isForbidden());
    }
  }

  // --- helpers ---

  private static AccountResponse anAccountResponse(final UUID accountId) {
    val response = new AccountResponse();
    response.setAccountId(accountId);
    response.setAccountNumber("03 1509 0000001 00");
    response.setCustomerId(CUSTOMER_ID);
    response.setCustomerName("Alice Smith");
    response.setStatus(AccountStatus.ACTIVE);
    response.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return response;
  }

  private static RequestPostProcessor accountJwt() {
    return SecurityMockMvcRequestPostProcessors.jwt()
        .jwt(j -> j.subject(CUSTOMER_ID))
        .authorities(
            new SimpleGrantedAuthority("SCOPE_" + SecurityConfig.CUSTOMER_ACCOUNT_SCOPE));
  }

  private static RequestPostProcessor jwtWithoutAccountScope() {
    return SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(CUSTOMER_ID));
  }
}
