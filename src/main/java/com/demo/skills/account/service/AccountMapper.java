package com.demo.skills.account.service;

import com.demo.skills.account.domain.Account;
import com.demo.skills.api.model.AccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapper between {@link Account} entities and OpenAPI-generated response models. */
@Mapper(componentModel = "spring")
public interface AccountMapper {

  /**
   * Maps an {@link Account} entity to an {@link AccountResponse}.
   *
   * <p>The {@code accountNumber} field in the response is the full NZ bank account number
   * including the suffix, e.g. {@code "03 1509 0000001 00"}.
   */
  @Mapping(
      target = "accountNumber",
      expression = "java(account.getAccountNumber() + \" \" + account.getSuffix())")
  @Mapping(
      target = "createdAt",
      expression = "java(account.getCreatedAt().atOffset(java.time.ZoneOffset.UTC))")
  AccountResponse toResponse(Account account);
}
