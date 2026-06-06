package com.demo.skills.account.service;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

/** Provides cached access to nickname allowlist entries. */
@Service
@RequiredArgsConstructor
public class AllowedNicknameList {

  private final AllowedNicknameWords allowedNicknameWords;

  /** Returns candidates that are not explicitly allowlisted and still require blocklist checks. */
  public List<String> candidatesToCheck(final Set<String> candidates) {
    val allowedWords = allowedNicknameWords.words();
    return candidates.stream().filter(candidate -> !allowedWords.contains(candidate)).toList();
  }
}
