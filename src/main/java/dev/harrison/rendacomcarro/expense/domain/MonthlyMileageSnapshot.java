package dev.harrison.rendacomcarro.expense.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record MonthlyMileageSnapshot(BigDecimal totalKilometers, BigDecimal professionalKilometers) {
    public MonthlyMileageSnapshot {
        if (totalKilometers == null || professionalKilometers == null || totalKilometers.signum() < 0
            || professionalKilometers.signum() < 0 || professionalKilometers.compareTo(totalKilometers) > 0) {
            throw new IllegalArgumentException("Quilometragem mensal inválida");
        }
    }

    public BigDecimal professionalRatio() {
        if (totalKilometers.signum() == 0) return BigDecimal.ZERO.setScale(4);
        return professionalKilometers.divide(totalKilometers, 4, RoundingMode.HALF_UP);
    }
}
