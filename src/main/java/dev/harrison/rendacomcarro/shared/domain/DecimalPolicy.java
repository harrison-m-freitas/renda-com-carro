package dev.harrison.rendacomcarro.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DecimalPolicy {
    public static final int MONEY_SCALE = 2;
    public static final int DISTANCE_SCALE = 1;
    public static final int VOLUME_SCALE = 3;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private DecimalPolicy() {
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING);
    }

    public static BigDecimal distance(BigDecimal value) {
        return value.setScale(DISTANCE_SCALE, ROUNDING);
    }

    public static BigDecimal volume(BigDecimal value) {
        return value.setScale(VOLUME_SCALE, ROUNDING);
    }
}
