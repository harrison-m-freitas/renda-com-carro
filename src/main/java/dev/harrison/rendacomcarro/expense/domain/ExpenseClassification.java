package dev.harrison.rendacomcarro.expense.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum ExpenseClassification implements LabeledEnum {
    PROFESSIONAL("Profissional"),
    PERSONAL("Pessoal"),
    MIXED("Misto");

    private final String label;

    ExpenseClassification(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
