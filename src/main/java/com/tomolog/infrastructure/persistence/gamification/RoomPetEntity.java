package com.tomolog.infrastructure.persistence.gamification;

import com.tomolog.infrastructure.persistence.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** JPA persistence model for a room pet (maps to {@code room_pets}, one per room). */
@Entity
@Table(
    name = "room_pets",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_room_pets_room_id",
            columnNames = {"room_id"}))
public class RoomPetEntity extends BaseTimeEntity {

  @Column(name = "room_id", nullable = false, unique = true)
  private Long roomId;

  @Column(name = "growth_points", nullable = false)
  private int growthPoints;

  @Column(nullable = false)
  private int level;

  protected RoomPetEntity() {}

  public RoomPetEntity(Long roomId, int growthPoints, int level) {
    this.roomId = roomId;
    this.growthPoints = growthPoints;
    this.level = level;
  }

  /** Applies a domain pet's mutable fields onto this managed entity (for updates). */
  public void apply(int growthPoints, int level) {
    this.growthPoints = growthPoints;
    this.level = level;
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
