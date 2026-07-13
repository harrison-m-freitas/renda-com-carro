package dev.harrison.rendacomcarro.finance.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum InstallmentStatus implements LabeledEnum {
    PENDING("Pendente"),
    PARTIALLY_PAID("Parcialmente paga"),
    PAID("Paga"),
    OVERDUE("Em atraso"),
    CANCELLED("Cancelada");

    private final String label;

    InstallmentStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
