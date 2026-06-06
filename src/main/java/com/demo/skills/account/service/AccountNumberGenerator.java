package com.demo.skills.account.service;

import com.demo.skills.account.domain.Account;
import com.demo.skills.account.domain.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Component;

/**
 * Generates NZ-format bank account numbers for new accounts.
 *
 * <p>Format: {@code BB BBBB AAAAAAA SS} where bank=03, branch=1509, account comes from a DB
 * sequence, and suffix increments per sub-account (00, 01, 02 ...).
 */
@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

  private static final String BANK_CODE = "03";
  private static final String BRANCH = "1509";

  private final AccountRepository accountRepository;

  /**
   * Allocates an account number and suffix for a new account belonging to {@code customerId}.
   *
   * <p>If the customer has no prior accounts, a new base number is drawn from the DB sequence.
   * Subsequent accounts reuse the same base number and increment the suffix.
   */
  public AccountNumberAllocation allocate(final String customerId) {
    return accountRepository
        .findTopByCustomerIdOrderBySuffixDesc(customerId)
        .map(AccountNumberGenerator::nextSubAccount)
        .orElseGet(this::firstAccount);
  }

  private AccountNumberAllocation firstAccount() {
    val seq = accountRepository.nextAccountSequence();
    val accountNumber = "%s %s %07d".formatted(BANK_CODE, BRANCH, seq);
    return new AccountNumberAllocation(accountNumber, "00");
  }

  private static AccountNumberAllocation nextSubAccount(final Account current) {
    val nextSuffix = Integer.parseInt(current.getSuffix()) + 1;
    return new AccountNumberAllocation(current.getAccountNumber(), "%02d".formatted(nextSuffix));
  }

  /** Holds the base account number and subaccount suffix for a new account. */
  public record AccountNumberAllocation(String accountNumber, String suffix) {}
}
