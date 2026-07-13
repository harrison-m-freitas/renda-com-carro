package dev.harrison.rendacomcarro.vehicle.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum FuelType implements LabeledEnum {
    FLEX("Flex"),
    GASOLINE("Gasolina"),
    ETHANOL("Etanol"),
    DIESEL("Diesel"),
    ELECTRIC("Elétrico"),
    HYBRID("Híbrido");

    private final String label;

    FuelType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
