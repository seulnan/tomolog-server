package com.tomolog.application.gamification;

import java.util.List;

/** A user's study stats and badges (SPEC §5: GET /api/stats/me). */
public record StatsResponse(
    int currentStreak, int longestStreak, long totalStudyMinutes, List<BadgeResponse> badges) {}
