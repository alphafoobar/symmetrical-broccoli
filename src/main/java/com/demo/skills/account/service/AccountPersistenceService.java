package com.demo.skills.account.service;

import com.demo.skills.account.domain.Account;
import com.demo.skills.account.domain.AccountRepository;
import com.demo.skills.account.domain.AccountStatus;
import com.demo.skills.api.model.AccountListResponse;
import com.demo.skills.api.model.AccountResponse;
import com.demo.skills.api.model.CreateAccountRequest;
import com.demo.skills.exception.AccountLimitExceededException;
import com.demo.skills.exception.AccountNotFoundException;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional account persistence operations with retry for transient database failures. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountPersistenceService {

  private static final int MAX_ACCOUNTS = 5;

  private final AccountRepository accountRepository;
  private final AccountNumberGenerator accountNumberGenerator;
  private final AccountMapper accountMapper;

  /** Creates a new savings account after request-level business validation has passed. */
  @Retry(name = "database")
  @Transactional
  @CacheEvict(value = "accountList", key = "#customerId")
  public AccountResponse createAccount(
      final String customerId, final CreateAccountRequest request) {
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
            .nickname(request.getNickname())
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
  @Retry(name = "database")
  @Transactional(readOnly = true)
  @Cacheable(value = "accounts", key = "#customerId + ':' + #accountId.toString()")
  public AccountResponse getAccount(final String customerId, final UUID accountId) {
    return accountRepository
        .findByAccountIdAndCustomerId(accountId, customerId)
        .map(accountMapper::toResponse)
        .orElseThrow(() -> new AccountNotFoundException(accountId));
  }

  /** Returns all accounts belonging to the authenticated customer. */
  @Retry(name = "database")
  @Transactional(readOnly = true)
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
