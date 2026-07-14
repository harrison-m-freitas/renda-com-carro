package dev.harrison.rendacomcarro.expense.web;

import dev.harrison.rendacomcarro.expense.application.ExpenseAllocationPreview;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record ExpenseAllocationPreviewResponse(
    String status,
    YearMonth referenceMonth,
    BigDecimal totalKilometers,
    BigDecimal professionalKilometers,
    BigDecimal professionalPercentage,
    boolean provisional,
    List<AlertResponse> alerts
) {
    public static ExpenseAllocationPreviewResponse from(ExpenseAllocationPreview preview) {
        return new ExpenseAllocationPreviewResponse(
            preview.status().name(),
            preview.referenceMonth(),
            preview.totalKilometers(),
            preview.professionalKilometers(),
            preview.professionalPercentage(),
            preview.provisional(),
            preview.alerts().stream()
                .map(alert -> new AlertResponse(alert.code(), alert.message(), alert.blocking()))
                .toList()
        );
    }

    public record AlertResponse(String code, String message, boolean blocking) {
    }
}
