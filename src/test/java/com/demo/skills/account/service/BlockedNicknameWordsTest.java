package com.demo.skills.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.demo.skills.account.domain.BlockedNickname;
import com.demo.skills.account.domain.BlockedNicknameRepository;
import java.util.List;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockedNicknameWordsTest {

  @Mock
  private BlockedNicknameRepository blockedNicknameRepository;

  @InjectMocks
  private BlockedNicknameWords blockedNicknameWords;

  @Test
  @DisplayName("returns normalized blocked words from repository")
  void returnsNormalizedBlockedWordsFromRepository() {
    // given
    given(blockedNicknameRepository.findAll())
        .willReturn(List.of(blockedNickname("Bad!"), blockedNickname("")));

    // when
    val words = blockedNicknameWords.words();

    // then
    assertThat(words).containsOnly("badi");
  }

  @Test
  @DisplayName("returns immutable blocked words")
  void returnsImmutableBlockedWords() {
    // given
    given(blockedNicknameRepository.findAll()).willReturn(List.of(blockedNickname("blocked")));

    // when
    val words = blockedNicknameWords.words();

    // then
    assertThatThrownBy(() -> words.add("other"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static BlockedNickname blockedNickname(final String value) {
    return BlockedNickname.builder().value(value).build();
  }
}
