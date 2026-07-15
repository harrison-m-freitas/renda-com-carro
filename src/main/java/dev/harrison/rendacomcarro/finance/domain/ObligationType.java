package dev.harrison.rendacomcarro.finance.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum ObligationType implements LabeledEnum {
    BANK_FINANCING("Financiamento bancário"),
    FAMILY_LOAN("Empréstimo de familiar ou conhecido"),
    OTHER_ACQUISITION("Dívida ou custo de aquisição"),
    OTHER_DEBT("Outro compromisso financeiro");

    private final String label;

    ObligationType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
