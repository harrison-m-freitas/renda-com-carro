package dev.harrison.rendacomcarro.finance.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum InterestRatePeriod implements LabeledEnum {
    MONTHLY("% ao mês"),
    ANNUAL("% ao ano");

    private final String label;

    InterestRatePeriod(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
