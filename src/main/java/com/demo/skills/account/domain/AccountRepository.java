package com.demo.skills.account.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Repository for {@link Account} persistence. */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

  /** Counts non-closed accounts for a customer to enforce the 5-account limit. */
  long countByCustomerIdAndStatusNot(String customerId, AccountStatus status);

  /** Serialises account creation for one customer within the current transaction. */
  @Query(
      value =
          """
          with customer_lock as (
              select pg_advisory_xact_lock(hashtextextended(:customerId, cast(0 as bigint)))
          )
          select true
          """,
      nativeQuery = true)
  boolean lockCustomerAccountCreation(String customerId);

  /** Returns all accounts belonging to a customer, ordered by suffix for display. */
  List<Account> findByCustomerIdOrderBySuffixAsc(String customerId);

  /** Returns an account only when it belongs to the given customer. */
  Optional<Account> findByAccountIdAndCustomerId(UUID accountId, String customerId);

  /** Returns the account with the highest suffix for a customer, used for suffix allocation. */
  Optional<Account> findTopByCustomerIdOrderBySuffixDesc(String customerId);

  /** Calls the PostgreSQL sequence to generate the next 7-digit account number seed. */
  @Query(value = "select nextval('account_seq')", nativeQuery = true)
  long nextAccountSequence();
}
