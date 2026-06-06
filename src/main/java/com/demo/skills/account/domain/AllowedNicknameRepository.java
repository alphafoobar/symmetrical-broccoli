package com.demo.skills.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for nickname allowlist entries. */
@Repository
public interface AllowedNicknameRepository extends JpaRepository<AllowedNickname, Long> {}
