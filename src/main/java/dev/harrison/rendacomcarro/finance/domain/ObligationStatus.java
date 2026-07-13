package dev.harrison.rendacomcarro.finance.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum ObligationStatus implements LabeledEnum {
    ACTIVE("Ativa"),
    PAID("Quitada"),
    CANCELLED("Cancelada");

    private final String label;

    ObligationStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
