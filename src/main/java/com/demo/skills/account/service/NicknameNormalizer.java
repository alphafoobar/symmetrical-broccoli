package com.demo.skills.account.service;

import java.util.Locale;

/** Normalizes nickname words for blocklist and allowlist matching. */
final class NicknameNormalizer {

  private NicknameNormalizer() {}

  static String normalize(final String value) {
    return lettersOnly(replaceLeetCharacters(value.toLowerCase(Locale.ROOT)));
  }

  private static String replaceLeetCharacters(final String value) {
    return value
        .replace('0', 'o')
        .replace('1', 'i')
        .replace('3', 'e')
        .replace('4', 'a')
        .replace('5', 's')
        .replace('7', 't')
        .replace('@', 'a')
        .replace('$', 's')
        .replace('!', 'i');
  }

  private static String lettersOnly(final String value) {
    return value.replaceAll("[^a-z]", "");
  }
}
