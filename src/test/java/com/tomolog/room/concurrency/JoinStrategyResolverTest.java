package com.tomolog.room.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tomolog.application.room.JoinStrategyResolver;
import com.tomolog.domain.room.JoinStrategyType;
import com.tomolog.domain.room.RoomJoinStrategy;
import com.tomolog.domain.room.RoomMember;
import java.util.List;
import org.junit.jupiter.api.Test;

class JoinStrategyResolverTest {

  /** Minimal stand-in strategy so the resolver can be tested without the real infrastructure. */
  private record FakeStrategy(JoinStrategyType type) implements RoomJoinStrategy {
    @Override
    public RoomMember join(Long roomId, Long userId) {
      return null;
    }
  }

  private final RoomJoinStrategy pessimistic = new FakeStrategy(JoinStrategyType.PESSIMISTIC);
  private final RoomJoinStrategy optimistic = new FakeStrategy(JoinStrategyType.OPTIMISTIC);

  @Test
  void active_returnsStrategyMatchingConfiguredType() {
    JoinStrategyResolver resolver =
        new JoinStrategyResolver(List.of(pessimistic, optimistic), JoinStrategyType.OPTIMISTIC);

    assertThat(resolver.active().type()).isEqualTo(JoinStrategyType.OPTIMISTIC);
  }

  @Test
  void strategyFor_returnsRequestedType() {
    JoinStrategyResolver resolver =
        new JoinStrategyResolver(List.of(pessimistic, optimistic), JoinStrategyType.PESSIMISTIC);

    assertThat(resolver.strategyFor(JoinStrategyType.PESSIMISTIC)).isSameAs(pessimistic);
  }

  @Test
  void strategyFor_whenTypeNotRegistered_thenThrows() {
    JoinStrategyResolver resolver =
        new JoinStrategyResolver(List.of(pessimistic), JoinStrategyType.PESSIMISTIC);

    assertThatThrownBy(() -> resolver.strategyFor(JoinStrategyType.DISTRIBUTED))
        .isInstanceOf(IllegalStateException.class);
  }
}
