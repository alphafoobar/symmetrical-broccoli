package com.demo.skills.account.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

/** Provides cached access to the profanity blocklist. */
@Service
@RequiredArgsConstructor
public class BlockedNicknameList {

  private final BlockedNicknameWords blockedNicknameWords;

  /** Returns true when any candidate contains a blocked word. */
  public boolean containsBlockedWord(final List<String> candidates) {
    val blockedWords = blockedNicknameWords.words();
    return candidates.stream()
        .anyMatch(candidate -> blockedWords.stream().anyMatch(candidate::contains));
  }
}
