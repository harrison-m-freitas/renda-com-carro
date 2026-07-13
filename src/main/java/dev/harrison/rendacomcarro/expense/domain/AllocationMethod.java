package dev.harrison.rendacomcarro.expense.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum AllocationMethod implements LabeledEnum {
    MILEAGE_RATIO("Proporcional à quilometragem"),
    MANUAL_PERCENTAGE("Percentual informado manualmente"),
    FIXED_AMOUNT("Valor profissional fixo");

    private final String label;

    AllocationMethod(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
