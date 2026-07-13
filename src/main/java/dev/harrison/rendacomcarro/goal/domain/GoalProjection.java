package dev.harrison.rendacomcarro.goal.domain;

import java.math.BigDecimal;

public record GoalProjection(
    BigDecimal target,
    BigDecimal realized,
    BigDecimal remaining,
    int eligibleRemainingDays,
    BigDecimal requiredPerDay,
    BigDecimal progressPercentage,
    GoalStatus status
) {}
