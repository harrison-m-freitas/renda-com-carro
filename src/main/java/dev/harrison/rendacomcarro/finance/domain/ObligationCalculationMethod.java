package dev.harrison.rendacomcarro.finance.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum ObligationCalculationMethod implements LabeledEnum {
    INSTALLMENT_KNOWN("Conheço o valor da parcela"),
    RATE_KNOWN("Conheço a taxa de juros"),
    INTEREST_FREE("Não há juros"),
    TOTAL_KNOWN("Conheço o valor total"),
    RATE_UNKNOWN("A taxa ainda não foi informada");

    private final String label;

    ObligationCalculationMethod(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
