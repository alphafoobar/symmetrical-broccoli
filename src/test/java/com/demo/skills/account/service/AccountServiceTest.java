package com.demo.skills.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.demo.skills.account.domain.Account;
import com.demo.skills.account.domain.AccountRepository;
import com.demo.skills.account.domain.AccountStatus;
import com.demo.skills.api.model.AccountResponse;
import com.demo.skills.api.model.CreateAccountRequest;
import com.demo.skills.exception.AccountLimitExceededException;
import com.demo.skills.exception.AccountNotFoundException;
import com.demo.skills.exception.NicknameNotAllowedException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AccountServiceTest {

  private static final String CUSTOMER_ID = "1234567";

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private NaughtyNameService naughtyNameService;

  @Mock
  private AccountNumberGenerator accountNumberGenerator;

  @Mock
  private AccountMapper accountMapper;

  @InjectMocks
  private AccountService accountService;

  @Nested
  @DisplayName("createAccount")
  class CreateAccount {

    @Test
    @DisplayName("creates account when all business rules pass")
    void createsAccountWhenRulesPass() {
      // given
      val request = new CreateAccountRequest();
      request.setCustomerName("Alice Smith");

      val allocation = new AccountNumberGenerator.AccountNumberAllocation("03 1509 0000001", "00");
      given(accountRepository.countByCustomerIdAndStatusNot(CUSTOMER_ID, AccountStatus.CLOSED))
          .willReturn(0L);
      given(accountNumberGenerator.allocate(CUSTOMER_ID)).willReturn(allocation);
      given(accountRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
      given(accountMapper.toResponse(any())).willReturn(anAccountResponse());

      // when
      val result = accountService.createAccount(CUSTOMER_ID, request);

      // then
      assertThat(result).isNotNull();
      then(accountRepository).should().save(any(Account.class));
    }

    @Test
    @DisplayName("locks customer account creation before counting existing accounts")
    void locksCustomerBeforeCountingExistingAccounts() {
      // given
      val request = new CreateAccountRequest();
      request.setCustomerName("Alice Smith");

      val allocation = new AccountNumberGenerator.AccountNumberAllocation("03 1509 0000001", "00");
      given(accountRepository.countByCustomerIdAndStatusNot(CUSTOMER_ID, AccountStatus.CLOSED))
          .willReturn(0L);
      given(accountNumberGenerator.allocate(CUSTOMER_ID)).willReturn(allocation);
      given(accountRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
      given(accountMapper.toResponse(any())).willReturn(anAccountResponse());

      // when
      accountService.createAccount(CUSTOMER_ID, request);

      // then
      val inOrder = inOrder(accountRepository);
      inOrder.verify(accountRepository).lockCustomerAccountCreation(CUSTOMER_ID);
      inOrder
          .verify(accountRepository)
          .countByCustomerIdAndStatusNot(CUSTOMER_ID, AccountStatus.CLOSED);
    }

    @Test
    @DisplayName("throws NicknameNotAllowedException when nickname is on blocklist")
    void throwsWhenNicknameBlocked() {
      // given
      val request = new CreateAccountRequest();
      request.setCustomerName("Alice Smith");
      request.setNickname("blocked");
      willThrow(new NicknameNotAllowedException())
          .given(naughtyNameService)
          .containsBlockedNicknameToken("blocked");

      // when / then
      assertThatThrownBy(() -> accountService.createAccount(CUSTOMER_ID, request))
          .isInstanceOf(NicknameNotAllowedException.class);

      then(accountRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("throws NicknameNotAllowedException when any nickname token is blocked")
    void throwsWhenNicknameContainsBlockedToken() {
      // given
      val request = new CreateAccountRequest();
      request.setCustomerName("Alice Smith");
      request.setNickname("safe blocked");
      willThrow(new NicknameNotAllowedException())
          .given(naughtyNameService)
          .containsBlockedNicknameToken("safe blocked");

      // when / then
      assertThatThrownBy(() -> accountService.createAccount(CUSTOMER_ID, request))
          .isInstanceOf(NicknameNotAllowedException.class);

      then(accountRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("throws NicknameNotAllowedException when a token normalizes to a blocked word")
    void throwsWhenNicknameTokenNormalizesToBlockedWord() {
      // given
      val request = new CreateAccountRequest();
      request.setCustomerName("Alice Smith");
      request.setNickname("notallowed1");
      willThrow(new NicknameNotAllowedException())
          .given(naughtyNameService)
          .containsBlockedNicknameToken("notallowed1");

      // when / then
      assertThatThrownBy(() -> accountService.createAccount(CUSTOMER_ID, request))
          .isInstanceOf(NicknameNotAllowedException.class);

      then(accountRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("throws AccountLimitExceededException when customer has 5 non-closed accounts")
    void throwsWhenAccountLimitReached() {
      // given
      val request = new CreateAccountRequest();
      request.setCustomerName("Alice Smith");
      given(accountRepository.countByCustomerIdAndStatusNot(CUSTOMER_ID, AccountStatus.CLOSED))
          .willReturn(5L);

      // when / then
      assertThatThrownBy(() -> accountService.createAccount(CUSTOMER_ID, request))
          .isInstanceOf(AccountLimitExceededException.class)
          .hasMessage("Maximum account limit reached");

      then(accountRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("validates nullable nickname when nickname is absent")
    void validatesNullableNicknameWhenNoNickname() {
      // given
      val request = new CreateAccountRequest();
      request.setCustomerName("Alice Smith");

      given(accountRepository.countByCustomerIdAndStatusNot(CUSTOMER_ID, AccountStatus.CLOSED))
          .willReturn(0L);
      val allocation = new AccountNumberGenerator.AccountNumberAllocation("03 1509 0000001", "00");
      given(accountNumberGenerator.allocate(CUSTOMER_ID)).willReturn(allocation);
      given(accountRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
      given(accountMapper.toResponse(any())).willReturn(anAccountResponse());

      // when
      accountService.createAccount(CUSTOMER_ID, request);

      // then
      then(naughtyNameService).should().containsBlockedNicknameToken(null);
    }

    @Test
    @DisplayName("does not log raw account numbers when creating an account")
    void doesNotLogRawAccountNumber(final CapturedOutput output) {
      // given
      val request = new CreateAccountRequest();
      request.setCustomerName("Alice Smith");

      val allocation = new AccountNumberGenerator.AccountNumberAllocation("03 1509 0000001", "00");
      given(accountRepository.countByCustomerIdAndStatusNot(CUSTOMER_ID, AccountStatus.CLOSED))
          .willReturn(0L);
      given(accountNumberGenerator.allocate(CUSTOMER_ID)).willReturn(allocation);
      given(accountRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
      given(accountMapper.toResponse(any())).willReturn(anAccountResponse());

      // when
      accountService.createAccount(CUSTOMER_ID, request);

      // then
      assertThat(output).contains("Account created").doesNotContain(allocation.accountNumber());
    }
  }

  @Nested
  @DisplayName("getAccount")
  class GetAccount {

    @Test
    @DisplayName("returns account when found")
    void returnsAccountWhenFound() {
      // given
      val accountId = UUID.randomUUID();
      val account = anAccount(accountId);
      given(accountRepository.findByAccountIdAndCustomerId(accountId, CUSTOMER_ID))
          .willReturn(Optional.of(account));
      given(accountMapper.toResponse(account)).willReturn(anAccountResponse());

      // when
      val result = accountService.getAccount(CUSTOMER_ID, accountId);

      // then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("throws AccountNotFoundException when account does not exist")
    void throwsWhenNotFound() {
      // given
      val accountId = UUID.randomUUID();
      given(accountRepository.findByAccountIdAndCustomerId(accountId, CUSTOMER_ID))
          .willReturn(Optional.empty());

      // when / then
      assertThatThrownBy(() -> accountService.getAccount(CUSTOMER_ID, accountId))
          .isInstanceOf(AccountNotFoundException.class)
          .hasMessageContaining(accountId.toString());
    }

    @Test
    @DisplayName("throws AccountNotFoundException when account belongs to another customer")
    void throwsWhenAccountBelongsToAnotherCustomer() {
      // given
      val accountId = UUID.randomUUID();
      given(accountRepository.findByAccountIdAndCustomerId(accountId, CUSTOMER_ID))
          .willReturn(Optional.empty());

      // when / then
      assertThatThrownBy(() -> accountService.getAccount(CUSTOMER_ID, accountId))
          .isInstanceOf(AccountNotFoundException.class)
          .hasMessageContaining(accountId.toString());
    }
  }

  @Nested
  @DisplayName("listAccounts")
  class ListAccounts {

    @Test
    @DisplayName("returns all accounts for customer")
    void returnsAllAccountsForCustomer() {
      // given
      val account = anAccount(UUID.randomUUID());
      given(accountRepository.findByCustomerIdOrderBySuffixAsc(CUSTOMER_ID))
          .willReturn(List.of(account));
      given(accountMapper.toResponse(account)).willReturn(anAccountResponse());

      // when
      val result = accountService.listAccounts(CUSTOMER_ID);

      // then
      assertThat(result.getAccounts()).hasSize(1);
    }

    @Test
    @DisplayName("returns empty list when customer has no accounts")
    void returnsEmptyListWhenNoAccounts() {
      // given
      given(accountRepository.findByCustomerIdOrderBySuffixAsc(CUSTOMER_ID))
          .willReturn(List.of());

      // when
      val result = accountService.listAccounts(CUSTOMER_ID);

      // then
      assertThat(result.getAccounts()).isEmpty();
    }
  }

  // --- helpers ---

  private static Account anAccount(final UUID accountId) {
    return Account.builder()
        .accountId(accountId)
        .accountNumber("03 1509 0000001")
        .suffix("00")
        .customerId(CUSTOMER_ID)
        .customerName("Alice Smith")
        .status(AccountStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  private static AccountResponse anAccountResponse() {
    val response = new AccountResponse();
    response.setAccountId(UUID.randomUUID());
    response.setAccountNumber("03 1509 0000001 00");
    response.setCustomerId(CUSTOMER_ID);
    response.setCustomerName("Alice Smith");
    response.setStatus(com.demo.skills.api.model.AccountStatus.ACTIVE);
    response.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return response;
  }
}
