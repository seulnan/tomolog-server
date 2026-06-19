package com.tomolog.room.concurrency;

/**
 * Selectable join-room concurrency strategies (SPEC §4), chosen via {@code tomolog.join-strategy}.
 */
public enum JoinStrategyType {
  PESSIMISTIC,
  OPTIMISTIC,
  DISTRIBUTED,
  // High-throughput strategy used for large THEMED rooms (atomic conditional counter update).
  ATOMIC
}
