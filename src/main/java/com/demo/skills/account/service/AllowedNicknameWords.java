package com.demo.skills.account.service;

import com.demo.skills.account.domain.AllowedNickname;
import com.demo.skills.account.domain.AllowedNicknameRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Provides cached normalized nickname allowlist words from the backing database. */
@Service
@RequiredArgsConstructor
public class AllowedNicknameWords {

  private final AllowedNicknameRepository allowedNicknameRepository;

  /** Returns normalized allowed words from the backing database, cached between refreshes. */
  @Cacheable("allowedNicknameWords")
  public List<String> words() {
    return allowedNicknameRepository.findAll().stream()
        .map(AllowedNickname::getValue)
        .map(NicknameNormalizer::normalize)
        .filter(word -> !word.isBlank())
        .distinct()
        .toList();
  }
}
