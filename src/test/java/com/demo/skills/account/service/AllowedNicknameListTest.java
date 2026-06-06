package com.demo.skills.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Set;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AllowedNicknameListTest {

  @Mock
  private AllowedNicknameWords allowedNicknameWords;

  @InjectMocks
  private AllowedNicknameList allowedNicknameList;

  @Test
  @DisplayName("returns candidates that are not allowlisted")
  void returnsCandidatesThatAreNotAllowlisted() {
    // given
    given(allowedNicknameWords.words()).willReturn(List.of("classic"));

    // when
    val candidatesToCheck = allowedNicknameList.candidatesToCheck(Set.of("classic", "blocked"));

    // then
    assertThat(candidatesToCheck).containsOnly("blocked");
  }

  @Test
  @DisplayName("returns immutable candidates to check")
  void returnsImmutableCandidatesToCheck() {
    // given
    given(allowedNicknameWords.words()).willReturn(List.of("classic"));

    // when
    val candidatesToCheck = allowedNicknameList.candidatesToCheck(Set.of("classic", "blocked"));

    // then
    assertThat(candidatesToCheck).containsOnly("blocked");
    assertThatThrownBy(() -> candidatesToCheck.add("other"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

}
