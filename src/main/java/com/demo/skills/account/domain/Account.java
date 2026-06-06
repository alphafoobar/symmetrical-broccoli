package com.demo.skills.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

/** JPA entity for a savings bank account. */
@Entity
@Table(name = "accounts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Account {

  @Id
  private UUID accountId;

  @Version
  private long version;

  @Column(nullable = false)
  private String accountNumber;

  @Column(nullable = false)
  private String suffix;

  @Column(nullable = false)
  private String customerId;

  @Column(nullable = false)
  private String customerName;

  @Nullable
  private String nickName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;
}
