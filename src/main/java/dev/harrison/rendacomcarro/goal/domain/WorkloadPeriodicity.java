package dev.harrison.rendacomcarro.goal.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum WorkloadPeriodicity implements LabeledEnum {
    DAILY("Por dia"),
    WEEKLY("Por semana"),
    MONTHLY("Por mês");

    private final String label;

    WorkloadPeriodicity(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
