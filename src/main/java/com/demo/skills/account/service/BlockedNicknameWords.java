package com.demo.skills.account.service;

import com.demo.skills.account.domain.BlockedNickname;
import com.demo.skills.account.domain.BlockedNicknameRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Provides cached normalized profanity blocklist words from the backing database. */
@Service
@RequiredArgsConstructor
public class BlockedNicknameWords {

  private final BlockedNicknameRepository blockedNicknameRepository;

  /** Returns normalized blocked words from the backing database, cached between refreshes. */
  @Cacheable("blockedNicknameWords")
  public List<String> words() {
    return blockedNicknameRepository.findAll().stream()
        .map(BlockedNickname::getValue)
        .map(NicknameNormalizer::normalize)
        .filter(word -> !word.isBlank())
        .distinct()
        .toList();
  }
}
