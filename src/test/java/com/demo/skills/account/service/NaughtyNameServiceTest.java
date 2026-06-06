package com.demo.skills.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.demo.skills.exception.NicknameNotAllowedException;
import java.util.List;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class NaughtyNameServiceTest {

  @Mock
  private BlockedNicknameList blockedNicknameList;

  @Mock
  private AllowedNicknameList allowedNicknameList;

  @InjectMocks
  private NaughtyNameService naughtyNameService;

  @Nested
  @DisplayName("containsBlockedNicknameToken")
  class ContainsBlockedNicknameToken {

    @Test
    @DisplayName("returns when nickname is null")
    void returnsWhenNicknameNull() {
      // when / then
      assertThatCode(() -> naughtyNameService.containsBlockedNicknameToken(null))
          .doesNotThrowAnyException();

      then(blockedNicknameList).should(never()).containsBlockedWord(any());
      then(allowedNicknameList).should(never()).candidatesToCheck(any());
    }

    @Test
    @DisplayName("returns when nickname has no searchable tokens")
    void returnsWhenNicknameHasNoSearchableTokens() {
      // when / then
      assertThatCode(() -> naughtyNameService.containsBlockedNicknameToken("268 - _"))
          .doesNotThrowAnyException();

      then(blockedNicknameList).should(never()).containsBlockedWord(any());
      then(allowedNicknameList).should(never()).candidatesToCheck(any());
    }

    @Test
    @DisplayName("throws NicknameNotAllowedException when nickname is on blocklist")
    void throwsWhenNicknameBlocked() {
      // given
      val candidatesToCheck = List.of("blocked");
      given(allowedNicknameList.candidatesToCheck(any())).willReturn(candidatesToCheck);
      given(blockedNicknameList.containsBlockedWord(candidatesToCheck)).willReturn(true);

      // when / then
      assertThatThrownBy(() -> naughtyNameService.containsBlockedNicknameToken("blocked"))
          .isInstanceOf(NicknameNotAllowedException.class);
    }

    @Test
    @DisplayName("logs candidates to check without raw nickname when nickname is blocked")
    void logsCandidatesToCheckWithoutRawNicknameWhenNicknameBlocked(final CapturedOutput output) {
      // given
      val nickname = "private blocked nickname";
      val candidatesToCheck = List.of("private", "blocked", "nickname", "privateblockednickname");
      given(allowedNicknameList.candidatesToCheck(any())).willReturn(candidatesToCheck);
      given(blockedNicknameList.containsBlockedWord(candidatesToCheck)).willReturn(true);

      // when / then
      assertThatThrownBy(() -> naughtyNameService.containsBlockedNicknameToken(nickname))
          .isInstanceOf(NicknameNotAllowedException.class);

      assertThat(output)
          .contains("Nickname contains blocked token")
          .contains("candidatesToCheck")
          .contains("privateblockednickname")
          .doesNotContain(nickname);
    }

    @Test
    @DisplayName("throws NicknameNotAllowedException when any nickname token is blocked")
    void throwsWhenNicknameContainsBlockedToken() {
      // given
      val candidatesToCheck = List.of("safe", "blocked", "safeblocked");
      given(allowedNicknameList.candidatesToCheck(any())).willReturn(candidatesToCheck);
      given(blockedNicknameList.containsBlockedWord(candidatesToCheck)).willReturn(true);

      // when / then
      assertThatThrownBy(() -> naughtyNameService.containsBlockedNicknameToken("safe blocked"))
          .isInstanceOf(NicknameNotAllowedException.class);
    }

    @Test
    @DisplayName("throws NicknameNotAllowedException when nickname contains a blocked substring")
    void throwsWhenNicknameContainsBlockedSubstring() {
      // given
      val candidatesToCheck = List.of("unblockedname");
      given(allowedNicknameList.candidatesToCheck(any())).willReturn(candidatesToCheck);
      given(blockedNicknameList.containsBlockedWord(candidatesToCheck)).willReturn(true);

      // when / then
      assertThatThrownBy(() -> naughtyNameService.containsBlockedNicknameToken("unblockedname"))
          .isInstanceOf(NicknameNotAllowedException.class);
    }

    @Test
    @DisplayName("throws NicknameNotAllowedException when a token normalizes to a blocked word")
    void throwsWhenNicknameTokenNormalizesToBlockedWord() {
      // given
      val candidatesToCheck = List.of("notallowedi");
      given(allowedNicknameList.candidatesToCheck(any())).willReturn(candidatesToCheck);
      given(blockedNicknameList.containsBlockedWord(candidatesToCheck)).willReturn(true);

      // when / then
      assertThatThrownBy(() -> naughtyNameService.containsBlockedNicknameToken("notallowed1"))
          .isInstanceOf(NicknameNotAllowedException.class);
    }

    @Test
    @DisplayName("returns when nickname candidate is allowlisted")
    void returnsWhenNicknameCandidateAllowlisted() {
      // given
      given(allowedNicknameList.candidatesToCheck(any())).willReturn(List.of());

      // when / then
      assertThatCode(() -> naughtyNameService.containsBlockedNicknameToken("classic"))
          .doesNotThrowAnyException();

      then(blockedNicknameList).should(never()).containsBlockedWord(any());
    }

    @Test
    @DisplayName("returns when nickname does not contain a blocked token")
    void returnsWhenNicknameAllowed() {
      // given
      val candidatesToCheck = List.of("safe", "name");
      given(allowedNicknameList.candidatesToCheck(any())).willReturn(candidatesToCheck);
      given(blockedNicknameList.containsBlockedWord(candidatesToCheck)).willReturn(false);

      // when / then
      assertThatCode(() -> naughtyNameService.containsBlockedNicknameToken("safe name"))
          .doesNotThrowAnyException();
    }
  }
}
