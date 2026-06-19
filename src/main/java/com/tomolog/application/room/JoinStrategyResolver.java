package com.tomolog.application.room;

import com.tomolog.domain.room.JoinStrategyType;
import com.tomolog.domain.room.RoomJoinStrategy;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Holds every {@link RoomJoinStrategy} keyed by type and exposes the one selected via {@code
 * tomolog.join-strategy}. Keeping all three available also lets the acceptance test run the same
 * harness against each (SPEC §4).
 */
@Component
public class JoinStrategyResolver {

  private final Map<JoinStrategyType, RoomJoinStrategy> strategies;
  private final JoinStrategyType activeType;

  public JoinStrategyResolver(
      List<RoomJoinStrategy> strategyBeans,
      @Value("${tomolog.join-strategy}") JoinStrategyType activeType) {
    this.strategies =
        strategyBeans.stream()
            .collect(Collectors.toMap(RoomJoinStrategy::type, Function.identity()));
    this.activeType = activeType;
  }

  /** The strategy selected by configuration. */
  public RoomJoinStrategy active() {
    return strategyFor(activeType);
  }

  /** The strategy of the given type, or fails fast if it is not registered. */
  public RoomJoinStrategy strategyFor(JoinStrategyType type) {
    RoomJoinStrategy strategy = strategies.get(type);
    if (strategy == null) {
      throw new IllegalStateException("No join strategy registered for type " + type);
    }
    return strategy;
  }
}
