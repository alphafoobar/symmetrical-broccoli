package com.demo.skills.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.demo.skills.account.domain.AllowedNickname;
import com.demo.skills.account.domain.AllowedNicknameRepository;
import java.util.List;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AllowedNicknameWordsTest {

  @Mock
  private AllowedNicknameRepository allowedNicknameRepository;

  @InjectMocks
  private AllowedNicknameWords allowedNicknameWords;

  @Test
  @DisplayName("returns normalized allowed words from repository")
  void returnsNormalizedAllowedWordsFromRepository() {
    // given
    given(allowedNicknameRepository.findAll())
        .willReturn(List.of(allowedNickname("Cl@ssic"), allowedNickname("")));

    // when
    val words = allowedNicknameWords.words();

    // then
    assertThat(words).containsOnly("classic");
  }

  @Test
  @DisplayName("returns immutable allowed words")
  void returnsImmutableAllowedWords() {
    // given
    given(allowedNicknameRepository.findAll()).willReturn(List.of(allowedNickname("classic")));

    // when
    val words = allowedNicknameWords.words();

    // then
    assertThatThrownBy(() -> words.add("other"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static AllowedNickname allowedNickname(final String value) {
    return AllowedNickname.builder().value(value).build();
  }
}
