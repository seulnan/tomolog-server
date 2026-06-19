package com.tomolog.gamification.event;

/** Published when a room pet gains growth points; the realtime layer broadcasts PET_GREW. */
public record PetGrewEvent(Long roomId, int growthPoints, int level) {}
