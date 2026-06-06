package com.demo.skills.exception;

import java.util.UUID;

/** Thrown when an account cannot be found by its ID. */
public class AccountNotFoundException extends RuntimeException {

  private final UUID accountId;

  /** Creates a new exception for the given account ID. */
  public AccountNotFoundException(final UUID accountId) {
    super("Account not found: %s".formatted(accountId));
    this.accountId = accountId;
  }

  /** The account ID that was not found. */
  public UUID accountId() {
    return accountId;
  }
}
