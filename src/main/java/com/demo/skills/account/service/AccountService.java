package com.demo.skills.account.service;

import com.demo.skills.account.domain.Account;
import com.demo.skills.account.domain.AccountRepository;
import com.demo.skills.account.domain.AccountStatus;
import com.demo.skills.api.model.AccountListResponse;
import com.demo.skills.api.model.AccountResponse;
import com.demo.skills.api.model.CreateAccountRequest;
import com.demo.skills.exception.AccountLimitExceededException;
import com.demo.skills.exception.AccountNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for savings account operations. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

  private static final int MAX_ACCOUNTS = 5;

  private final AccountRepository accountRepository;
  private final NaughtyNameService naughtyNameService;
  private final AccountNumberGenerator accountNumberGenerator;
  private final AccountMapper accountMapper;

  /**
   * Creates a new savings account for the authenticated customer.
   *
   * <p>Business rules enforced atomically within the transaction:
   * <ol>
   *   <li>Nickname (if provided) must not be on the profanity blocklist.
   *   <li>Customer must have fewer than 5 non-closed accounts.
   * </ol>
   */
  @Transactional
  @CircuitBreaker(name = "database")
  @CacheEvict(value = "accountList", key = "#customerId")
  public AccountResponse createAccount(
      final String customerId, final CreateAccountRequest request) {
    val nickName = request.getNickName();
    naughtyNameService.containsBlockedNicknameToken(nickName);

    accountRepository.lockCustomerAccountCreation(customerId);

    val activeCount =
        accountRepository.countByCustomerIdAndStatusNot(customerId, AccountStatus.CLOSED);
    if (activeCount >= MAX_ACCOUNTS) {
      throw new AccountLimitExceededException(customerId);
    }

    val allocation = accountNumberGenerator.allocate(customerId);
    val now = Instant.now();
    val account =
        Account.builder()
            .accountId(UUID.randomUUID())
            .accountNumber(allocation.accountNumber())
            .suffix(allocation.suffix())
            .customerId(customerId)
            .customerName(request.getCustomerName())
            .nickName(nickName)
            .status(AccountStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();

    val saved = accountRepository.save(account);

    log.atInfo()
        .addKeyValue("customerId", customerId)
        .addKeyValue("accountId", saved.getAccountId())
        .addKeyValue("suffix", allocation.suffix())
        .log("Account created");

    return accountMapper.toResponse(saved);
  }

  /** Returns the account with the given ID when it belongs to the authenticated customer. */
  @Transactional(readOnly = true)
  @CircuitBreaker(name = "database")
  @Cacheable(value = "accounts", key = "#customerId + ':' + #accountId.toString()")
  public AccountResponse getAccount(final String customerId, final UUID accountId) {
    return accountRepository
        .findByAccountIdAndCustomerId(accountId, customerId)
        .map(accountMapper::toResponse)
        .orElseThrow(() -> new AccountNotFoundException(accountId));
  }

  /** Returns all accounts belonging to the authenticated customer. */
  @Transactional(readOnly = true)
  @CircuitBreaker(name = "database")
  @Cacheable(value = "accountList", key = "#customerId")
  public AccountListResponse listAccounts(final String customerId) {
    val accounts =
        accountRepository.findByCustomerIdOrderBySuffixAsc(customerId).stream()
            .map(accountMapper::toResponse)
            .toList();

    val response = new AccountListResponse();
    response.setAccounts(accounts);
    return response;
  }
}
