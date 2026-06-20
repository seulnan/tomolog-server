package com.tomolog.domain.gamification;

/**
 * The shared pet that grows as room members submit snapshots — a pure domain model. One pet per
 * room; it levels up every {@code POINTS_PER_LEVEL} points. Concurrent feeds are counted via the
 * atomic update in the repository, not this method (SPEC §3).
 */
public class RoomPet {

  public static final int POINTS_PER_LEVEL = 100;

  private final Long id;
  private final Long roomId;
  private int growthPoints;
  private int level;

  /** Creates a new pet for a room (level 1, no growth yet). */
  public RoomPet(Long roomId) {
    this(null, roomId, 0, 1);
  }

  /** Full constructor used by the persistence mapper. */
  public RoomPet(Long id, Long roomId, int growthPoints, int level) {
    this.id = id;
    this.roomId = roomId;
    this.growthPoints = growthPoints;
    this.level = level;
  }

  public void addGrowth(int points) {
    this.growthPoints += points;
    this.level = (growthPoints / POINTS_PER_LEVEL) + 1;
  }

  public Long getId() {
    return id;
  }

  public Long getRoomId() {
    return roomId;
  }

  public int getGrowthPoints() {
    return growthPoints;
  }

  public int getLevel() {
    return level;
  }
}
