package dev.harrison.rendacomcarro.vehicle.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum VehicleStatus implements LabeledEnum {
    ACTIVE("Ativo"),
    INACTIVE("Inativo"),
    ARCHIVED("Arquivado");

    private final String label;

    VehicleStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
