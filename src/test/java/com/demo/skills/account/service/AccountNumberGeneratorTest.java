package com.demo.skills.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.demo.skills.account.domain.Account;
import com.demo.skills.account.domain.AccountRepository;
import com.demo.skills.account.domain.AccountStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountNumberGeneratorTest {

  private static final String CUSTOMER_ID = "1234567";

  @Mock
  private AccountRepository accountRepository;

  @InjectMocks
  private AccountNumberGenerator generator;

  @Nested
  @DisplayName("allocate — first account")
  class FirstAccount {

    @Test
    @DisplayName("generates account number from sequence and uses suffix 00")
    void generatesNewAccountNumberForFirstAccount() {
      // given
      given(accountRepository.findTopByCustomerIdOrderBySuffixDesc(CUSTOMER_ID))
          .willReturn(Optional.empty());
      given(accountRepository.nextAccountSequence()).willReturn(42L);

      // when
      val result = generator.allocate(CUSTOMER_ID);

      // then
      assertThat(result.accountNumber()).isEqualTo("03 1509 0000042");
      assertThat(result.suffix()).isEqualTo("00");
    }

    @Test
    @DisplayName("zero-pads the sequence number to 7 digits")
    void zeroPadsSequenceNumberTo7Digits() {
      // given
      given(accountRepository.findTopByCustomerIdOrderBySuffixDesc(CUSTOMER_ID))
          .willReturn(Optional.empty());
      given(accountRepository.nextAccountSequence()).willReturn(1L);

      // when
      val result = generator.allocate(CUSTOMER_ID);

      // then
      assertThat(result.accountNumber()).isEqualTo("03 1509 0000001");
    }
  }

  @Nested
  @DisplayName("allocate — subsequent accounts")
  class SubsequentAccounts {

    @Test
    @DisplayName("reuses existing account number and increments suffix")
    void reusesAccountNumberAndIncrementsSuffix() {
      // given
      val existing = accountWithSuffix("00");
      given(accountRepository.findTopByCustomerIdOrderBySuffixDesc(CUSTOMER_ID))
          .willReturn(Optional.of(existing));

      // when
      val result = generator.allocate(CUSTOMER_ID);

      // then
      assertThat(result.accountNumber()).isEqualTo("03 1509 0000001");
      assertThat(result.suffix()).isEqualTo("01");
    }

    @Test
    @DisplayName("increments suffix from 03 to 04 for fourth sub-account")
    void incrementsSuffixCorrectly() {
      // given
      val existing = accountWithSuffix("03");
      given(accountRepository.findTopByCustomerIdOrderBySuffixDesc(CUSTOMER_ID))
          .willReturn(Optional.of(existing));

      // when
      val result = generator.allocate(CUSTOMER_ID);

      // then
      assertThat(result.suffix()).isEqualTo("04");
    }
  }

  private static Account accountWithSuffix(final String suffix) {
    return Account.builder()
        .accountId(UUID.randomUUID())
        .accountNumber("03 1509 0000001")
        .suffix(suffix)
        .customerId(CUSTOMER_ID)
        .customerName("Alice Smith")
        .status(AccountStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }
}
