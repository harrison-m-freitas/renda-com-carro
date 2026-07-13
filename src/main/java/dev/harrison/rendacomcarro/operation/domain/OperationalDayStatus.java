package dev.harrison.rendacomcarro.operation.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum OperationalDayStatus implements LabeledEnum {
    IN_PROGRESS("Em andamento"),
    CLOSED("Fechado"),
    CANCELLED("Cancelado");

    private final String label;

    OperationalDayStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
