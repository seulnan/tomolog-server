package com.tomolog.domain.gamification;

import com.tomolog.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * The shared pet that grows as room members submit study snapshots. One pet per room; every
 * snapshot adds growth points and the pet levels up every {@code POINTS_PER_LEVEL} points (SPEC
 * §3).
 */
@Entity
@Table(name = "room_pets")
public class RoomPet extends BaseTimeEntity {

  public static final int POINTS_PER_LEVEL = 100;

  @Column(name = "room_id", nullable = false, unique = true)
  private Long roomId;

  @Column(name = "growth_points", nullable = false)
  private int growthPoints;

  @Column(nullable = false)
  private int level;

  protected RoomPet() {}

  public RoomPet(Long roomId) {
    this.roomId = roomId;
    this.growthPoints = 0;
    this.level = 1;
  }

  public void addGrowth(int points) {
    this.growthPoints += points;
    this.level = (growthPoints / POINTS_PER_LEVEL) + 1;
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
