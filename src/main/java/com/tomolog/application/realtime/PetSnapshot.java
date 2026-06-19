package com.tomolog.application.realtime;

/** Room pet state, used as the PET_GREW event payload and in responses. */
public record PetSnapshot(Long roomId, int growthPoints, int level) {}
