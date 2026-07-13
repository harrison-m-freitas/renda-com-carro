package dev.harrison.rendacomcarro.operation.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum RevenueType implements LabeledEnum {
    TRIP("Corrida"),
    BONUS("Bônus"),
    PROMOTION("Promoção"),
    TIP("Gorjeta"),
    ADJUSTMENT("Ajuste"),
    CONSOLIDATED("Consolidado");

    private final String label;

    RevenueType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
