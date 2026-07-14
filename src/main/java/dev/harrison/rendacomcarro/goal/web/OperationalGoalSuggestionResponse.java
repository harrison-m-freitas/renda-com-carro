package dev.harrison.rendacomcarro.goal.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.harrison.rendacomcarro.goal.application.OperationalGoalSuggestion;
import dev.harrison.rendacomcarro.goal.application.OperationalGoalSuggestionItem;
import dev.harrison.rendacomcarro.goal.application.SuggestionItemType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record OperationalGoalSuggestionResponse(
    String month,
    String monthLabel,
    Set<UUID> vehicleIds,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal currentExpenses,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal overdueProfessionalExpenses,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal currentVehicleObligations,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal overdueVehicleObligations,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal professionalCostsTotal,
    List<Item> items,
    List<Item> ignoredItems,
    List<String> warnings
) {
    public record Item(
        SuggestionItemType type,
        UUID sourceId,
        UUID vehicleId,
        String vehicleLabel,
        String label,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        boolean overdue,
        String explanation
    ) {
        static Item from(OperationalGoalSuggestionItem item) {
            return new Item(
                item.type(),
                item.sourceId(),
                item.vehicleId(),
                item.vehicleLabel(),
                item.label(),
                item.amount(),
                item.overdue(),
                item.explanation()
            );
        }
    }

    public static OperationalGoalSuggestionResponse from(OperationalGoalSuggestion suggestion) {
        return new OperationalGoalSuggestionResponse(
            suggestion.month().toString(),
            suggestion.monthLabel(),
            suggestion.vehicleIds(),
            suggestion.currentExpenses(),
            suggestion.overdueProfessionalExpenses(),
            suggestion.currentVehicleObligations(),
            suggestion.overdueVehicleObligations(),
            suggestion.professionalCostsTotal(),
            suggestion.items().stream().map(Item::from).toList(),
            suggestion.ignoredItems().stream().map(Item::from).toList(),
            suggestion.warnings()
        );
    }
}
