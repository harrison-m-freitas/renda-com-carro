package dev.harrison.rendacomcarro.goal.application;

import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public record OperationalGoalSuggestion(
    YearMonth month,
    String monthLabel,
    UUID vehicleId,
    BigDecimal currentExpenses,
    BigDecimal overdueProfessionalExpenses,
    BigDecimal currentVehicleObligations,
    BigDecimal overdueVehicleObligations,
    BigDecimal professionalCostsTotal,
    List<OperationalGoalSuggestionItem> items,
    List<OperationalGoalSuggestionItem> ignoredItems,
    List<String> warnings
) {
    public OperationalGoalSuggestion {
        if (month == null || monthLabel == null || vehicleId == null) {
            throw new IllegalArgumentException("Sugestão operacional inválida");
        }
        currentExpenses = DecimalPolicy.money(currentExpenses);
        overdueProfessionalExpenses = DecimalPolicy.money(overdueProfessionalExpenses);
        currentVehicleObligations = DecimalPolicy.money(currentVehicleObligations);
        overdueVehicleObligations = DecimalPolicy.money(overdueVehicleObligations);
        professionalCostsTotal = DecimalPolicy.money(professionalCostsTotal);
        items = List.copyOf(items);
        ignoredItems = List.copyOf(ignoredItems);
        warnings = List.copyOf(warnings);
    }
}
