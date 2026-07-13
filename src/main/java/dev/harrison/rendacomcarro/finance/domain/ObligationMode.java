package dev.harrison.rendacomcarro.finance.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum ObligationMode implements LabeledEnum {
    STRUCTURED("Parcelas programadas"),
    FLEXIBLE("Pagamentos flexíveis");

    private final String label;

    ObligationMode(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
