package dev.harrison.rendacomcarro.operation.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum ShiftStatus implements LabeledEnum {
    OPEN("Aberto"),
    CLOSED("Fechado"),
    CANCELLED("Cancelado");

    private final String label;

    ShiftStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
