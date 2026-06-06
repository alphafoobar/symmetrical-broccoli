package com.demo.skills.account.service;

import com.demo.skills.api.model.AccountListResponse;
import com.demo.skills.api.model.AccountResponse;
import com.demo.skills.api.model.CreateAccountRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Application service for savings account operations. */
@Service
@RequiredArgsConstructor
public class AccountService {

  private final NaughtyNameService naughtyNameService;
  private final AccountPersistenceService accountPersistenceService;

  /**
   * Creates a new savings account for the authenticated customer.
   *
   * <p>Request-level business rules are checked before any retried database work:
   * <ol>
   *   <li>Nickname (if provided) must not be on the profanity blocklist.
   * </ol>
   */
  public AccountResponse createAccount(
      final String customerId, final CreateAccountRequest request) {
    naughtyNameService.containsBlockedNicknameToken(request.getNickname());
    return accountPersistenceService.createAccount(customerId, request);
  }

  /** Returns the account with the given ID when it belongs to the authenticated customer. */
  public AccountResponse getAccount(final String customerId, final UUID accountId) {
    return accountPersistenceService.getAccount(customerId, accountId);
  }

  /** Returns all accounts belonging to the authenticated customer. */
  public AccountListResponse listAccounts(final String customerId) {
    return accountPersistenceService.listAccounts(customerId);
  }
}
