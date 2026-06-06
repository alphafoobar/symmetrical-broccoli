package com.demo.skills.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.demo.skills.api.model.AccountListResponse;
import com.demo.skills.api.model.AccountResponse;
import com.demo.skills.api.model.CreateAccountRequest;
import com.demo.skills.exception.NicknameNotAllowedException;
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
class AccountServiceTest {

  private static final String CUSTOMER_ID = "1234567";

  @Mock
  private NaughtyNameService naughtyNameService;

  @Mock
  private AccountPersistenceService accountPersistenceService;

  @InjectMocks
  private AccountService accountService;

  @Nested
  @DisplayName("createAccount")
  class CreateAccount {

    @Test
    @DisplayName("validates nickname before delegating to persistence")
    void validatesNicknameBeforeDelegating() {
      // given
      val request = new CreateAccountRequest();
      request.setCustomerName("Alice Smith");
      request.setNickname("holiday");
      val response = new AccountResponse();
      given(accountPersistenceService.createAccount(CUSTOMER_ID, request)).willReturn(response);

      // when
      val result = accountService.createAccount(CUSTOMER_ID, request);

      // then
      assertThat(result).isSameAs(response);
      val ordered = inOrder(naughtyNameService, accountPersistenceService);
      ordered.verify(naughtyNameService).containsBlockedNicknameToken("holiday");
      ordered.verify(accountPersistenceService).createAccount(CUSTOMER_ID, request);
    }

    @Test
    @DisplayName("validates nullable nickname when nickname is absent")
    void validatesNullableNicknameWhenNoNickname() {
      // given
      val request = new CreateAccountRequest();
      request.setCustomerName("Alice Smith");
      val response = new AccountResponse();
      given(accountPersistenceService.createAccount(CUSTOMER_ID, request)).willReturn(response);

      // when
      accountService.createAccount(CUSTOMER_ID, request);

      // then
      then(naughtyNameService).should().containsBlockedNicknameToken(null);
    }

    @Test
    @DisplayName("does not call persistence when nickname is blocked")
    void doesNotCallPersistenceWhenNicknameBlocked() {
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

      then(accountPersistenceService).should(never()).createAccount(CUSTOMER_ID, request);
    }
  }

  @Nested
  @DisplayName("getAccount")
  class GetAccount {

    @Test
    @DisplayName("delegates account lookup to persistence")
    void delegatesAccountLookupToPersistence() {
      // given
      val accountId = UUID.randomUUID();
      val response = new AccountResponse();
      given(accountPersistenceService.getAccount(CUSTOMER_ID, accountId)).willReturn(response);

      // when
      val result = accountService.getAccount(CUSTOMER_ID, accountId);

      // then
      assertThat(result).isSameAs(response);
    }
  }

  @Nested
  @DisplayName("listAccounts")
  class ListAccounts {

    @Test
    @DisplayName("delegates account listing to persistence")
    void delegatesAccountListingToPersistence() {
      // given
      val response = new AccountListResponse();
      given(accountPersistenceService.listAccounts(CUSTOMER_ID)).willReturn(response);

      // when
      val result = accountService.listAccounts(CUSTOMER_ID);

      // then
      assertThat(result).isSameAs(response);
    }
  }
}
