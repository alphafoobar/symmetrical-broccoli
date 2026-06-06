package com.demo.skills.account.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AccountRepositoryTest {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Nested
  @DisplayName("countByCustomerIdAndStatusNot")
  class CountByCustomerIdAndStatusNot {

    @Test
    @DisplayName("counts only non-closed accounts")
    void countsOnlyNonClosedAccounts() {
      // given
      val customerId = "1234567";
      saveAccount(customerId, AccountStatus.ACTIVE, "00");
      saveAccount(customerId, AccountStatus.INACTIVE, "01");
      saveAccount(customerId, AccountStatus.CLOSED, "02");
      entityManager.flush();
      entityManager.clear();

      // when
      val count =
          accountRepository.countByCustomerIdAndStatusNot(customerId, AccountStatus.CLOSED);

      // then
      assertThat(count).isEqualTo(2L);
    }
  }

  @Nested
  @DisplayName("lockCustomerAccountCreation")
  class LockCustomerAccountCreation {

    @Test
    @DisplayName("acquires a transaction-scoped advisory lock for the customer")
    void acquiresTransactionScopedAdvisoryLock() {
      // when
      val locked = accountRepository.lockCustomerAccountCreation("1234567");

      // then
      assertThat(locked).isTrue();
    }
  }

  @Nested
  @DisplayName("findByCustomerIdOrderBySuffixAsc")
  class FindByCustomerId {

    @Test
    @DisplayName("returns accounts ordered by suffix ascending")
    void returnsAccountsOrderedBySuffix() {
      // given
      val customerId = "2345678";
      saveAccount(customerId, AccountStatus.ACTIVE, "01");
      saveAccount(customerId, AccountStatus.ACTIVE, "00");
      entityManager.flush();
      entityManager.clear();

      // when
      val accounts = accountRepository.findByCustomerIdOrderBySuffixAsc(customerId);

      // then
      assertThat(accounts).hasSize(2);
      assertThat(accounts.get(0).getSuffix()).isEqualTo("00");
      assertThat(accounts.get(1).getSuffix()).isEqualTo("01");
    }
  }

  @Nested
  @DisplayName("findTopByCustomerIdOrderBySuffixDesc")
  class FindTopByCustomerIdOrderBySuffixDesc {

    @Test
    @DisplayName("returns account with highest suffix")
    void returnsAccountWithHighestSuffix() {
      // given
      val customerId = "3456789";
      saveAccount(customerId, AccountStatus.ACTIVE, "00");
      saveAccount(customerId, AccountStatus.ACTIVE, "01");
      entityManager.flush();
      entityManager.clear();

      // when
      val top = accountRepository.findTopByCustomerIdOrderBySuffixDesc(customerId);

      // then
      assertThat(top)
          .hasValueSatisfying(account -> assertThat(account.getSuffix()).isEqualTo("01"));
    }

    @Test
    @DisplayName("returns empty when customer has no accounts")
    void returnsEmptyWhenNoAccounts() {
      // when
      val top =
          accountRepository.findTopByCustomerIdOrderBySuffixDesc("9999999");

      // then
      assertThat(top).isEmpty();
    }
  }

  @Nested
  @DisplayName("nextAccountSequence")
  class NextAccountSequence {

    @Test
    @DisplayName("returns monotonically increasing values")
    void returnsMonotonicallyIncreasingValues() {
      // when
      val first = accountRepository.nextAccountSequence();
      val second = accountRepository.nextAccountSequence();

      // then
      assertThat(second).isGreaterThan(first);
    }
  }

  // --- helpers ---

  private void saveAccount(
      final String customerId, final AccountStatus status, final String suffix) {
    val account =
        Account.builder()
            .accountId(UUID.randomUUID())
            .accountNumber("03 1509 0000001")
            .suffix(suffix)
            .customerId(customerId)
            .customerName("Alice Smith")
            .status(status)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    entityManager.persist(account);
  }
}
