package dev.harrison.rendacomcarro.expense.application;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public record ExpenseAllocationPreview(
    Status status,
    UUID vehicleId,
    YearMonth referenceMonth,
    BigDecimal totalKilometers,
    BigDecimal professionalKilometers,
    BigDecimal professionalPercentage,
    boolean provisional,
    List<MileageAlert> alerts
) {
    public ExpenseAllocationPreview {
        alerts = List.copyOf(alerts);
    }

    public enum Status {
        CONFIRMED,
        ESTIMATED,
        INSUFFICIENT_DATA
    }
}
