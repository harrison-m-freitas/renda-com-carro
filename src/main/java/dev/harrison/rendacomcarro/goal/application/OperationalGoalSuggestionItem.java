package dev.harrison.rendacomcarro.goal.application;

import dev.harrison.rendacomcarro.shared.domain.DecimalPolicy;
import java.math.BigDecimal;
import java.util.UUID;

public record OperationalGoalSuggestionItem(
    SuggestionItemType type,
    UUID sourceId,
    UUID vehicleId,
    String vehicleLabel,
    String label,
    BigDecimal amount,
    boolean overdue,
    String explanation
) {
    public OperationalGoalSuggestionItem {
        if (type == null || sourceId == null || label == null || label.isBlank()
            || amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Item da sugestão operacional inválido");
        }
        amount = DecimalPolicy.money(amount);
        vehicleLabel = vehicleLabel == null || vehicleLabel.isBlank()
            ? "Sem veículo específico"
            : vehicleLabel.trim();
        label = label.trim();
        explanation = explanation == null ? "" : explanation.trim();
    }
}
