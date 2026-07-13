package dev.harrison.rendacomcarro.goal.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum GoalStatus implements LabeledEnum {
    BELOW("Abaixo da meta"),
    ON_TRACK("Dentro da meta"),
    ABOVE("Acima da meta");

    private final String label;

    GoalStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
