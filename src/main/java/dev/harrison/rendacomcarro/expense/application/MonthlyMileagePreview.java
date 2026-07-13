package dev.harrison.rendacomcarro.expense.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public record MonthlyMileagePreview(
    UUID vehicleId,
    YearMonth month,
    BigDecimal inferredInitialOdometer,
    BigDecimal inferredFinalOdometer,
    BigDecimal totalKilometers,
    BigDecimal professionalKilometers,
    BigDecimal personalKilometers,
    BigDecimal professionalPercentage,
    OdometerOrigin initialOrigin,
    OdometerOrigin finalOrigin,
    int closedDays,
    int closedShifts,
    int fuelings,
    List<MileageAlert> alerts,
    LocalDateTime calculatedAt
) {
    public MonthlyMileagePreview {
        alerts = List.copyOf(alerts);
    }

    public boolean hasBlockingAlerts() {
        return alerts.stream().anyMatch(MileageAlert::blocking);
    }

    public List<MileageAlert> blockingAlerts() {
        return alerts.stream().filter(MileageAlert::blocking).toList();
    }

    public List<MileageAlert> warnings() {
        return alerts.stream().filter(alert -> !alert.blocking()).toList();
    }
}
