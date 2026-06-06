package com.demo.skills.exception;

/** Thrown when a customer attempts to create a sixth non-closed account. */
public class AccountLimitExceededException extends RuntimeException {

  private final String customerId;

  /** Creates a new exception for the given customer. */
  public AccountLimitExceededException(final String customerId) {
    super("Customer %s has reached the maximum of 5 accounts".formatted(customerId));
    this.customerId = customerId;
  }

  /** The customer ID that triggered the limit. */
  public String customerId() {
    return customerId;
  }
}
