package com.demo.skills.account.service;

import com.demo.skills.exception.NicknameNotAllowedException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/** Validates account nicknames against the profanity blocklist. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaughtyNameService {

  private final BlockedNicknameList blockedNicknameList;
  private final AllowedNicknameList allowedNicknameList;

  /** Throws when the supplied nickname contains a blocked token. */
  public void containsBlockedNicknameToken(final @Nullable String nickname) {
    if (nickname == null || nickname.isBlank()) {
      return;
    }

    val candidates = candidateTokens(nickname);
    if (candidates.isEmpty()) {
      return;
    }

    val candidatesToCheck = allowedNicknameList.candidatesToCheck(candidates);
    if (candidatesToCheck.isEmpty()) {
      return;
    }

    if (blockedNicknameList.containsBlockedWord(candidatesToCheck)) {
      // This might log profanities, but it may be useful to check if any should be whitelisted.
      log.atInfo()
          .addKeyValue("candidatesToCheck", candidatesToCheck)
          .log("Nickname contains blocked token");
      throw new NicknameNotAllowedException();
    }
  }

  private static Set<String> candidateTokens(final String value) {
    val lower = value.toLowerCase(Locale.ROOT);

    return Stream.concat(
            // Normal word-like chunks: "bad word" -> ["bad", "word"]
            Arrays.stream(lower.split("\\s+")),
            // Compact version: "b.a.d.w.o.r.d" -> "badword"
            Stream.of(lower))
        .map(NicknameNormalizer::normalize)
        .filter(token -> !token.isBlank())
        .collect(Collectors.toSet());
  }
}
