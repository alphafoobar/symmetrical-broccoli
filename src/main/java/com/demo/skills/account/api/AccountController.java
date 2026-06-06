package com.demo.skills.account.api;

import com.demo.skills.account.service.AccountService;
import com.demo.skills.api.AccountsApi;
import com.demo.skills.api.model.AccountListResponse;
import com.demo.skills.api.model.AccountResponse;
import com.demo.skills.api.model.CreateAccountRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller implementing the accounts API contract. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccountController implements AccountsApi {

  private final AccountService accountService;

  @Override
  public ResponseEntity<AccountResponse> createAccount(final CreateAccountRequest body) {
    val customerId = currentCustomerId();
    val response = accountService.createAccount(customerId, body);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  public ResponseEntity<AccountResponse> getAccount(final UUID accountId) {
    val customerId = currentCustomerId();
    val response = accountService.getAccount(customerId, accountId);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<AccountListResponse> listAccounts() {
    val customerId = currentCustomerId();
    val response = accountService.listAccounts(customerId);
    return ResponseEntity.ok(response);
  }

  private static String currentCustomerId() {
    val authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken auth)) {
      throw new IllegalStateException("Expected JWT authentication");
    }
    return auth.getToken().getSubject();
  }
}
