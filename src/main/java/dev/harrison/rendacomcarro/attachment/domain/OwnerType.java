package dev.harrison.rendacomcarro.attachment.domain;

import dev.harrison.rendacomcarro.shared.domain.LabeledEnum;

public enum OwnerType implements LabeledEnum {
    REVENUE("Receita"),
    EXPENSE("Gasto"),
    FUELING("Abastecimento"),
    OBLIGATION("Obrigação"),
    PAYMENT("Pagamento"),
    VEHICLE("Veículo");

    private final String label;

    OwnerType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
