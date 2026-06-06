package com.demo.skills.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockedNicknameListTest {

  @Mock
  private BlockedNicknameWords blockedNicknameWords;

  @InjectMocks
  private BlockedNicknameList blockedNicknameList;

  @Test
  @DisplayName("returns true when any candidate contains blocked word")
  void returnsTrueWhenAnyCandidateContainsBlockedWord() {
    // given
    given(blockedNicknameWords.words()).willReturn(List.of("blocked"));

    // when
    val containsBlockedWord = blockedNicknameList.containsBlockedWord(List.of("safe", "unblocked"));

    // then
    assertThat(containsBlockedWord).isTrue();
  }

  @Test
  @DisplayName("returns false when candidates do not contain blocked word")
  void returnsFalseWhenCandidatesDoNotContainBlockedWord() {
    // given
    given(blockedNicknameWords.words()).willReturn(List.of("blocked"));

    // when
    val containsBlockedWord = blockedNicknameList.containsBlockedWord(List.of("safe", "name"));

    // then
    assertThat(containsBlockedWord).isFalse();
  }

}
