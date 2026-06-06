package com.demo.skills.account.domain;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for the profanity blocklist. */
@Repository
public interface BlockedNicknameRepository extends JpaRepository<BlockedNickname, Long> {

  /** Counts blocked words matching any normalized nickname token. */
  @Query("select count(b) from BlockedNickname b where lower(b.value) in :values")
  long countByNormalizedValueIn(@Param("values") Collection<String> values);
}
