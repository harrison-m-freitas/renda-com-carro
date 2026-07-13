package dev.harrison.rendacomcarro.finance.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum ObligationType implements LabeledEnum {
    FAMILY_LOAN("Empréstimo familiar"),
    BANK_FINANCING("Financiamento bancário"),
    OTHER_ACQUISITION("Outro custo de aquisição");

    private final String label;

    ObligationType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
